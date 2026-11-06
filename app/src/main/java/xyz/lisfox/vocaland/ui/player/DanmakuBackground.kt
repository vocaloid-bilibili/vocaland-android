package xyz.lisfox.vocaland.ui.player

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import xyz.lisfox.vocaland.data.model.DanmakuItem

private const val MAX_ACTIVE = 300
private const val SPEED_PLUS = 0.65f
private const val SAFE_LEFT_DP = 50f
private const val SAFE_GAP_DP = 4f
private const val SAFE_GAP_MS = 100L
private const val TOP_OFFSET_DP = 4f
private const val BOTTOM_OFFSET_DP = 4f
private const val VERTICAL_GAP_DP = 2f
private const val RESYNC_THRESHOLD_MS = 900L
private const val FALLBACK_DURATION_SECONDS = 4.5f

private val DURATION_BOUNDARIES = floatArrayOf(558f, 888f, 1082f, 1246f, 1452f, 1744f, 2314f, 2560f)
private val DURATION_MIN_SECONDS = floatArrayOf(7f, 8f, 9f, 10f, 11f, 13f, 14f, 16f)
private val DURATION_MAX_SECONDS = floatArrayOf(13f, 14f, 14f, 15f, 16f, 18f, 20f, 22f)

@Composable
fun DanmakuBackground(
    danmaku: List<DanmakuItem>,
    cid: Long?,
    playing: Boolean,
    positionMs: Long,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textSizePx = with(density) { 13.sp.toPx() }
    val lightBackground = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val paint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = textSizePx
            typeface = Typeface.DEFAULT
            setShadowLayer(2.5f, 0f, 1f, Color.argb(70, 0, 0, 0))
        }
    }
    val measured = remember(danmaku, paint) {
        danmaku.map { item ->
            MeasuredDanmaku(
                text = item.text,
                timeMs = item.timeMs,
                color = item.color,
                textWidth = paint.measureText(item.text),
            )
        }
    }

    var playheadNanos by remember { mutableLongStateOf(positionMs * 1_000_000L) }
    var wasPlaying by remember { mutableStateOf(playing) }
    var lastPausedPositionMs by remember { mutableLongStateOf(positionMs) }

    LaunchedEffect(cid) {
        playheadNanos = positionMs * 1_000_000L
        lastPausedPositionMs = positionMs
    }

    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        if (abs(playheadNanos / 1_000_000L - positionMs) > RESYNC_THRESHOLD_MS) {
            playheadNanos = positionMs * 1_000_000L
        }
        var previous = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                playheadNanos += now - previous
                previous = now
            }
        }
    }

    LaunchedEffect(positionMs, playing) {
        val playheadMs = playheadNanos / 1_000_000L
        if (playing) {
            wasPlaying = true
            if (abs(playheadMs - positionMs) > RESYNC_THRESHOLD_MS) {
                playheadNanos = positionMs * 1_000_000L
            }
        } else if (wasPlaying) {
            wasPlaying = false
            lastPausedPositionMs = positionMs
        } else if (positionMs != lastPausedPositionMs) {
            playheadNanos = positionMs * 1_000_000L
            lastPausedPositionMs = positionMs
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() else 0f
        val heightPx = if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() else 0f
        val layout = remember(measured, paint, widthPx, heightPx, density) {
            buildLayout(
                measured = measured,
                paint = paint,
                density = density,
                widthPx = widthPx,
                heightPx = heightPx,
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (layout.items.isEmpty() || size.width <= 0f || size.height <= 0f) return@Canvas
            val playheadMs = playheadNanos / 1_000_000L
            val startIndex = lowerBound(layout.items, playheadMs - layout.maxDurationMs)
            var drawn = 0

            for (index in startIndex until layout.items.size) {
                val item = layout.items[index]
                if (item.timeMs > playheadMs) break
                val elapsed = playheadMs - item.timeMs
                if (elapsed > item.durationMs) continue
                if (drawn >= MAX_ACTIVE) break

                val progress = elapsed.toFloat() / item.durationMs.toFloat()
                val x = item.startX - item.distance * progress

                paint.color = lighten(item.color, 0.28f)
                paint.alpha = if (lightBackground) 135 else 115
                drawContext.canvas.nativeCanvas.drawText(item.text, x, item.baseline, paint)
                drawn++
            }
        }
    }
}

private data class MeasuredDanmaku(
    val text: String,
    val timeMs: Long,
    val color: Int,
    val textWidth: Float,
)

private data class PlacedDanmaku(
    val text: String,
    val timeMs: Long,
    val color: Int,
    val textWidth: Float,
    val startX: Float,
    val distance: Float,
    val speedPxPerMs: Float,
    val durationMs: Long,
    val baseline: Float,
) {
    val endMs: Long get() = timeMs + durationMs
}

private data class DanmakuLayout(
    val items: List<PlacedDanmaku>,
    val maxDurationMs: Long,
)

private fun buildLayout(
    measured: List<MeasuredDanmaku>,
    paint: Paint,
    density: Density,
    widthPx: Float,
    heightPx: Float,
): DanmakuLayout {
    if (measured.isEmpty() || widthPx <= 0f || heightPx <= 0f) {
        return DanmakuLayout(emptyList(), 0L)
    }

    val metrics = paint.fontMetrics
    val textHeight = metrics.descent - metrics.ascent
    val laneHeight = textHeight + with(density) { VERTICAL_GAP_DP.dp.toPx() }
    val topOffset = with(density) { TOP_OFFSET_DP.dp.toPx() }
    val bottomOffset = with(density) { BOTTOM_OFFSET_DP.dp.toPx() }
    val safeLeft = with(density) { SAFE_LEFT_DP.dp.toPx() }
    val safeGap = with(density) { SAFE_GAP_DP.dp.toPx() }
    val densityScale = density.density
    val widthDp = widthPx / densityScale
    val laneCount = ((heightPx - topOffset - bottomOffset) / laneHeight)
        .toInt()
        .coerceAtLeast(1)
    val lanes = Array(laneCount) { mutableListOf<PlacedDanmaku>() }
    val items = ArrayList<PlacedDanmaku>(measured.size)
    var maxDurationMs = 0L

    for (item in measured) {
        val textWidthDp = item.textWidth / densityScale
        val baseDuration = durationBaseSeconds(widthDp, textWidthDp)
        val speedDpPerSecond = (widthDp + textWidthDp) / baseDuration * SPEED_PLUS
        val speedPxPerMs = speedDpPerSecond * densityScale / 1000f
        val startX = widthPx
        val distance = startX + item.textWidth + safeLeft
        val durationMs = (distance / speedPxPerMs).toLong().coerceAtLeast(1L)
        val middleMs = item.timeMs + (startX / speedPxPerMs).toLong()

        val laneIndex = lanes.indexOfFirst { lane ->
            canReuseLane(
                lane = lane,
                startMs = item.timeMs,
                startX = startX,
                safeGap = safeGap,
                middleMs = middleMs,
            )
        }
        if (laneIndex < 0) continue

        val baseline = topOffset +
            laneIndex * laneHeight +
            (laneHeight - textHeight) / 2f -
            metrics.ascent
        val placed = PlacedDanmaku(
            text = item.text,
            timeMs = item.timeMs,
            color = item.color,
            textWidth = item.textWidth,
            startX = startX,
            distance = distance,
            speedPxPerMs = speedPxPerMs,
            durationMs = durationMs,
            baseline = baseline,
        )
        lanes[laneIndex].add(placed)
        items.add(placed)
        maxDurationMs = maxOf(maxDurationMs, durationMs)
    }

    return DanmakuLayout(items, maxDurationMs)
}

private fun canReuseLane(
    lane: List<PlacedDanmaku>,
    startMs: Long,
    startX: Float,
    safeGap: Float,
    middleMs: Long,
): Boolean {
    for (previous in lane) {
        if (previous.endMs + SAFE_GAP_MS <= startMs) continue

        val elapsed = (startMs - previous.timeMs).coerceAtLeast(0L)
        val previousX = previous.startX - previous.speedPxPerMs * elapsed
        if (previousX + previous.textWidth + safeGap > startX) return false
        if (middleMs <= previous.endMs + SAFE_GAP_MS) return false
    }
    return true
}

private fun durationBaseSeconds(containerWidthDp: Float, textWidthDp: Float): Float {
    if (containerWidthDp < DURATION_BOUNDARIES[0]) return FALLBACK_DURATION_SECONDS

    val minSeconds: Float
    val maxSeconds: Float
    if (containerWidthDp > DURATION_BOUNDARIES.last()) {
        val extra = ((containerWidthDp - DURATION_BOUNDARIES.last()) / 500f).toInt()
        minSeconds = DURATION_MIN_SECONDS.last() + extra
        maxSeconds = DURATION_MAX_SECONDS.last() + extra
    } else {
        val index = DURATION_BOUNDARIES.indices.firstOrNull {
            containerWidthDp <= DURATION_BOUNDARIES[it]
        } ?: return FALLBACK_DURATION_SECONDS
        minSeconds = DURATION_MIN_SECONDS[index]
        maxSeconds = DURATION_MAX_SECONDS[index]
    }

    val ratio = (textWidthDp / containerWidthDp).coerceIn(0f, 1f)
    val u = (maxSeconds - minSeconds) / 0.1f - 1f
    val pivot = ln(u) / 0.2f + 0.2f
    val duration = minSeconds +
        (maxSeconds - minSeconds) / (1f + exp(-0.2f * (ratio - pivot)))
    return duration.takeIf { it.isFinite() && it > 0f } ?: FALLBACK_DURATION_SECONDS
}

private fun lowerBound(items: List<PlacedDanmaku>, timeMs: Long): Int {
    var low = 0
    var high = items.size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (items[mid].timeMs < timeMs) {
            low = mid + 1
        } else {
            high = mid
        }
    }
    return low
}

private fun lighten(rgb: Int, amount: Float): Int {
    val red = (rgb shr 16) and 0xFF
    val green = (rgb shr 8) and 0xFF
    val blue = rgb and 0xFF
    fun mix(channel: Int): Int = (channel + (255 - channel) * amount).toInt().coerceIn(0, 255)
    return Color.rgb(mix(red), mix(green), mix(blue))
}
