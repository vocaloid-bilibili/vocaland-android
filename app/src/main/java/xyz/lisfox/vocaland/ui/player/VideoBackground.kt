package xyz.lisfox.vocaland.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.SystemClock
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

private const val VIDEO_SYNC_THRESHOLD_MS = 900L
private const val FRAME_INTERVAL_MS = 1_000L
private const val FRAME_FADE_MS = 500
private const val FRAME_WIDTH = 320
private const val FRAME_HEIGHT = 180

@Composable
internal fun VideoBackground(
    videoUrl: String?,
    playing: Boolean,
    positionMs: Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember(context) { FrameVideoPlayer(context) }
    var previousFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    val fade = remember { Animatable(1f) }
    val videoUrlState by rememberUpdatedState(videoUrl)
    val playingState by rememberUpdatedState(playing)
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    LaunchedEffect(player) {
        while (isActive) {
            val startedAt = SystemClock.elapsedRealtime()
            val url = videoUrlState
            if (url != null && playingState) {
                val bitmap = player.captureFrame(url, FRAME_WIDTH, FRAME_HEIGHT)
                if (bitmap != null) {
                    val nextFrame = bitmap.asImageBitmap()
                    val hasPrevious = currentFrame != null
                    previousFrame = currentFrame
                    currentFrame = nextFrame
                    if (hasPrevious) {
                        fade.snapTo(0f)
                        fade.animateTo(1f, tween(durationMillis = FRAME_FADE_MS))
                        previousFrame = null
                    } else {
                        fade.snapTo(1f)
                    }
                }
            }
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            delay((FRAME_INTERVAL_MS - elapsed).coerceAtLeast(0L))
        }
    }
    Box(modifier = modifier) {
        previousFrame?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(24.dp),
            )
        }
        currentFrame?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(24.dp)
                    .alpha(fade.value),
            )
        }
        if (currentFrame != null || previousFrame != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f)),
            )
        }
        AndroidView(
            factory = { player.createView() },
            update = { player.sync(videoUrl, playing, positionMs) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private class FrameTextureView(context: Context) : TextureView(context) {
    override fun onTouchEvent(event: MotionEvent): Boolean = false
}

private class FrameVideoPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val viewContext = context
    private val mediaPlayer = MediaPlayer()
    private var textureView: FrameTextureView? = null
    private var surface: Surface? = null
    private var currentUrl: String? = null
    private var prepared = false
    private var playWhenReady = false
    private var pendingPositionMs = 0L
    private var released = false

    fun createView(): FrameTextureView {
        val view = FrameTextureView(viewContext)
        view.alpha = 0f
        view.isFocusable = false
        view.isClickable = false
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int,
            ) {
                attachSurface(surfaceTexture)
            }

            override fun onSurfaceTextureSizeChanged(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int,
            ) = Unit

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                detachSurface()
                return true
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
        }
        textureView = view
        return view
    }

    fun captureFrame(videoUrl: String, width: Int, height: Int): Bitmap? {
        if (videoUrl != currentUrl) return null
        val view = textureView ?: return null
        return runCatching { view.getBitmap(width, height) }.getOrNull()
    }

    fun sync(videoUrl: String?, playing: Boolean, positionMs: Long) {
        if (released) return
        if (videoUrl != currentUrl) {
            load(videoUrl, playing, positionMs)
            return
        }

        playWhenReady = playing
        if (!prepared) {
            pendingPositionMs = positionMs.coerceAtLeast(0L)
            return
        }

        val currentPosition = runCatching { mediaPlayer.currentPosition.toLong() }.getOrDefault(0L)
        if (positionMs > 0L && abs(currentPosition - positionMs) > VIDEO_SYNC_THRESHOLD_MS) {
            runCatching { mediaPlayer.seekTo(positionMs.toInt()) }
        }
        val isPlaying = runCatching { mediaPlayer.isPlaying }.getOrDefault(false)
        when {
            playing && !isPlaying -> runCatching { mediaPlayer.start() }
            !playing && isPlaying -> runCatching { mediaPlayer.pause() }
            else -> Unit
        }
    }

    private fun load(videoUrl: String?, playing: Boolean, positionMs: Long) {
        currentUrl = videoUrl
        prepared = false
        playWhenReady = playing
        pendingPositionMs = positionMs.coerceAtLeast(0L)
        runCatching { mediaPlayer.reset() }
        applySurface()
        if (videoUrl == null) return
        runCatching {
            mediaPlayer.setVolume(0f, 0f)
            mediaPlayer.isLooping = false
            mediaPlayer.setDataSource(appContext, Uri.parse(videoUrl))
            mediaPlayer.setOnPreparedListener { preparedPlayer ->
                prepared = true
                preparedPlayer.setVolume(0f, 0f)
                if (pendingPositionMs > 0L) {
                    runCatching { preparedPlayer.seekTo(pendingPositionMs.toInt()) }
                }
                if (playWhenReady) {
                    runCatching { preparedPlayer.start() }
                }
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                prepared = false
                true
            }
            mediaPlayer.prepareAsync()
        }.onFailure {
            prepared = false
        }
    }

    private fun attachSurface(surfaceTexture: SurfaceTexture) {
        if (released) return
        surface?.release()
        val newSurface = Surface(surfaceTexture)
        surface = newSurface
        applySurface()
    }

    private fun applySurface() {
        if (released) return
        runCatching { mediaPlayer.setSurface(surface) }
    }

    private fun detachSurface() {
        if (released) return
        runCatching { mediaPlayer.setSurface(null) }
        surface?.release()
        surface = null
    }

    fun release() {
        released = true
        runCatching { mediaPlayer.release() }
        surface?.release()
        surface = null
        textureView = null
    }
}
