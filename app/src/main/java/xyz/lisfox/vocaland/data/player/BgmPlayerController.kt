package xyz.lisfox.vocaland.data.player

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.model.BgmTrack
import xyz.lisfox.vocaland.data.repository.BgmRepository

data class BgmPlayerState(
    val tracks: List<BgmTrack> = emptyList(),
    val currentIndex: Int = 0,
    val loadingPlaylist: Boolean = false,
    val loadingTrack: Boolean = false,
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val error: String? = null,
) {
    val currentTrack: BgmTrack?
        get() = tracks.getOrNull(currentIndex)
}

class BgmPlayerController(
    private val repository: BgmRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(BgmPlayerState())
    val state: StateFlow<BgmPlayerState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var loadJob: Job? = null
    private var prepareJob: Job? = null
    private var progressJob: Job? = null
    private var loaded = false

    fun ensureLoaded() {
        if (loaded || loadJob?.isActive == true) return
        _state.update { it.copy(loadingPlaylist = true, error = null) }
        loadJob = scope.launch {
            val result = repository.loadTracks()
            if (!isActive) return@launch
            result
                .onSuccess { tracks ->
                    loaded = true
                    _state.update { current ->
                        val index = current.currentIndex.coerceIn(0, tracks.lastIndex.coerceAtLeast(0))
                        current.copy(
                            tracks = tracks,
                            currentIndex = index,
                            loadingPlaylist = false,
                            durationMs = tracks.getOrNull(index)?.durationSeconds?.times(1000L) ?: 0,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    loaded = false
                    _state.update {
                        it.copy(
                            loadingPlaylist = false,
                            error = error.message ?: "播放列表加载失败",
                        )
                    }
                }
        }
    }

    fun retry() {
        if (loadJob?.isActive == true) return
        loaded = false
        ensureLoaded()
    }

    fun replaceQueue(tracks: List<BgmTrack>) {
        if (tracks.isEmpty()) return
        loadJob?.cancel()
        loaded = true
        prepareJob?.cancel()
        progressJob?.cancel()
        releasePlayer()
        _state.update {
            it.copy(
                tracks = tracks,
                currentIndex = 0,
                loadingPlaylist = false,
                loadingTrack = false,
                playing = false,
                positionMs = 0,
                durationMs = tracks.first().durationSeconds * 1000L,
                error = null,
            )
        }
        playAt(0)
    }

    fun playAt(index: Int) {
        val tracks = _state.value.tracks
        if (index !in tracks.indices) return
        val track = tracks[index]

        prepareJob?.cancel()
        progressJob?.cancel()
        releasePlayer()
        _state.update {
            it.copy(
                currentIndex = index,
                loadingTrack = true,
                playing = false,
                positionMs = 0,
                durationMs = track.durationSeconds * 1000L,
                error = null,
            )
        }

        prepareJob = scope.launch {
            val result = repository.resolveAudioUrl(track.bvid)
            if (!isActive) return@launch
            result
                .onSuccess { url ->
                    if (!isActive) return@onSuccess
                    runCatching { preparePlayer(url, track) }
                        .onFailure { error ->
                            releasePlayer()
                            _state.update {
                                it.copy(
                                    loadingTrack = false,
                                    playing = false,
                                    error = error.message ?: "播放失败",
                                )
                            }
                        }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loadingTrack = false,
                            playing = false,
                            error = error.message ?: "音频解析失败",
                        )
                    }
                }
        }
    }

    fun toggle() {
        if (_state.value.loadingTrack) return
        val current = player
        if (current == null) {
            playAt(_state.value.currentIndex)
            return
        }
        val isPlaying = runCatching { current.isPlaying }.getOrDefault(false)
        if (isPlaying) {
            runCatching { current.pause() }
            _state.update {
                it.copy(
                    playing = false,
                    positionMs = runCatching { current.currentPosition.toLong() }
                        .getOrDefault(it.positionMs),
                )
            }
        } else {
            runCatching { current.start() }
                .onSuccess {
                    _state.update { it.copy(playing = true, error = null) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(playing = false, error = error.message ?: "播放失败")
                    }
                }
        }
    }

    fun previous() {
        val state = _state.value
        if (state.tracks.isEmpty()) return
        val index = (state.currentIndex - 1 + state.tracks.size) % state.tracks.size
        playAt(index)
    }

    fun next() {
        val state = _state.value
        if (state.tracks.isEmpty()) return
        val index = (state.currentIndex + 1) % state.tracks.size
        playAt(index)
    }

    fun seekTo(positionMs: Long) {
        val current = player ?: return
        val duration = _state.value.durationMs
        val target = positionMs.coerceIn(0, duration.coerceAtLeast(0))
        runCatching { current.seekTo(target.toInt()) }
            .onSuccess { _state.update { it.copy(positionMs = target) } }
    }

    private fun preparePlayer(url: String, track: BgmTrack) {
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build(),
        )
        mediaPlayer.setDataSource(url)
        mediaPlayer.setOnPreparedListener { prepared ->
            if (player !== prepared) return@setOnPreparedListener
            val duration = prepared.duration.toLong().takeIf { it > 0 }
                ?: track.durationSeconds * 1000L
            runCatching { prepared.start() }
                .onSuccess {
                    _state.update {
                        it.copy(
                            loadingTrack = false,
                            playing = true,
                            positionMs = 0,
                            durationMs = duration,
                            error = null,
                        )
                    }
                    startProgressUpdates()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loadingTrack = false,
                            playing = false,
                            error = error.message ?: "播放失败",
                        )
                    }
                }
        }
        mediaPlayer.setOnCompletionListener { completed ->
            if (player !== completed) return@setOnCompletionListener
            val state = _state.value
            if (state.tracks.isEmpty()) return@setOnCompletionListener
            playAt((state.currentIndex + 1) % state.tracks.size)
        }
        mediaPlayer.setOnErrorListener { failed, _, _ ->
            if (player === failed) {
                _state.update {
                    it.copy(
                        loadingTrack = false,
                        playing = false,
                        error = "播放失败",
                    )
                }
            }
            true
        }
        mediaPlayer.prepareAsync()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val current = player
                if (current != null) {
                    val position = runCatching { current.currentPosition.toLong() }.getOrDefault(0)
                    val duration = runCatching { current.duration.toLong() }.getOrDefault(0)
                    _state.update {
                        it.copy(
                            positionMs = position,
                            durationMs = if (duration > 0) duration else it.durationMs,
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun releasePlayer() {
        player?.let { current ->
            runCatching { current.reset() }
            runCatching { current.release() }
        }
        player = null
    }
}
