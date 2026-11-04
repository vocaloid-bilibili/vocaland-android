package xyz.lisfox.vocaland.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.model.PublicPlaylistDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.PlaylistRepository

data class PlaylistUiState(
    val query: String = "",
    val sort: String = "hot",
    val page: Int = 1,
    val perPage: Int = 24,
    val total: Int = 0,
    val items: List<PublicPlaylistDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
) {
    val totalPages: Int
        get() = if (perPage <= 0) 1 else ((total + perPage - 1) / perPage).coerceAtLeast(1)
}

class PlaylistViewModel(
    private val repository: PlaylistRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PlaylistUiState())
    val state: StateFlow<PlaylistUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load(1)
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun submitSearch() {
        load(1)
    }

    fun setSort(sort: String) {
        if (sort == _state.value.sort) return
        _state.update { it.copy(sort = sort) }
        load(1)
    }

    fun refresh() {
        load(1)
    }

    fun previousPage() {
        val state = _state.value
        if (!state.loading && state.page > 1) load(state.page - 1)
    }

    fun nextPage() {
        val state = _state.value
        if (!state.loading && state.page < state.totalPages) load(state.page + 1)
    }

    private fun load(page: Int) {
        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            val current = _state.value
            val result = repository.loadPublicPage(
                page = page,
                sort = current.sort,
                keyword = current.query,
            )
            if (!isActive) return@launch
            result
                .onSuccess { content ->
                    _state.update {
                        it.copy(
                            page = content.page,
                            perPage = content.perPage,
                            total = content.total,
                            items = content.items,
                            loading = false,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
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
