package xyz.lisfox.vocaland.ui.voting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.EdHistoryPage
import xyz.lisfox.vocaland.data.repository.TrendingHistoryPage
import xyz.lisfox.vocaland.data.repository.VotingHubData
import xyz.lisfox.vocaland.data.repository.VotingRepository

data class VotingHubUiState(
    val loading: Boolean = true,
    val data: VotingHubData? = null,
    val error: String? = null,
    val trendingHistory: TrendingHistoryPage? = null,
    val trendingHistoryLoading: Boolean = false,
    val trendingHistoryError: String? = null,
    val edHistory: EdHistoryPage? = null,
    val edHistoryLoading: Boolean = false,
    val edHistoryError: String? = null,
    val edHistoryBoard: String = "vocaloid-weekly",
    val edHistoryExpanded: Boolean = false,
    val trendingHistoryExpanded: Boolean = false,
)

class VotingHubViewModel(
    private val repository: VotingRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(VotingHubUiState())
    val state: StateFlow<VotingHubUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var trendingHistoryJob: Job? = null
    private var edHistoryJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadHub()
        if (_state.value.trendingHistoryExpanded) loadTrendingHistory(1)
        if (_state.value.edHistoryExpanded) loadEdHistory(_state.value.edHistoryBoard, 1)
    }

    fun toggleEdHistoryExpanded() {
        val next = !_state.value.edHistoryExpanded
        _state.update { it.copy(edHistoryExpanded = next) }
        if (next && _state.value.edHistory == null && _state.value.edHistoryError == null) {
            loadEdHistory(_state.value.edHistoryBoard, 1)
        }
    }

    fun toggleTrendingHistoryExpanded() {
        val next = !_state.value.trendingHistoryExpanded
        _state.update { it.copy(trendingHistoryExpanded = next) }
        if (next && _state.value.trendingHistory == null && _state.value.trendingHistoryError == null) {
            loadTrendingHistory(1)
        }
    }

    private fun loadHub() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repository.loadHub()
                .onSuccess { data ->
                    _state.update {
                        it.copy(loading = false, data = data, error = null)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(loading = false, error = error.toUserMessage())
                    }
                }
        }
    }

    fun loadTrendingHistory(page: Int) {
        trendingHistoryJob?.cancel()
        trendingHistoryJob = viewModelScope.launch {
            _state.update { it.copy(trendingHistoryLoading = true, trendingHistoryError = null) }
            repository.loadTrendingHistory(page = page)
                .onSuccess { history ->
                    _state.update {
                        it.copy(
                            trendingHistory = history,
                            trendingHistoryLoading = false,
                            trendingHistoryError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            trendingHistoryLoading = false,
                            trendingHistoryError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun loadEdHistory(board: String = _state.value.edHistoryBoard, page: Int) {
        edHistoryJob?.cancel()
        edHistoryJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    edHistoryBoard = board,
                    edHistory = if (it.edHistoryBoard == board) it.edHistory else null,
                    edHistoryLoading = true,
                    edHistoryError = null,
                )
            }
            repository.loadEdHistory(board = board, page = page)
                .onSuccess { history ->
                    _state.update {
                        it.copy(
                            edHistory = history,
                            edHistoryLoading = false,
                            edHistoryError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            edHistoryLoading = false,
                            edHistoryError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun selectEdHistoryBoard(board: String) {
        if (board == _state.value.edHistoryBoard) return
        loadEdHistory(board = board, page = 1)
    }

    fun previousTrendingHistoryPage() {
        val history = _state.value.trendingHistory ?: return
        if (history.page > 1) loadTrendingHistory(history.page - 1)
    }

    fun nextTrendingHistoryPage() {
        val history = _state.value.trendingHistory ?: return
        if (history.page < history.totalPages) loadTrendingHistory(history.page + 1)
    }

    fun previousEdHistoryPage() {
        val history = _state.value.edHistory ?: return
        if (history.page > 1) {
            loadEdHistory(board = _state.value.edHistoryBoard, page = history.page - 1)
        }
    }

    fun nextEdHistoryPage() {
        val history = _state.value.edHistory ?: return
        if (history.page < history.totalPages) {
            loadEdHistory(board = _state.value.edHistoryBoard, page = history.page + 1)
        }
    }
}
