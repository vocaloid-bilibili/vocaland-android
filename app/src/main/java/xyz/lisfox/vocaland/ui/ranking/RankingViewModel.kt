package xyz.lisfox.vocaland.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.model.RankingBoard
import xyz.lisfox.vocaland.data.model.RankingOrder
import xyz.lisfox.vocaland.data.model.RankingPage
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.RankingRepository

data class RankingUiState(
    val board: RankingBoard = RankingBoard.Weekly,
    val part: String = "main",
    val order: RankingOrder = RankingOrder.Point,
    val issue: Int? = null,
    val latestIssue: Int? = null,
    val page: Int = 1,
    val data: RankingPage? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val issueNotice: String? = null,
)

class RankingViewModel(
    private val repository: RankingRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RankingUiState())
    val state: StateFlow<RankingUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun selectBoard(board: RankingBoard) {
        val current = _state.value
        if (current.board == board) return

        _state.update {
            it.copy(
                board = board,
                part = if (!board.supportsNewPart && it.part == "new") "main" else it.part,
                issue = null,
                page = 1,
                data = null,
                error = null,
                issueNotice = null,
            )
        }
        load()
    }

    fun selectPart(part: String) {
        val current = _state.value
        if (current.part == part) return
        if (part == "new" && !current.board.supportsNewPart) return

        _state.update {
            it.copy(part = part, page = 1, data = null, error = null, issueNotice = null)
        }
        load()
    }

    fun selectOrder(order: RankingOrder) {
        if (_state.value.order == order) return

        _state.update {
            it.copy(order = order, page = 1, data = null, error = null, issueNotice = null)
        }
        load()
    }

    fun previousPage() {
        val data = _state.value.data ?: return
        if (data.page <= 1) return

        _state.update { it.copy(page = data.page - 1) }
        load()
    }

    fun nextPage() {
        val data = _state.value.data ?: return
        if (data.page >= data.totalPages) return

        _state.update { it.copy(page = data.page + 1) }
        load()
    }

    fun previousIssue() {
        val currentIssue = currentIssue() ?: return
        if (currentIssue <= 1) return

        _state.update {
            it.copy(
                issue = currentIssue - 1,
                page = 1,
                data = null,
                error = null,
                issueNotice = null,
            )
        }
        load()
    }

    fun nextIssue() {
        val currentIssue = currentIssue() ?: return
        val latest = _state.value.latestIssue ?: _state.value.data?.latestIssue ?: return
        if (currentIssue >= latest) return

        _state.update {
            it.copy(
                issue = currentIssue + 1,
                page = 1,
                data = null,
                error = null,
                issueNotice = null,
            )
        }
        load()
    }

    fun goToLatestIssue() {
        val latest = _state.value.latestIssue ?: return
        if (currentIssue() == latest) return

        _state.update {
            it.copy(
                issue = latest,
                page = 1,
                data = null,
                error = null,
                issueNotice = null,
            )
        }
        load()
    }

    fun retry() {
        load()
    }

    private fun currentIssue(): Int? =
        _state.value.data?.issue ?: _state.value.issue

    private fun load() {
        val snapshot = _state.value
        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }

        loadJob = viewModelScope.launch {
            repository.loadRanking(
                board = snapshot.board,
                part = snapshot.part,
                order = snapshot.order,
                issue = snapshot.issue,
                page = snapshot.page,
            ).onSuccess { pageData ->
                val requestedIssue = snapshot.issue
                val knownLatest = pageData.latestIssue ?: _state.value.latestIssue
                val issueNotice = when {
                    requestedIssue == null || requestedIssue == pageData.issue -> null
                    requestedIssue > pageData.issue ->
                        "该期尚未发布，已切换到第 ${pageData.issue} 期"

                    pageData.issue == knownLatest ->
                        "当前未登录或权限不足，仅可查看最新一期，已停留在第 ${pageData.issue} 期"

                    else ->
                        "该期不在当前账号可查看范围内，已切换到第 ${pageData.issue} 期"
                }
                _state.update {
                    it.copy(
                        data = pageData,
                        issue = pageData.issue,
                        latestIssue = pageData.latestIssue ?: it.latestIssue,
                        page = pageData.page,
                        loading = false,
                        error = null,
                        issueNotice = issueNotice,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.toUserMessage(),
                    )
                }
            }
        }
    }
}
