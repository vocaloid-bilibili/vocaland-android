package xyz.lisfox.vocaland.ui.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.artist.ArtistExtras
import xyz.lisfox.vocaland.data.model.ArtistDetailDto
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.ArtistStatsSummaryDto
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.ArtistDetailRepository
import kotlin.math.ceil

data class ArtistDetailUiState(
    val artist: ArtistDetailDto? = null,
    val stats: ArtistStatsSummaryDto? = null,
    val extras: ArtistExtras = ArtistExtras(),
    val songs: List<SongDto> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val totalPages: Int = 1,
    val loading: Boolean = true,
    val songsLoading: Boolean = false,
    val extrasLoading: Boolean = false,
    val error: String? = null,
)

class ArtistDetailViewModel(
    private val repository: ArtistDetailRepository,
    private val kind: ArtistKind,
    private val artistId: Int,
) : ViewModel() {
    private val _state = MutableStateFlow(ArtistDetailUiState())
    val state: StateFlow<ArtistDetailUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var songsJob: Job? = null

    init {
        load()
    }

    fun retry() {
        if (_state.value.artist == null) {
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
        _state.update {
            it.copy(
                loading = true,
                extrasLoading = true,
                error = null,
            )
        }

        loadJob = viewModelScope.launch {
            coroutineScope {
                val extrasDeferred = async {
                    repository.loadExtras(kind, artistId)
                        .getOrDefault(ArtistExtras())
                }
                val contentResult = repository.load(kind, artistId)

                contentResult
                    .onSuccess { content ->
                        val totalPages = pagesFor(content.songs.total)
                        _state.update {
                            it.copy(
                                artist = content.profile.artist,
                                stats = content.profile.stats,
                                songs = content.songs.songs,
                                page = 1,
                                total = content.songs.total,
                                totalPages = totalPages,
                                loading = false,
                                songsLoading = false,
                                extrasLoading = true,
                                error = null,
                            )
                        }
                    }
                    .onFailure { error ->
                        extrasDeferred.cancel()
                        _state.update {
                            it.copy(
                                loading = false,
                                extrasLoading = false,
                                error = error.toUserMessage(),
                            )
                        }
                    }

                if (contentResult.isSuccess) {
                    val extras = extrasDeferred.await()
                    _state.update {
                        it.copy(
                            extras = extras,
                            extrasLoading = false,
                        )
                    }
                }
            }
        }
    }

    private fun loadSongs(page: Int) {
        songsJob?.cancel()
        _state.update { it.copy(songsLoading = true, error = null) }

        songsJob = viewModelScope.launch {
            repository.loadSongs(kind, artistId, page)
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
