package xyz.lisfox.vocaland.ui.voting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.model.EdBonusVoteDto
import xyz.lisfox.vocaland.data.model.EdCampaignDto
import xyz.lisfox.vocaland.data.model.EdVotingCandidateDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.VotingRepository
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.util.parseIsoMillis

data class EdVotingUiState(
    val campaign: EdCampaignDto? = null,
    val candidates: List<EdVotingCandidateDto> = emptyList(),
    val maxVotes: Int = 0,
    val voterCount: Int = 0,
    val positive: Set<Int> = emptySet(),
    val negative: Set<Int> = emptySet(),
    val serverPositive: Set<Int> = emptySet(),
    val serverNegative: Set<Int> = emptySet(),
    val bonusVotes: List<EdBonusVoteDto> = emptyList(),
    val loading: Boolean = true,
    val candidateLoading: Boolean = false,
    val error: String? = null,
    val candidateError: String? = null,
    val submitting: Boolean = false,
    val submitError: String? = null,
    val notice: String? = null,
    val isLoggedIn: Boolean = false,
) {
    val selectedCount: Int get() = positive.size + negative.size

    val dirty: Boolean
        get() = positive != serverPositive || negative != serverNegative

    val votingOpen: Boolean
        get() {
            val current = campaign ?: return false
            if (current.status != "voting") return false
            val end = parseIsoMillis(current.voteEnd) ?: return true
            return end > System.currentTimeMillis()
        }
}

class EdVotingViewModel(
    private val repository: VotingRepository,
    private val session: AuthSession,
    private val campaignId: Int,
) : ViewModel() {
    private val _state = MutableStateFlow(EdVotingUiState())
    val state: StateFlow<EdVotingUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

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

    fun togglePositive(candidateId: Int) {
        val current = _state.value
        if (!current.votingOpen || candidateId <= 0) return

        val positive = if (candidateId in current.positive) {
            current.positive - candidateId
        } else {
            current.positive + candidateId
        }
        val negative = current.negative - candidateId
        _state.update {
            it.copy(
                positive = positive,
                negative = negative,
                submitError = null,
                notice = null,
            )
        }
    }

    fun toggleNegative(candidateId: Int) {
        val current = _state.value
        if (!current.votingOpen || candidateId <= 0) return

        val negative = if (candidateId in current.negative) {
            current.negative - candidateId
        } else {
            current.negative + candidateId
        }
        val positive = current.positive - candidateId
        _state.update {
            it.copy(
                positive = positive,
                negative = negative,
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
        if (current.maxVotes > 0 && current.selectedCount > current.maxVotes) {
            _state.update {
                it.copy(submitError = "最多选择 ${current.maxVotes} 项")
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(submitting = true, submitError = null, notice = null)
            }
            repository.castEdVotes(
                campaignId = campaignId,
                positiveCandidateIds = current.positive,
                negativeCandidateIds = current.negative,
                bonusVotes = current.bonusVotes,
            ).onSuccess { votes ->
                val positive = votes.filter { !it.isBonus && !it.isNegative }
                    .map { it.candidateId }
                    .toSet()
                val negative = votes.filter { !it.isBonus && it.isNegative }
                    .map { it.candidateId }
                    .toSet()
                val bonus = votes.filter { it.isBonus && it.bonusCount > 0 }
                    .map {
                        EdBonusVoteDto(
                            candidateId = it.candidateId,
                            count = it.bonusCount,
                            isNegative = it.isNegative,
                        )
                    }
                _state.update {
                    it.copy(
                        positive = positive,
                        negative = negative,
                        serverPositive = positive,
                        serverNegative = negative,
                        bonusVotes = bonus,
                        submitting = false,
                        notice = "投票已保存",
                    )
                }
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
            repository.loadEdCampaign(campaignId)
                .onSuccess { campaign ->
                    _state.update {
                        it.copy(
                            campaign = campaign,
                            loading = false,
                            error = null,
                            isLoggedIn = loggedIn,
                            voterCount = campaign.voterCount ?: it.voterCount,
                        )
                    }
                    loadVotingData(campaign, loggedIn)
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

    private suspend fun loadVotingData(campaign: EdCampaignDto, loggedIn: Boolean) {
        if (campaign.status != "voting" || !loggedIn) {
            _state.update {
                it.copy(
                    candidates = emptyList(),
                    candidateLoading = false,
                    candidateError = null,
                    positive = emptySet(),
                    negative = emptySet(),
                    serverPositive = emptySet(),
                    serverNegative = emptySet(),
                    bonusVotes = emptyList(),
                    maxVotes = campaign.maxVotes ?: 0,
                )
            }
            return
        }

        _state.update {
            it.copy(
                candidateLoading = true,
                candidateError = null,
                candidates = emptyList(),
            )
        }
        repository.loadEdVoting(campaignId)
            .onSuccess { data ->
                val positive = data.myVotes
                    .filter { !it.isBonus && !it.isNegative }
                    .map { it.candidateId }
                    .toSet()
                val negative = data.myVotes
                    .filter { !it.isBonus && it.isNegative }
                    .map { it.candidateId }
                    .toSet()
                val bonus = data.myVotes
                    .filter { it.isBonus && it.bonusCount > 0 }
                    .map {
                        EdBonusVoteDto(
                            candidateId = it.candidateId,
                            count = it.bonusCount,
                            isNegative = it.isNegative,
                        )
                    }
                _state.update {
                    it.copy(
                        candidates = data.candidates.data,
                        maxVotes = data.candidates.maxVotes,
                        voterCount = data.candidates.voterCount,
                        positive = positive,
                        negative = negative,
                        serverPositive = positive,
                        serverNegative = negative,
                        bonusVotes = bonus,
                        candidateLoading = false,
                        candidateError = null,
                    )
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        candidateLoading = false,
                        candidateError = error.toUserMessage(),
                    )
                }
            }
    }
}
