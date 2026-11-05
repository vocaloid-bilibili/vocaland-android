package xyz.lisfox.vocaland.ui.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.discovery.TagArtistPage
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.TagDetailRepository

data class TagDetailUiState(
    val tag: TagItemDto? = null,
    val hotSongs: List<SongDto> = emptyList(),
    val hotTotal: Int = 0,
    val latestSongs: List<SongDto> = emptyList(),
    val latestTotal: Int = 0,
    val artists: Map<ArtistKind, TagArtistPage> = emptyMap(),
    val artistPages: Map<ArtistKind, Int> = emptyMap(),
    val artistLoading: ArtistKind? = null,
    val artistErrorKind: ArtistKind? = null,
    val artistError: String? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class TagDetailViewModel(
    private val repository: TagDetailRepository,
    private val tagId: Int,
) : ViewModel() {
    private val _state = MutableStateFlow(TagDetailUiState())
    val state: StateFlow<TagDetailUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var artistJob: Job? = null

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun retryArtist(kind: ArtistKind) {
        loadArtist(kind, _state.value.artistPages[kind] ?: 1)
    }

    fun previousArtistPage(kind: ArtistKind) {
        val page = _state.value.artistPages[kind] ?: 1
        if (page <= 1) return
        loadArtist(kind, page - 1)
    }

    fun nextArtistPage(kind: ArtistKind) {
        val page = _state.value.artistPages[kind] ?: 1
        val totalPages = _state.value.artists[kind]?.totalPages ?: 1
        if (page >= totalPages) return
        loadArtist(kind, page + 1)
    }

    private fun load() {
        loadJob?.cancel()
        artistJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }

        loadJob = viewModelScope.launch {
            repository.load(tagId)
                .onSuccess { content ->
                    _state.update {
                        it.copy(
                            tag = content.tag,
                            hotSongs = content.hot.songs,
                            hotTotal = content.hot.total,
                            latestSongs = content.latest.songs,
                            latestTotal = content.latest.total,
                            artists = content.artists,
                            artistPages = content.artists.keys.associateWith { 1 },
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

    private fun loadArtist(kind: ArtistKind, page: Int) {
        artistJob?.cancel()
        _state.update {
            it.copy(
                artistLoading = kind,
                artistErrorKind = null,
                artistError = null,
            )
        }

        artistJob = viewModelScope.launch {
            repository.loadArtistPage(kind, tagId, page)
                .onSuccess { pageData ->
                    _state.update {
                        it.copy(
                            artists = it.artists + (kind to pageData),
                            artistPages = it.artistPages + (kind to page),
                            artistLoading = null,
                            artistErrorKind = null,
                            artistError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            artistLoading = null,
                            artistErrorKind = kind,
                            artistError = error.toUserMessage(),
                        )
                    }
                }
        }
    }
}
