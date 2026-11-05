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
import xyz.lisfox.vocaland.data.model.PlaylistSortMode
import xyz.lisfox.vocaland.data.model.PlaylistVisibility
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.PlaylistRepository

data class PlaylistDetailUiState(
    val detail: PlaylistDetailDto? = null,
    val loading: Boolean = true,
    val mutating: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val sortMode: String? = null,
    val deleted: Boolean = false,
)

class PlaylistDetailViewModel(
    private val repository: PlaylistRepository,
    private val playlistId: Int,
) : ViewModel() {
    private val _state = MutableStateFlow(PlaylistDetailUiState())
    val state: StateFlow<PlaylistDetailUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var actionJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _state.update { it.copy(loading = it.detail == null, error = null) }
        loadJob = viewModelScope.launch {
            val result = repository.loadDetail(playlistId, _state.value.sortMode)
            if (!isActive) return@launch
            result
                .onSuccess { detail ->
                    _state.update {
                        it.copy(
                            detail = detail,
                            sortMode = detail.playlist.sortMode,
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

    fun clearNotice() {
        _state.update { it.copy(notice = null) }
    }

    fun consumeDeleted() {
        _state.update { it.copy(deleted = false) }
    }

    fun updateName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        runMutation("名称已更新") {
            repository.updatePlaylist(playlistId, name = trimmed).map { }
        }
    }

    fun updateDescription(description: String?) {
        val value = description?.trim()?.takeIf { it.isNotEmpty() }
        runMutation(if (value == null) "简介已清除" else "简介已更新") {
            repository.updatePlaylist(
                playlistId,
                description = value,
                clearDescription = value == null,
            ).map { }
        }
    }

    fun toggleVisibility() {
        val playlist = _state.value.detail?.playlist ?: return
        val next = PlaylistVisibility.fromKey(playlist.visibility)
        val target = if (next == PlaylistVisibility.Public) {
            PlaylistVisibility.Private
        } else {
            PlaylistVisibility.Public
        }
        runMutation("已设为${target.label}") {
            repository.updatePlaylist(
                playlistId,
                visibility = target.key,
            ).map { }
        }
    }

    fun changeSortMode(mode: PlaylistSortMode) {
        if (_state.value.sortMode == mode.key) return
        runMutation(
            successMessage = "排序方式已更新",
            beforeReload = {
                _state.update { it.copy(sortMode = mode.key) }
            },
        ) {
            repository.updatePlaylist(
                playlistId,
                sortMode = mode.key,
            ).map { }
        }
    }

    fun setCover(bvid: String?) {
        runMutation(if (bvid == null) "封面已清除" else "封面已更新") {
            repository.setCover(playlistId, bvid).map { }
        }
    }

    fun removeSong(songId: Int) {
        runMutation("已移除歌曲") {
            repository.removeSong(playlistId, songId).map { }
        }
    }

    fun saveNote(songId: Int, note: String?) {
        runMutation("备注已保存") {
            repository.saveNote(playlistId, songId, note).map { }
        }
    }

    fun moveUp(index: Int) {
        val songs = _state.value.detail?.songs ?: return
        if (index <= 0 || index >= songs.size) return
        moveSong(songs[index].songId, songs[index - 1].songId)
    }

    fun moveDown(index: Int) {
        val songs = _state.value.detail?.songs ?: return
        if (index < 0 || index >= songs.size - 1) return
        moveSong(songs[index].songId, songs.getOrNull(index + 2)?.songId)
    }

    fun deletePlaylist() {
        if (_state.value.mutating) return
        actionJob?.cancel()
        _state.update { it.copy(mutating = true, notice = null) }
        actionJob = viewModelScope.launch {
            repository.deletePlaylist(playlistId)
                .onSuccess {
                    _state.update { it.copy(mutating = false, deleted = true) }
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

    private fun moveSong(songId: Int, beforeSongId: Int?) {
        runMutation("顺序已更新") {
            repository.moveSong(playlistId, songId, beforeSongId).map { }
        }
    }

    private fun runMutation(
        successMessage: String,
        beforeReload: (() -> Unit)? = null,
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
                        it.copy(
                            mutating = false,
                            notice = _state.value.notice ?: successMessage,
                        )
                    }
                    beforeReload?.invoke()
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
