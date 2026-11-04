package xyz.lisfox.vocaland.ui.song

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
import xyz.lisfox.vocaland.data.repository.SongDetailRepository

data class SongDetailUiState(
    val song: SongDto? = null,
    val tags: List<TagItemDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class SongDetailViewModel(
    private val repository: SongDetailRepository,
    private val songId: Int,
) : ViewModel() {
    private val _state = MutableStateFlow(SongDetailUiState())
    val state: StateFlow<SongDetailUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }

        loadJob = viewModelScope.launch {
            repository.load(songId)
                .onSuccess { content ->
                    _state.update {
                        it.copy(
                            song = content.song,
                            tags = content.tags,
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
