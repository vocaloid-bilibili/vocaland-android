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
import xyz.lisfox.vocaland.data.model.PlaylistInfoDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.PlaylistRepository

data class MyPlaylistsUiState(
    val items: List<PlaylistInfoDto> = emptyList(),
    val limit: Int = 0,
    val loading: Boolean = true,
    val mutating: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

class MyPlaylistsViewModel(
    private val repository: PlaylistRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(MyPlaylistsUiState())
    val state: StateFlow<MyPlaylistsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var actionJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _state.update { it.copy(loading = it.items.isEmpty(), error = null) }
        loadJob = viewModelScope.launch {
            repository.loadMyPlaylists()
                .onSuccess { page ->
                    if (!isActive) return@launch
                    _state.update {
                        it.copy(
                            items = page.items,
                            limit = page.limit,
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

    fun clearNotice() {
        _state.update { it.copy(notice = null) }
    }

    fun create(name: String, description: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        runMutation("歌单已创建") {
            repository.createPlaylist(trimmed, description).map { }
        }
    }

    fun update(
        id: Int,
        name: String,
        description: String?,
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        runMutation("歌单已更新") {
            repository.updatePlaylist(
                id = id,
                name = trimmed,
                description = description,
                clearDescription = description == null,
            ).map { }
        }
    }

    fun delete(id: Int) {
        runMutation("歌单已删除") {
            repository.deletePlaylist(id)
        }
    }

    private fun runMutation(
        successMessage: String,
        block: suspend () -> Result<Unit>,
    ) {
        if (_state.value.mutating) return
        actionJob?.cancel()
        _state.update { it.copy(mutating = true, notice = null) }
        actionJob = viewModelScope.launch {
            val result = block()
            if (!isActive) return@launch
            result
                .onSuccess {
                    _state.update {
                        it.copy(mutating = false, notice = successMessage)
                    }
                    load()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            mutating = false,
                            notice = error.toUserMessage(),
                        )
                    }
                }
        }
    }
}
