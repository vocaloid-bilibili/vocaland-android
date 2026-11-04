package xyz.lisfox.vocaland.data.player

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import androidx.core.content.ContextCompat
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
import xyz.lisfox.vocaland.playback.BgmPlaybackService

private val PLAYBACK_AUDIO_ATTRIBUTES = AudioAttributes.Builder()
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .build()

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
    private val context: Context,
    private val repository: BgmRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(BgmPlayerState())
    val state: StateFlow<BgmPlayerState> = _state.asStateFlow()

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(PLAYBACK_AUDIO_ATTRIBUTES)
        .setOnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    player?.setVolume(0.2f, 0.2f)
                }

                AudioManager.AUDIOFOCUS_GAIN -> {
                    player?.setVolume(1f, 1f)
                }
            }
        }
        .build()

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
        startPlaybackService()

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
        if (_state.value.playing) pause() else play()
    }

    fun play() {
        if (_state.value.loadingTrack) return
        val current = player
        if (current == null) {
            playAt(_state.value.currentIndex)
            return
        }
        if (runCatching { current.isPlaying }.getOrDefault(false)) return
        if (!requestAudioFocus()) {
            _state.update { it.copy(playing = false, error = "无法获取音频焦点") }
            return
        }
        startPlaybackService()
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

    fun pause() {
        if (_state.value.loadingTrack) {
            prepareJob?.cancel()
            releasePlayer()
            _state.update {
                it.copy(loadingTrack = false, playing = false, positionMs = 0)
            }
            return
        }
        val current = player ?: return
        runCatching { current.pause() }
        abandonAudioFocus()
        _state.update {
            it.copy(
                playing = false,
                positionMs = runCatching { current.currentPosition.toLong() }
                    .getOrDefault(it.positionMs),
            )
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
        mediaPlayer.setAudioAttributes(PLAYBACK_AUDIO_ATTRIBUTES)
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        mediaPlayer.setDataSource(url)
        mediaPlayer.setOnPreparedListener { prepared ->
            if (player !== prepared) return@setOnPreparedListener
            val duration = prepared.duration.toLong().takeIf { it > 0 }
                ?: track.durationSeconds * 1000L
            if (!requestAudioFocus()) {
                _state.update {
                    it.copy(
                        loadingTrack = false,
                        playing = false,
                        durationMs = duration,
                        error = "无法获取音频焦点",
                    )
                }
                return@setOnPreparedListener
            }
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
                releasePlayer()
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

    private fun requestAudioFocus(): Boolean =
        audioManager.requestAudioFocus(focusRequest) ==
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    private fun abandonAudioFocus() {
        runCatching { audioManager.abandonAudioFocusRequest(focusRequest) }
    }

    private fun startPlaybackService() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, BgmPlaybackService::class.java),
        )
    }

    private fun releasePlayer() {
        player?.let { current ->
            runCatching { current.reset() }
            runCatching { current.release() }
        }
        player = null
        abandonAudioFocus()
    }
}
