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
import xyz.lisfox.vocaland.data.model.PlaylistDetailDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.PlaylistRepository

data class PlaylistDetailUiState(
    val detail: PlaylistDetailDto? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class PlaylistDetailViewModel(
    private val repository: PlaylistRepository,
    private val playlistId: Int,
) : ViewModel() {
    private val _state = MutableStateFlow(PlaylistDetailUiState())
    val state: StateFlow<PlaylistDetailUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _state.update { it.copy(loading = it.detail == null, error = null) }
        loadJob = viewModelScope.launch {
            val result = repository.loadDetail(playlistId)
            if (!isActive) return@launch
            result
                .onSuccess { detail ->
                    _state.update {
                        it.copy(
                            detail = detail,
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
