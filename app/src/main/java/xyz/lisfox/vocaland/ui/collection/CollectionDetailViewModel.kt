package xyz.lisfox.vocaland.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.collection.CollectionEntry
import xyz.lisfox.vocaland.data.collection.CollectionHeader
import xyz.lisfox.vocaland.data.collection.CollectionRepository
import xyz.lisfox.vocaland.data.collection.CollectionTarget
import xyz.lisfox.vocaland.data.remote.toUserMessage
import kotlin.math.ceil

data class CollectionDetailUiState(
    val header: CollectionHeader? = null,
    val entries: List<CollectionEntry> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val totalPages: Int = 1,
    val loading: Boolean = true,
    val error: String? = null,
)

class CollectionDetailViewModel(
    private val repository: CollectionRepository,
    private val target: CollectionTarget,
) : ViewModel() {
    private val _state = MutableStateFlow(CollectionDetailUiState())
    val state: StateFlow<CollectionDetailUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load(1)
    }

    fun load(page: Int = _state.value.page) {
        loadJob?.cancel()
        _state.update { it.copy(loading = it.header == null, error = null) }
        loadJob = viewModelScope.launch {
            repository.loadDetail(target = target, page = page)
                .onSuccess { result ->
                    if (!isActive) return@launch
                    _state.update {
                        it.copy(
                            header = result.header,
                            entries = result.entries,
                            page = page,
                            total = result.total,
                            totalPages = pagesFor(result.total, result.pageSize),
                            loading = false,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    if (!isActive) return@launch
                    _state.update {
                        it.copy(
                            loading = false,
                            error = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun previousPage() {
        val state = _state.value
        if (!state.loading && state.page > 1) load(state.page - 1)
    }

    fun nextPage() {
        val state = _state.value
        if (!state.loading && state.page < state.totalPages) load(state.page + 1)
    }

    private fun pagesFor(total: Int, pageSize: Int): Int {
        if (pageSize <= 0) return 1
        return maxOf(1, ceil(total.toDouble() / pageSize).toInt())
    }
}
