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

enum class PlayMode(val label: String) {
    SINGLE("单曲循环"),
    LIST("列表循环"),
    SHUFFLE("随机循环"),
    STOP_AFTER_LIST("播完全部暂停"),
    STOP_AFTER_TRACK("播完当前暂停"),
}

data class BgmPlayerState(
    val tracks: List<BgmTrack> = emptyList(),
    val currentIndex: Int = 0,
    val loadingPlaylist: Boolean = false,
    val loadingTrack: Boolean = false,
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val cid: Long? = null,
    val error: String? = null,
    val playMode: PlayMode = PlayMode.LIST,
    val volume: Float = 1f,
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
                    val volume = _state.value.volume * 0.2f
                    player?.setVolume(volume, volume)
                }

                AudioManager.AUDIOFOCUS_GAIN -> {
                    val volume = _state.value.volume
                    player?.setVolume(volume, volume)
                }
            }
        }
        .build()

    private var player: MediaPlayer? = null
    private var loadJob: Job? = null
    private var prepareJob: Job? = null
    private var progressJob: Job? = null
    private var loaded = false
    private val pendingQueueAdds = mutableListOf<BgmTrack>()
    private var pendingPlay: BgmTrack? = null

    fun ensureLoaded() {
        if (loaded || loadJob?.isActive == true) return
        _state.update { it.copy(loadingPlaylist = true, error = null) }
        loadJob = scope.launch {
            val result = repository.loadTracks()
            if (!isActive) return@launch
            result
                .onSuccess { loadedTracks ->
                    loaded = true
                    val current = _state.value
                    val merged = loadedTracks.toMutableList()
                    pendingQueueAdds.forEach { pending ->
                        if (merged.none { it.bvid == pending.bvid }) merged.add(pending)
                    }
                    pendingQueueAdds.clear()

                    var index = current.currentIndex.coerceIn(
                        0,
                        merged.lastIndex.coerceAtLeast(0),
                    )
                    val playTarget = pendingPlay
                    pendingPlay = null
                    if (playTarget != null) {
                        val existing = merged.indexOfFirst { it.bvid == playTarget.bvid }
                        index = if (existing >= 0) {
                            existing
                        } else {
                            merged.add(playTarget)
                            merged.lastIndex
                        }
                    }

                    _state.update {
                        it.copy(
                            tracks = merged,
                            currentIndex = index,
                            loadingPlaylist = false,
                            durationMs = merged.getOrNull(index)?.durationSeconds?.times(1000L) ?: 0,
                            error = null,
                        )
                    }

                    if (playTarget != null) playAt(index)
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
        pendingQueueAdds.clear()
        pendingPlay = null
        prepareJob?.cancel()
        progressJob?.cancel()
        releasePlayer()
        val startIndex = if (_state.value.playMode == PlayMode.SHUFFLE) {
            tracks.indices.random()
        } else {
            0
        }
        _state.update {
            it.copy(
                tracks = tracks,
                currentIndex = startIndex,
                loadingPlaylist = false,
                loadingTrack = false,
                playing = false,
                positionMs = 0,
                cid = null,
                durationMs = tracks[startIndex].durationSeconds * 1000L,
                error = null,
            )
        }
        playAt(startIndex)
    }

    fun setPlayMode(mode: PlayMode) {
        if (_state.value.playMode == mode) return
        _state.update { it.copy(playMode = mode) }
    }

    fun addToQueue(track: BgmTrack): Boolean {
        if (_state.value.tracks.any { it.bvid == track.bvid }) return false
        if (pendingQueueAdds.any { it.bvid == track.bvid }) return false
        if (!loaded) {
            pendingQueueAdds.add(track)
            ensureLoaded()
            return true
        }
        _state.update { it.copy(tracks = it.tracks + track) }
        return true
    }

    fun playTrackNow(track: BgmTrack) {
        val state = _state.value
        if (!loaded) {
            pendingPlay = track
            ensureLoaded()
            return
        }
        val index = state.tracks.indexOfFirst { it.bvid == track.bvid }
        if (index >= 0) {
            playAt(index)
            return
        }
        _state.update { it.copy(tracks = it.tracks + track) }
        playAt(_state.value.tracks.lastIndex)
    }

    fun removeTrack(index: Int) {
        val state = _state.value
        if (index !in state.tracks.indices) return
        val wasCurrent = index == state.currentIndex
        val wasPlaying = state.playing
        val nextTracks = state.tracks.toMutableList().apply { removeAt(index) }

        if (nextTracks.isEmpty()) {
            prepareJob?.cancel()
            progressJob?.cancel()
            releasePlayer()
            _state.update {
                it.copy(
                    tracks = emptyList(),
                    currentIndex = 0,
                    loadingTrack = false,
                    playing = false,
                    positionMs = 0,
                    cid = null,
                    durationMs = 0,
                    error = null,
                )
            }
            return
        }

        val nextIndex = when {
            index < state.currentIndex -> state.currentIndex - 1
            index == state.currentIndex -> index.coerceAtMost(nextTracks.lastIndex)
            else -> state.currentIndex
        }.coerceIn(0, nextTracks.lastIndex)

        if (wasCurrent) {
            prepareJob?.cancel()
            progressJob?.cancel()
            releasePlayer()
            _state.update {
                it.copy(
                    tracks = nextTracks,
                    currentIndex = nextIndex,
                    loadingTrack = false,
                    playing = false,
                    positionMs = 0,
                    cid = null,
                    durationMs = nextTracks[nextIndex].durationSeconds * 1000L,
                )
            }
            if (wasPlaying) playAt(nextIndex)
        } else {
            _state.update {
                it.copy(
                    tracks = nextTracks,
                    currentIndex = nextIndex,
                )
            }
        }
    }

    fun moveTrack(from: Int, to: Int) {
        val state = _state.value
        if (from !in state.tracks.indices || to !in state.tracks.indices || from == to) return
        val currentBvid = state.currentTrack?.bvid
        val nextTracks = state.tracks.toMutableList()
        val moved = nextTracks.removeAt(from)
        nextTracks.add(to, moved)
        val nextIndex = currentBvid
            ?.let { bvid -> nextTracks.indexOfFirst { it.bvid == bvid } }
            ?.takeIf { it >= 0 }
            ?: state.currentIndex
        _state.update {
            it.copy(
                tracks = nextTracks,
                currentIndex = nextIndex,
            )
        }
    }

    fun setVolume(value: Float) {
        val volume = value.coerceIn(0f, 1f)
        _state.update { it.copy(volume = volume) }
        player?.setVolume(volume, volume)
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
                cid = null,
                durationMs = track.durationSeconds * 1000L,
                error = null,
            )
        }
        startPlaybackService()

        prepareJob = scope.launch {
            val result = repository.resolveAudio(track.bvid)
            if (!isActive) return@launch
            result
                .onSuccess { audio ->
                    if (!isActive) return@onSuccess
                    _state.update { it.copy(cid = audio.cid) }
                    runCatching { preparePlayer(audio.url, track) }
                        .onFailure { error ->
                            releasePlayer()
                            _state.update {
                                it.copy(
                                    loadingTrack = false,
                                    playing = false,
                                    cid = null,
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
                it.copy(loadingTrack = false, playing = false, positionMs = 0, cid = null)
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

    private fun handleTrackCompleted() {
        val state = _state.value
        if (state.tracks.isEmpty()) return
        when (state.playMode) {
            PlayMode.SINGLE -> restartCurrentTrack()
            PlayMode.LIST -> {
                playAt((state.currentIndex + 1) % state.tracks.size)
            }

            PlayMode.SHUFFLE -> playNextShuffle()
            PlayMode.STOP_AFTER_LIST -> {
                if (state.currentIndex < state.tracks.lastIndex) {
                    playAt(state.currentIndex + 1)
                } else {
                    finishSequence()
                }
            }

            PlayMode.STOP_AFTER_TRACK -> finishSequence()
        }
    }

    private fun restartCurrentTrack() {
        val current = player
        if (current == null) {
            playAt(_state.value.currentIndex)
            return
        }
        if (!requestAudioFocus()) {
            _state.update { it.copy(playing = false, error = "无法获取音频焦点") }
            return
        }
        runCatching {
            current.seekTo(0)
            current.start()
        }.onSuccess {
            _state.update { it.copy(playing = true, positionMs = 0, error = null) }
            startProgressUpdates()
        }.onFailure { error ->
            _state.update {
                it.copy(playing = false, error = error.message ?: "播放失败")
            }
        }
    }

    private fun finishSequence() {
        runCatching { player?.seekTo(0) }
        abandonAudioFocus()
        _state.update { it.copy(playing = false, positionMs = 0) }
    }

    private fun playNextShuffle() {
        val state = _state.value
        if (state.tracks.isEmpty()) return
        if (state.tracks.size == 1) {
            restartCurrentTrack()
            return
        }
        playAt(randomShuffleIndex(state))
    }

    private fun randomShuffleIndex(state: BgmPlayerState): Int =
        state.tracks.indices.filter { it != state.currentIndex }.random()

    private fun preparePlayer(url: String, track: BgmTrack) {
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        mediaPlayer.setAudioAttributes(PLAYBACK_AUDIO_ATTRIBUTES)
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        mediaPlayer.setVolume(_state.value.volume, _state.value.volume)
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
            handleTrackCompleted()
        }
        mediaPlayer.setOnErrorListener { failed, _, _ ->
            if (player === failed) {
                releasePlayer()
                _state.update {
                    it.copy(
                        loadingTrack = false,
                        playing = false,
                        cid = null,
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
