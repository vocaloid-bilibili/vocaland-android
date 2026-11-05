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
import xyz.lisfox.vocaland.data.model.TrendingCampaignDto
import xyz.lisfox.vocaland.data.model.TrendingCandidateDto
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
) {
    val votingOpen: Boolean
        get() {
            val current = campaign ?: return false
            if (current.status != "voting") return false
            val end = parseIsoMillis(current.voteEnd) ?: return true
            return end > System.currentTimeMillis()
        }

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

    private fun load() {
        loadJob?.cancel()
        tallyJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    candidateError = null,
                    submitError = null,
                    notice = null,
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
                    loadVotingData(campaign, loggedIn)
                    if (campaign.status == "voting") startTallyPolling()
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

    private suspend fun loadVotingData(campaign: TrendingCampaignDto, loggedIn: Boolean) {
        _state.update {
            it.copy(
                candidateLoading = true,
                candidateError = null,
                candidates = emptyList(),
                selected = if (loggedIn) it.selected else emptySet(),
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
