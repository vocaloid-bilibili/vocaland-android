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
import xyz.lisfox.vocaland.data.collection.CollectionDto
import xyz.lisfox.vocaland.data.collection.CollectionRepository
import xyz.lisfox.vocaland.data.remote.toUserMessage

data class CollectionListUiState(
    val items: List<CollectionDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class CollectionListViewModel(
    private val repository: CollectionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CollectionListUiState())
    val state: StateFlow<CollectionListUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _state.update { it.copy(loading = it.items.isEmpty(), error = null) }
        loadJob = viewModelScope.launch {
            repository.loadCollections()
                .onSuccess { collections ->
                    if (!isActive) return@launch
                    _state.update {
                        it.copy(
                            items = collections,
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
}
