package xyz.lisfox.vocaland.ui.player

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs

private const val VIDEO_SYNC_THRESHOLD_MS = 900L

@Composable
internal fun VideoBackground(
    videoUrl: String?,
    playing: Boolean,
    positionMs: Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val blurRadiusPx = with(LocalDensity.current) { 24.dp.toPx() }
    val player = remember(context) { VideoBackgroundPlayer(context) }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = { player.createView(blurRadiusPx) },
        update = { player.sync(videoUrl, playing, positionMs) },
        modifier = modifier,
    )
}

private class VideoBackgroundPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val viewContext = context
    private val mediaPlayer = MediaPlayer()
    private var surface: Surface? = null
    private var currentUrl: String? = null
    private var prepared = false
    private var playWhenReady = false
    private var pendingPositionMs = 0L
    private var released = false

    fun createView(blurRadiusPx: Float): TextureView {
        val view = TextureView(viewContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(
                RenderEffect.createBlurEffect(blurRadiusPx, blurRadiusPx, Shader.TileMode.CLAMP),
            )
        }
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
        return view
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
    }
}
