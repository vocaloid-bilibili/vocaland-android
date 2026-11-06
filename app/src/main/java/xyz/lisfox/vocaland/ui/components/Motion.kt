package xyz.lisfox.vocaland.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** 全应用统一的动画时长。 */
object MotionDurations {
    const val FAST = 180
    const val QUICK = 200
    const val MEDIUM = 260
    const val EMPHASIZED = 320
    const val SLOW = 360
    const val CAROUSEL_INTERVAL_MS = 5_000L
    const val SHIMMER = 1_200
}

/**
 * 骨架屏扫光效果：在原有背景上叠加一条周期性扫过的高光。
 * 使用方先铺好底色（如 surfaceContainerHighest），再调用本修饰符。
 */
fun Modifier.shimmer(
    durationMillis: Int = MotionDurations.SHIMMER,
    highlightColor: Color? = null,
): Modifier = composed {
    val fallback = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val highlight = highlightColor ?: fallback
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    drawWithContent {
        drawContent()
        val width = size.width
        if (width <= 0f) return@drawWithContent
        val bandWidth = width * 0.45f
        val centerX = (width + bandWidth * 2) * progress - bandWidth
        val brush = Brush.linearGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.5f to highlight,
                1f to Color.Transparent,
            ),
            start = Offset(centerX - bandWidth, 0f),
            end = Offset(centerX + bandWidth, size.height),
        )
        drawRect(brush)
    }
}
