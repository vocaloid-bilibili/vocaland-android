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
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.model.EdBonusVoteDto
import xyz.lisfox.vocaland.data.model.EdCampaignDto
import xyz.lisfox.vocaland.data.model.EdCandidateDto
import xyz.lisfox.vocaland.data.model.EdEligibilityDto
import xyz.lisfox.vocaland.data.model.EdMechanismConfigDto
import xyz.lisfox.vocaland.data.model.EdResultDto
import xyz.lisfox.vocaland.data.model.EdVotingCandidateDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.VotingRepository
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.util.parseIsoMillis

data class EdVotingUiState(
    val campaign: EdCampaignDto? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val myNomination: EdCandidateDto? = null,
    val nominationLoading: Boolean = false,
    val nominationCandidates: List<EdCandidateDto> = emptyList(),
    val nominationCandidatesLoading: Boolean = false,
    val nominationCandidatesError: String? = null,
    val bvid: String = "",
    val name: String = "",
    val vocalist: String = "",
    val producer: String = "",
    val eligibility: EdEligibilityDto? = null,
    val eligibilityLoading: Boolean = false,
    val eligibilityError: String? = null,
    val nominationSubmitting: Boolean = false,
    val nominationError: String? = null,
    val nominationNotice: String? = null,
    val withdrawing: Boolean = false,
    val candidates: List<EdVotingCandidateDto> = emptyList(),
    val maxVotes: Int = 0,
    val voterCount: Int = 0,
    val positive: Set<Int> = emptySet(),
    val negative: Set<Int> = emptySet(),
    val serverPositive: Set<Int> = emptySet(),
    val serverNegative: Set<Int> = emptySet(),
    val bonusVotes: Map<Int, EdBonusVoteDto> = emptyMap(),
    val serverBonusVotes: Map<Int, EdBonusVoteDto> = emptyMap(),
    val allowBonusVote: Boolean = true,
    val allowNegativeVote: Boolean = true,
    val mechanismConfig: EdMechanismConfigDto = EdMechanismConfigDto(),
    val voteExpired: Boolean = false,
    val walletTickets: Int = 0,
    val walletLoading: Boolean = false,
    val candidateLoading: Boolean = false,
    val candidateError: String? = null,
    val submitting: Boolean = false,
    val submitError: String? = null,
    val notice: String? = null,
    val results: List<EdResultDto> = emptyList(),
    val ranking: List<EdResultDto> = emptyList(),
    val resultsLoading: Boolean = false,
    val resultsError: String? = null,
) {
    val selectedCount: Int get() = positive.size + negative.size

    val bonusTotal: Int get() = bonusVotes.values.sumOf { it.count }

    val bonusBudget: Int get() = walletTickets + serverBonusVotes.values.sumOf { it.count }

    val bonusRemaining: Int get() = (bonusBudget - bonusTotal).coerceAtLeast(0)

    val dirty: Boolean
        get() = positive != serverPositive ||
            negative != serverNegative ||
            bonusVotes != serverBonusVotes

    val votingOpen: Boolean
        get() {
            val current = campaign ?: return false
            if (current.status != "voting" || voteExpired) return false
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

    val canSubmitNomination: Boolean
        get() {
            val eligible = eligibility ?: return false
            if (!eligible.canSubmit) return false
            if (eligible.songId == null && name.isBlank()) return false
            return bvid.trim().length >= 10
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
    private var eligibilityJob: Job? = null
    private var submitJob: Job? = null

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

    fun markVoteExpired() {
        _state.update { it.copy(voteExpired = true) }
    }

    fun updateBvid(value: String) {
        eligibilityJob?.cancel()
        _state.update {
            it.copy(
                bvid = value.take(12),
                eligibility = null,
                eligibilityLoading = false,
                eligibilityError = null,
                nominationError = null,
                nominationNotice = null,
            )
        }
        scheduleEligibilityCheck()
    }

    fun updateName(value: String) {
        _state.update { it.copy(name = value.take(200), nominationError = null) }
    }

    fun updateVocalist(value: String) {
        _state.update { it.copy(vocalist = value.take(200), nominationError = null) }
    }

    fun updateProducer(value: String) {
        _state.update { it.copy(producer = value.take(200), nominationError = null) }
    }

    fun submitNomination() {
        val current = _state.value
        if (!current.isLoggedIn) {
            _state.update { it.copy(nominationError = "请先登录后再提名") }
            return
        }
        if (!current.nominationOpen) {
            _state.update { it.copy(nominationError = "当前提名已结束") }
            return
        }
        if (!current.canSubmitNomination || current.nominationSubmitting) return
        val eligibility = current.eligibility ?: return

        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    nominationSubmitting = true,
                    nominationError = null,
                    nominationNotice = null,
                )
            }
            repository.submitEdNomination(
                campaignId = campaignId,
                bvid = current.bvid.trim(),
                eligibility = eligibility,
                name = current.name.trim(),
                vocalist = current.vocalist.trim(),
                producer = current.producer.trim(),
            ).onSuccess {
                _state.update {
                    it.copy(
                        bvid = "",
                        name = "",
                        vocalist = "",
                        producer = "",
                        eligibility = null,
                        nominationSubmitting = false,
                        nominationNotice = "提名已提交，等待审核",
                    )
                }
                loadNominationData(loggedIn = true)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        nominationSubmitting = false,
                        nominationError = error.toUserMessage(),
                    )
                }
            }
        }
    }

    fun withdrawNomination() {
        val nomination = _state.value.myNomination ?: return
        if (_state.value.withdrawing) return

        viewModelScope.launch {
            _state.update { it.copy(withdrawing = true, nominationError = null) }
            repository.withdrawEdNomination(nomination.id)
                .onSuccess {
                    _state.update {
                        it.copy(
                            withdrawing = false,
                            nominationNotice = "提名已撤回",
                        )
                    }
                    loadNominationData(loggedIn = true)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            withdrawing = false,
                            nominationError = error.toUserMessage(),
                        )
                    }
                }
        }
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
        if (!current.votingOpen || !current.allowNegativeVote || candidateId <= 0) return

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

    fun addBonusVote(candidateId: Int, amount: Int = 1) {
        val current = _state.value
        if (!current.votingOpen || !current.isLoggedIn || !current.allowBonusVote || candidateId <= 0) return
        if (current.bonusTotal >= current.bonusBudget) {
            _state.update { it.copy(notice = "附加票已用完") }
            return
        }

        val existingCount = current.bonusVotes[candidateId]?.count ?: 0
        val nextCount = (existingCount + amount)
            .coerceAtMost(existingCount + current.bonusRemaining)
            .coerceAtLeast(0)
        if (nextCount <= existingCount) return

        val next = EdBonusVoteDto(
            candidateId = candidateId,
            count = nextCount,
            isNegative = current.bonusVotes[candidateId]?.isNegative ?: false,
        )
        _state.update {
            it.copy(
                bonusVotes = it.bonusVotes + (candidateId to next),
                notice = null,
                submitError = null,
            )
        }
    }

    fun removeBonusVote(candidateId: Int) {
        val existing = _state.value.bonusVotes[candidateId] ?: return
        if (existing.count <= 1) {
            clearBonusVote(candidateId)
            return
        }

        _state.update {
            it.copy(
                bonusVotes = it.bonusVotes + (
                    candidateId to existing.copy(count = existing.count - 1)
                    ),
                notice = null,
                submitError = null,
            )
        }
    }

    fun setBonusNegative(candidateId: Int, negative: Boolean) {
        val existing = _state.value.bonusVotes[candidateId] ?: return
        _state.update {
            it.copy(
                bonusVotes = it.bonusVotes + (
                    candidateId to existing.copy(isNegative = negative)
                    ),
                notice = null,
                submitError = null,
            )
        }
    }

    fun clearBonusVote(candidateId: Int) {
        _state.update {
            it.copy(
                bonusVotes = it.bonusVotes - candidateId,
                notice = null,
                submitError = null,
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
                negativeCandidateIds = if (current.allowNegativeVote) {
                    current.negative
                } else {
                    emptySet()
                },
                bonusVotes = if (current.allowBonusVote) {
                    current.bonusVotes.values.toList()
                } else {
                    emptyList()
                },
            ).onSuccess { votes ->
                val positive = votes.filter { !it.isBonus && !it.isNegative }
                    .map { it.candidateId }
                    .toSet()
                val negative = votes.filter { !it.isBonus && it.isNegative }
                    .map { it.candidateId }
                    .toSet()
                val bonus = votes.filter { it.isBonus && it.bonusCount > 0 }
                    .associate { vote ->
                        vote.candidateId to EdBonusVoteDto(
                            candidateId = vote.candidateId,
                            count = vote.bonusCount,
                            isNegative = vote.isNegative,
                        )
                    }
                _state.update {
                    it.copy(
                        positive = positive,
                        negative = negative,
                        serverPositive = positive,
                        serverNegative = negative,
                        bonusVotes = bonus,
                        serverBonusVotes = bonus,
                        submitting = false,
                        notice = "投票已保存",
                    )
                }
                loadWallet()
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
                    nominationError = null,
                    nominationNotice = null,
                    myNomination = null,
                    nominationCandidates = emptyList(),
                    nominationLoading = false,
                    nominationCandidatesLoading = false,
                    nominationCandidatesError = null,
                    nominationSubmitting = false,
                    eligibility = null,
                    eligibilityError = null,
                    eligibilityLoading = false,
                    bvid = "",
                    name = "",
                    vocalist = "",
                    producer = "",
                    withdrawing = false,
                    submitError = null,
                    notice = null,
                    results = emptyList(),
                    resultsLoading = false,
                    resultsError = null,
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
                            voteExpired = false,
                            voterCount = campaign.voterCount ?: it.voterCount,
                        )
                    }
                    when (campaign.status) {
                        "nomination" -> loadNominationData(loggedIn)
                        "voting" -> loadVotingData(campaign, loggedIn)
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

    private suspend fun loadNominationData(loggedIn: Boolean) {
        if (!loggedIn) {
            _state.update {
                it.copy(
                    myNomination = null,
                    nominationCandidates = emptyList(),
                    nominationLoading = false,
                    nominationCandidatesLoading = false,
                    nominationCandidatesError = null,
                    eligibility = null,
                )
            }
            return
        }

        _state.update {
            it.copy(
                nominationLoading = true,
                nominationCandidatesLoading = true,
                nominationCandidatesError = null,
            )
        }
        repository.loadEdNomination(campaignId)
            .onSuccess { data ->
                _state.update {
                    it.copy(
                        myNomination = data.myNomination,
                        nominationCandidates = data.candidates.data,
                        nominationLoading = false,
                        nominationCandidatesLoading = false,
                    )
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        nominationLoading = false,
                        nominationCandidatesLoading = false,
                        nominationCandidatesError = error.toUserMessage(),
                    )
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
                    bonusVotes = emptyMap(),
                    serverBonusVotes = emptyMap(),
                    allowBonusVote = true,
                    allowNegativeVote = true,
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
                positive = emptySet(),
                negative = emptySet(),
                serverPositive = emptySet(),
                serverNegative = emptySet(),
                bonusVotes = emptyMap(),
                serverBonusVotes = emptyMap(),
                walletTickets = 0,
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
                    .associate { vote ->
                        vote.candidateId to EdBonusVoteDto(
                            candidateId = vote.candidateId,
                            count = vote.bonusCount,
                            isNegative = vote.isNegative,
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
                        serverBonusVotes = bonus,
                        allowBonusVote = data.candidates.mechanism?.config?.allowBonusVote ?: true,
                        allowNegativeVote = data.candidates.mechanism?.config?.allowNegativeVote ?: true,
                        mechanismConfig = data.candidates.mechanism?.config ?: EdMechanismConfigDto(),
                        candidateLoading = false,
                        candidateError = null,
                    )
                }
                if (_state.value.allowBonusVote) loadWallet()
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

    private suspend fun loadResults() {
        _state.update {
            it.copy(
                resultsLoading = true,
                resultsError = null,
                results = emptyList(),
                ranking = emptyList(),
            )
        }
        repository.loadEdResults(campaignId)
            .onSuccess { data ->
                _state.update {
                    it.copy(
                        results = data.results,
                        ranking = data.ranking,
                        resultsLoading = false,
                    )
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

    private suspend fun loadWallet() {
        _state.update { it.copy(walletLoading = true) }
        repository.loadWallet()
            .onSuccess { wallet ->
                _state.update {
                    it.copy(
                        walletTickets = wallet.voteTickets,
                        walletLoading = false,
                    )
                }
            }
            .onFailure {
                _state.update { it.copy(walletLoading = false) }
            }
    }

    private fun scheduleEligibilityCheck() {
        eligibilityJob?.cancel()
        val current = _state.value
        val bvid = current.bvid.trim()
        if (!current.isLoggedIn || !current.nominationOpen || bvid.length < 10) {
            _state.update { it.copy(eligibilityLoading = false) }
            return
        }

        eligibilityJob = viewModelScope.launch {
            delay(500)
            _state.update {
                it.copy(eligibilityLoading = true, eligibilityError = null)
            }
            repository.checkEdEligibility(campaignId, bvid)
                .onSuccess { eligibility ->
                    if (_state.value.bvid.trim() == bvid) {
                        _state.update {
                            it.copy(
                                eligibility = eligibility,
                                eligibilityLoading = false,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    if (_state.value.bvid.trim() == bvid) {
                        _state.update {
                            it.copy(
                                eligibilityLoading = false,
                                eligibilityError = error.toUserMessage(),
                            )
                        }
                    }
                }
        }
    }
}
