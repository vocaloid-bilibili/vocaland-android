package xyz.lisfox.vocaland.ui.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.TagDetailRepository
import kotlin.math.ceil

data class TagDetailUiState(
    val tag: TagItemDto? = null,
    val sort: String = "latest",
    val songs: List<SongDto> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val totalPages: Int = 1,
    val loading: Boolean = true,
    val songsLoading: Boolean = false,
    val error: String? = null,
)

class TagDetailViewModel(
    private val repository: TagDetailRepository,
    private val tagId: Int,
) : ViewModel() {
    private val _state = MutableStateFlow(TagDetailUiState())
    val state: StateFlow<TagDetailUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var songsJob: Job? = null

    init {
        load()
    }

    fun selectSort(sort: String) {
        val current = _state.value
        if (current.sort == sort || current.loading) return

        _state.update {
            it.copy(
                sort = sort,
                page = 1,
                songsLoading = true,
                error = null,
            )
        }
        loadSongs(page = 1)
    }

    fun retry() {
        if (_state.value.tag == null) {
            load()
        } else {
            loadSongs(_state.value.page)
        }
    }

    fun previousPage() {
        val current = _state.value
        if (current.page <= 1 || current.songsLoading) return
        loadSongs(current.page - 1)
    }

    fun nextPage() {
        val current = _state.value
        if (current.page >= current.totalPages || current.songsLoading) return
        loadSongs(current.page + 1)
    }

    private fun load() {
        loadJob?.cancel()
        songsJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }

        loadJob = viewModelScope.launch {
            repository.load(tagId, _state.value.sort)
                .onSuccess { content ->
                    _state.update {
                        it.copy(
                            tag = content.tag,
                            songs = content.songs.songs,
                            page = 1,
                            total = content.songs.total,
                            totalPages = pagesFor(content.songs.total),
                            loading = false,
                            songsLoading = false,
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

    private fun loadSongs(page: Int) {
        songsJob?.cancel()
        _state.update { it.copy(songsLoading = true, error = null) }

        songsJob = viewModelScope.launch {
            repository.loadSongs(tagId, page, _state.value.sort)
                .onSuccess { songPage ->
                    _state.update {
                        it.copy(
                            songs = songPage.songs,
                            page = page,
                            total = songPage.total,
                            totalPages = pagesFor(songPage.total),
                            songsLoading = false,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            songsLoading = false,
                            error = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun pagesFor(total: Int): Int =
        maxOf(1, ceil(total.toDouble() / PAGE_SIZE).toInt())

    private companion object {
        const val PAGE_SIZE = 20
    }
}
