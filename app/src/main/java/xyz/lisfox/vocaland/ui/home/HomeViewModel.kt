package xyz.lisfox.vocaland.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.model.HomeContent
import xyz.lisfox.vocaland.data.model.SongListItemDto
import xyz.lisfox.vocaland.data.model.TOP_STATS
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.HomeRepository

data class HomeUiState(
    val content: HomeContent? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val selectedStat: String = TOP_STATS.first().key,
    val topSongs: List<SongListItemDto> = emptyList(),
    val topSongsLoading: Boolean = false,
    val topSongsError: String? = null,
)

class HomeViewModel(
    private val repository: HomeRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        _state.update {
            it.copy(
                loading = it.content == null,
                refreshing = it.content != null,
                error = null,
            )
        }
        refreshJob = viewModelScope.launch {
            runCatching { repository.loadHome() }
                .onSuccess { content ->
                    _state.update {
                        it.copy(
                            content = content,
                            loading = false,
                            refreshing = false,
                            error = null,
                            selectedStat = content.topStat,
                            topSongs = content.topSongs,
                            topSongsLoading = false,
                            topSongsError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun selectStat(stat: String) {
        val current = _state.value
        if (stat == current.selectedStat || current.topSongsLoading) return

        _state.update {
            it.copy(
                selectedStat = stat,
                topSongsLoading = true,
                topSongsError = null,
            )
        }

        viewModelScope.launch {
            repository.loadTopSongs(stat)
                .onSuccess { songs ->
                    _state.update {
                        if (it.selectedStat != stat) {
                            it
                        } else {
                            it.copy(topSongs = songs, topSongsLoading = false)
                        }
                    }
                }
                .onFailure { error ->
                    _state.update {
                        if (it.selectedStat != stat) {
                            it
                        } else {
                            it.copy(
                                topSongsLoading = false,
                                topSongsError = error.toUserMessage(),
                            )
                        }
                    }
                }
        }
    }
}
