package xyz.lisfox.vocaland.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
private const val FRAME_INTERVAL_MS = 200L
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
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    LaunchedEffect(player, videoUrl) {
        frame = null
        if (videoUrl == null) return@LaunchedEffect
        while (isActive) {
            val bitmap = player.captureFrame(videoUrl, FRAME_WIDTH, FRAME_HEIGHT)
            if (bitmap != null) {
                frame = bitmap.asImageBitmap()
            }
            delay(FRAME_INTERVAL_MS)
        }
    }
    Box(modifier = modifier) {
        Crossfade(
            targetState = frame,
            animationSpec = tween(durationMillis = FRAME_INTERVAL_MS.toInt()),
            label = "videoFrame",
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(24.dp),
                )
            }
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
