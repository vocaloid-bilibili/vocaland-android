package xyz.lisfox.vocaland.ui.voting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.model.SearchEntityDto
import xyz.lisfox.vocaland.data.model.TRENDING_NOMINATION_LIMIT
import xyz.lisfox.vocaland.data.model.TrendingCampaignDto
import xyz.lisfox.vocaland.data.model.TrendingCandidateDto
import xyz.lisfox.vocaland.data.model.TrendingResultRowDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.VotingRepository
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.util.parseIsoMillis

data class TrendingVotingUiState(
    val campaign: TrendingCampaignDto? = null,
    val candidates: List<TrendingCandidateDto> = emptyList(),
    val tally: Map<Int, Int> = emptyMap(),
    val totalVotes: Int = 0,
    val voterCount: Int = 0,
    val maxVotes: Int = 0,
    val selected: Set<Int> = emptySet(),
    val serverSelected: Set<Int> = emptySet(),
    val loading: Boolean = true,
    val candidateLoading: Boolean = false,
    val error: String? = null,
    val candidateError: String? = null,
    val submitting: Boolean = false,
    val submitError: String? = null,
    val notice: String? = null,
    val isLoggedIn: Boolean = false,
    val myNominations: List<TrendingCandidateDto> = emptyList(),
    val nominationsLoading: Boolean = false,
    val nominationsError: String? = null,
    val entityQuery: String = "",
    val entityResults: List<SearchEntityDto> = emptyList(),
    val entitySearchLoading: Boolean = false,
    val entitySearchError: String? = null,
    val nominating: Boolean = false,
    val nominatingId: Int? = null,
    val nominationError: String? = null,
    val nominationNotice: String? = null,
    val withdrawingId: Int? = null,
    val results: List<TrendingResultRowDto> = emptyList(),
    val resultsLoading: Boolean = false,
    val resultsError: String? = null,
) {
    val votingOpen: Boolean
        get() {
            val current = campaign ?: return false
            if (current.status != "voting") return false
            val end = parseIsoMillis(current.voteEnd) ?: return true
            return end > System.currentTimeMillis()
        }

    val nominationOpen: Boolean
        get() {
            val current = campaign ?: return false
            if (current.status != "nomination") return false
            val end = parseIsoMillis(current.submitEnd) ?: return true
            return end > System.currentTimeMillis()
        }

    val nominationAtLimit: Boolean get() = myNominations.size >= TRENDING_NOMINATION_LIMIT

    val dirty: Boolean get() = selected != serverSelected
}

class TrendingVotingViewModel(
    private val repository: VotingRepository,
    private val session: AuthSession,
    private val campaignId: Int,
) : ViewModel() {
    private val _state = MutableStateFlow(TrendingVotingUiState())
    val state: StateFlow<TrendingVotingUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var tallyJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            session.state
                .map { it.isLoggedIn }
                .distinctUntilChanged()
                .collectLatest { load() }
        }
    }

    fun refresh() {
        load()
    }

    fun toggle(candidateId: Int) {
        val current = _state.value
        if (!current.votingOpen || !current.isLoggedIn || candidateId <= 0) return

        if (candidateId in current.selected) {
            _state.update {
                it.copy(
                    selected = it.selected - candidateId,
                    submitError = null,
                    notice = null,
                )
            }
            return
        }

        if (current.maxVotes > 0 && current.selected.size >= current.maxVotes) {
            _state.update { it.copy(notice = "最多选择 ${current.maxVotes} 项") }
            return
        }

        _state.update {
            it.copy(
                selected = it.selected + candidateId,
                submitError = null,
                notice = null,
            )
        }
    }

    fun submit() {
        val current = _state.value
        if (!current.isLoggedIn) {
            _state.update { it.copy(submitError = "请先登录后再投票") }
            return
        }
        if (!current.votingOpen) {
            _state.update { it.copy(submitError = "当前投票已结束") }
            return
        }
        if (!current.dirty || current.submitting) return
        if (current.maxVotes > 0 && current.selected.size > current.maxVotes) {
            _state.update {
                it.copy(submitError = "最多选择 ${current.maxVotes} 项")
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(submitting = true, submitError = null, notice = null)
            }
            repository.castTrendingVotes(
                campaignId = campaignId,
                candidateIds = current.selected,
            ).onSuccess { response ->
                val selected = response.data.toSet()
                _state.update {
                    it.copy(
                        selected = selected,
                        serverSelected = selected,
                        submitting = false,
                        notice = "投票已保存",
                    )
                }
                refreshTally()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        submitting = false,
                        submitError = error.toUserMessage(),
                    )
                }
            }
        }
    }

    fun updateEntityQuery(value: String) {
        _state.update {
            it.copy(
                entityQuery = value.take(100),
                entityResults = emptyList(),
                entitySearchLoading = false,
                entitySearchError = null,
                nominationError = null,
                nominationNotice = null,
            )
        }
        scheduleEntitySearch()
    }

    fun submitNomination(entity: SearchEntityDto) {
        val current = _state.value
        val campaign = current.campaign ?: return
        if (!current.isLoggedIn || !current.nominationOpen || current.nominating) return
        if (current.nominationAtLimit) {
            _state.update { it.copy(nominationError = "最多提名 $TRENDING_NOMINATION_LIMIT 个候选") }
            return
        }
        if (entity.id <= 0) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    nominating = true,
                    nominatingId = entity.id,
                    nominationError = null,
                    nominationNotice = null,
                )
            }
            repository.submitTrendingNomination(
                campaignId = campaignId,
                entityKind = campaign.entityKind,
                entityId = entity.id,
            ).onSuccess {
                _state.update {
                    it.copy(
                        nominating = false,
                        nominatingId = null,
                        entityQuery = "",
                        entityResults = emptyList(),
                        nominationNotice = "提名成功",
                    )
                }
                loadNominations(loggedIn = true)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        nominating = false,
                        nominatingId = null,
                        nominationError = error.toUserMessage(),
                    )
                }
            }
        }
    }

    fun withdrawNomination(candidateId: Int) {
        if (candidateId <= 0 || _state.value.withdrawingId != null) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    withdrawingId = candidateId,
                    nominationError = null,
                    nominationNotice = null,
                )
            }
            repository.withdrawTrendingNomination(candidateId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            withdrawingId = null,
                            nominationNotice = "提名已撤回",
                        )
                    }
                    loadNominations(loggedIn = true)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            withdrawingId = null,
                            nominationError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun load() {
        loadJob?.cancel()
        tallyJob?.cancel()
        searchJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    candidateError = null,
                    submitError = null,
                    notice = null,
                    nominationError = null,
                    nominationNotice = null,
                    myNominations = emptyList(),
                    entityQuery = "",
                    entityResults = emptyList(),
                    entitySearchLoading = false,
                    entitySearchError = null,
                    nominating = false,
                    nominatingId = null,
                    withdrawingId = null,
                    results = emptyList(),
                    resultsLoading = false,
                    resultsError = null,
                )
            }
            val loggedIn = session.state.value.isLoggedIn
            repository.loadTrendingCampaign(campaignId)
                .onSuccess { campaign ->
                    _state.update {
                        it.copy(
                            campaign = campaign,
                            loading = false,
                            error = null,
                            isLoggedIn = loggedIn,
                            maxVotes = campaign.maxVotes ?: it.maxVotes,
                        )
                    }
                    when (campaign.status) {
                        "nomination" -> loadNominations(loggedIn)
                        "voting" -> {
                            loadVotingData(campaign, loggedIn)
                            startTallyPolling()
                        }
                        "finalized" -> loadResults()
                        else -> Unit
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = error.toUserMessage(),
                            isLoggedIn = loggedIn,
                        )
                    }
                }
        }
    }

    private suspend fun loadNominations(loggedIn: Boolean) {
        _state.update {
            it.copy(
                nominationsLoading = true,
                nominationsError = null,
                myNominations = emptyList(),
            )
        }
        repository.loadTrendingNominations(campaignId, includeMine = loggedIn)
            .onSuccess { data ->
                _state.update {
                    it.copy(
                        candidates = data.candidates.data,
                        myNominations = data.myNominations,
                        nominationsLoading = false,
                        candidateLoading = false,
                    )
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        nominationsLoading = false,
                        nominationsError = error.toUserMessage(),
                    )
                }
            }
    }

    private suspend fun loadVotingData(campaign: TrendingCampaignDto, loggedIn: Boolean) {
        _state.update {
            it.copy(
                candidateLoading = true,
                candidateError = null,
                candidates = emptyList(),
                selected = emptySet(),
                serverSelected = emptySet(),
            )
        }
        repository.loadTrendingVoting(
            campaignId = campaignId,
            includeMyVotes = loggedIn && campaign.status == "voting",
        ).onSuccess { data ->
            val selected = data.myVotes.toSet()
            _state.update {
                it.copy(
                    candidates = data.candidates.data,
                    tally = data.tally.data.associate { entry ->
                        entry.candidateId to entry.voteCount
                    },
                    totalVotes = data.tally.totalVotes,
                    voterCount = data.tally.voterCount,
                    maxVotes = data.tally.maxVotes ?: campaign.maxVotes ?: it.maxVotes,
                    selected = selected,
                    serverSelected = selected,
                    candidateLoading = false,
                    candidateError = null,
                )
            }
        }.onFailure { error ->
            _state.update {
                it.copy(
                    candidateLoading = false,
                    candidateError = error.toUserMessage(),
                )
            }
        }
    }

    private suspend fun loadResults() {
        _state.update {
            it.copy(
                resultsLoading = true,
                resultsError = null,
                results = emptyList(),
            )
        }
        repository.loadTrendingResults(campaignId)
            .onSuccess { results ->
                _state.update {
                    it.copy(results = results, resultsLoading = false)
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        resultsLoading = false,
                        resultsError = error.toUserMessage(),
                    )
                }
            }
    }

    private fun scheduleEntitySearch() {
        searchJob?.cancel()
        val current = _state.value
        val campaign = current.campaign ?: return
        val query = current.entityQuery.trim()
        if (!current.isLoggedIn || !current.nominationOpen || query.isEmpty()) {
            _state.update {
                it.copy(
                    entityResults = emptyList(),
                    entitySearchLoading = false,
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(400)
            _state.update {
                it.copy(entitySearchLoading = true, entitySearchError = null)
            }
            repository.searchNominationEntities(campaign.entityKind, query)
                .onSuccess { results ->
                    if (_state.value.entityQuery.trim() == query) {
                        _state.update {
                            it.copy(
                                entityResults = results,
                                entitySearchLoading = false,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    if (_state.value.entityQuery.trim() == query) {
                        _state.update {
                            it.copy(
                                entitySearchLoading = false,
                                entitySearchError = error.toUserMessage(),
                            )
                        }
                    }
                }
        }
    }

    private fun startTallyPolling() {
        tallyJob?.cancel()
        tallyJob = viewModelScope.launch {
            while (isActive) {
                delay(TALLY_INTERVAL_MS)
                if (!_state.value.votingOpen) break
                refreshTally()
            }
        }
    }

    private fun refreshTally() {
        viewModelScope.launch {
            repository.loadTrendingTally(campaignId).onSuccess { tally ->
                _state.update {
                    it.copy(
                        tally = tally.data.associate { entry ->
                            entry.candidateId to entry.voteCount
                        },
                        totalVotes = tally.totalVotes,
                        voterCount = tally.voterCount,
                        maxVotes = tally.maxVotes ?: it.maxVotes,
                    )
                }
            }
        }
    }

    private companion object {
        const val TALLY_INTERVAL_MS = 30_000L
    }
}
