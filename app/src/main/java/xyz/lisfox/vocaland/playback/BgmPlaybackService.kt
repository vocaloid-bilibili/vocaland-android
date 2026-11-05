package xyz.lisfox.vocaland.playback

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.MainActivity
import xyz.lisfox.vocaland.R
import xyz.lisfox.vocaland.VocaLandApplication
import xyz.lisfox.vocaland.data.player.BgmPlayerController
import xyz.lisfox.vocaland.data.player.BgmPlayerState
import xyz.lisfox.vocaland.util.toHttps

class BgmPlaybackService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var playerController: BgmPlayerController
    private lateinit var session: MediaSession
    private var metadataKey: String? = null
    private var notificationKey: String? = null
    private var coverKey: String? = null
    private var coverBitmap: Bitmap? = null
    private val appIcon by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.app_icon)
    }

    override fun onCreate() {
        super.onCreate()
        playerController = (application as VocaLandApplication).container.bgmPlayerController
        createNotificationChannel()
        session = MediaSession(this, MEDIA_SESSION_TAG).apply {
            setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS,
            )
            setSessionActivity(contentIntent())
            setCallback(
                object : MediaSession.Callback() {
                    override fun onPlay() = playerController.play()

                    override fun onPause() = playerController.pause()

                    override fun onStop() = playerController.pause()

                    override fun onSkipToNext() = playerController.next()

                    override fun onSkipToPrevious() = playerController.previous()

                    override fun onSeekTo(pos: Long) = playerController.seekTo(pos)
                },
            )
            isActive = true
        }

        val initial = playerController.state.value
        updateSession(initial)
        updateCover(initial.currentTrack?.cover)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(initial),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )

        scope.launch {
            playerController.state.collect { state ->
                updateSession(state)
                updateCover(state.currentTrack?.cover)
                updateNotification(state)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PREVIOUS -> playerController.previous()
            ACTION_TOGGLE -> playerController.toggle()
            ACTION_NEXT -> playerController.next()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        session.isActive = false
        session.release()
        scope.cancel()
        super.onDestroy()
    }

    private fun updateSession(state: BgmPlayerState) {
        val track = state.currentTrack
        val key = "${track?.bvid}|${state.durationMs}"
        if (key != metadataKey) {
            metadataKey = key
            session.setMetadata(
                MediaMetadata.Builder()
                    .putString(
                        MediaMetadata.METADATA_KEY_TITLE,
                        track?.title ?: DEFAULT_TITLE,
                    )
                    .putString(
                        MediaMetadata.METADATA_KEY_ARTIST,
                        track?.artist ?: "",
                    )
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, state.durationMs)
                    .apply {
                        track?.cover?.takeIf { it.isNotBlank() }?.let { cover ->
                            putString(MediaMetadata.METADATA_KEY_ART_URI, cover)
                            putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, cover)
                        }
                    }
                    .build(),
            )
        }
        session.setPlaybackState(buildPlaybackState(state))
    }

    private fun buildPlaybackState(state: BgmPlayerState): PlaybackState {
        val playbackState = when {
            state.currentTrack == null -> PlaybackState.STATE_NONE
            state.error != null -> PlaybackState.STATE_ERROR
            state.loadingTrack -> PlaybackState.STATE_BUFFERING
            state.playing -> PlaybackState.STATE_PLAYING
            else -> PlaybackState.STATE_PAUSED
        }
        return PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SEEK_TO or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS,
            )
            .setState(
                playbackState,
                state.positionMs,
                if (state.playing) 1f else 0f,
            )
            .setBufferedPosition(state.positionMs)
            .apply {
                state.error?.let { setErrorMessage(it) }
            }
            .build()
    }

    private fun updateCover(coverUrl: String?) {
        val resolved = toHttps(coverUrl)
        if (resolved == coverKey) return
        coverKey = resolved
        coverBitmap = null
        if (resolved == null) {
            notificationKey = null
            return
        }
        scope.launch {
            val bitmap = runCatching {
                val result = Coil.imageLoader(this@BgmPlaybackService).execute(
                    ImageRequest.Builder(this@BgmPlaybackService)
                        .data(resolved)
                        .size(512)
                        .allowHardware(false)
                        .build(),
                )
                (result as? SuccessResult)?.drawable?.toBitmap()
            }.getOrNull()
            if (coverKey != resolved) return@launch
            coverBitmap = bitmap
            notificationKey = null
            updateNotification(playerController.state.value)
        }
    }

    private fun updateNotification(state: BgmPlayerState) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val track = state.currentTrack
        val key = "${track?.bvid}|${state.playing}|${state.loadingTrack}|${state.error}"
        if (key == notificationKey) return
        notificationKey = key
        runCatching {
            NotificationManagerCompat.from(this)
                .notify(NOTIFICATION_ID, buildNotification(state))
        }
    }

    private fun buildNotification(state: BgmPlayerState): Notification {
        val track = state.currentTrack
        val text = when {
            state.loadingTrack -> "正在加载…"
            state.error != null -> state.error
            else -> track?.artist?.takeIf { it.isNotBlank() } ?: "暂无曲目"
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vocaland)
            .setLargeIcon(coverBitmap ?: appIcon)
            .setContentTitle(track?.title ?: DEFAULT_TITLE)
            .setContentText(text)
            .setContentIntent(contentIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_previous,
                    "上一首",
                    serviceIntent(ACTION_PREVIOUS, 1),
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    if (state.playing) {
                        android.R.drawable.ic_media_pause
                    } else {
                        android.R.drawable.ic_media_play
                    },
                    if (state.playing) "暂停" else "播放",
                    serviceIntent(ACTION_TOGGLE, 2),
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_next,
                    "下一首",
                    serviceIntent(ACTION_NEXT, 3),
                ).build(),
            )
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2),
            )
            .build()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "音乐播放",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "显示正在播放的歌曲与播放控制"
            setShowBadge(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun contentIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, BgmPlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        const val CHANNEL_ID = "bgm_playback"
        const val NOTIFICATION_ID = 1001
        const val MEDIA_SESSION_TAG = "vocaland_bgm"
        const val DEFAULT_TITLE = "VocaLand 播放器"
        const val ACTION_PREVIOUS = "xyz.lisfox.vocaland.action.PREVIOUS"
        const val ACTION_TOGGLE = "xyz.lisfox.vocaland.action.TOGGLE"
        const val ACTION_NEXT = "xyz.lisfox.vocaland.action.NEXT"
    }
}
