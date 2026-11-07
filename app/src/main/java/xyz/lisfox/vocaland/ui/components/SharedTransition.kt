@file:OptIn(ExperimentalSharedTransitionApi::class)

package xyz.lisfox.vocaland.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalSharedElementVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** 歌曲封面共享元素的 key：列表卡片与详情页大图必须完全一致才能接上转场。 */
private const val SONG_COVER_KEY_PREFIX = "song-cover-"

fun songCoverSharedKey(songId: Int): String = "$SONG_COVER_KEY_PREFIX$songId"

fun isSongCoverSharedKey(key: Any?): Boolean =
    key?.toString()?.startsWith(SONG_COVER_KEY_PREFIX) == true

/**
 * 共享封面在列表页与详情页统一按详情页大图的宽度解码。
 *
 * 共享元素转场绘制的是**目标页**那份内容：从详情页返回时画的是列表卡片里的小图，
 * 若两端解码尺寸不同，转场开始的一瞬间会从清晰跳成放大的模糊图（看起来像闪一下）。
 */
@Composable
fun sharedCoverDecodeWidth(): Dp {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    return (screenWidthDp - 32.dp).coerceAtLeast(64.dp)
}

/** 列表页已经知道的封面信息，详情页首帧直接拿来占位。 */
data class SharedCoverHint(
    val url: String?,
    val title: String?,
)

private fun <K, V> lru(maxSize: Int): LinkedHashMap<K, V> =
    object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean =
            size > maxSize
    }

/**
 * 列表页正在展示的封面信息，key 与共享元素 key 一致。
 *
 * 详情页在数据到达前没有封面地址与标题，进入转场的第一帧就没有可绘制的内容，
 * 加载稍慢时整个「封面放大」的动画会消失。列表页把当前封面记下来，
 * 详情页首帧先用它占位。
 */
private val sharedCoverHints = lru<String, SharedCoverHint>(32)

fun putSharedCoverHint(key: String, url: String?, title: String?) {
    val resolvedUrl = url?.trim().orEmpty()
    val resolvedTitle = title?.trim().orEmpty()
    if (resolvedUrl.isEmpty() && resolvedTitle.isEmpty()) {
        sharedCoverHints.remove(key)
    } else {
        sharedCoverHints[key] = SharedCoverHint(
            url = resolvedUrl.takeIf { it.isNotEmpty() },
            title = resolvedTitle.takeIf { it.isNotEmpty() },
        )
    }
}

fun getSharedCoverHint(key: String): SharedCoverHint? = sharedCoverHints[key]

/**
 * 已经解码好的封面位图。
 *
 * 转场中绘制的是目标页那一份内容，而目标页是刚组合出来的，`AsyncImage` 第一帧
 * 只有占位色，这就是「闪一下」的来源。这里缓存上一次真正画出来的位图，
 * 目标页首帧先画它，等自己的请求回来再覆盖上去，两端就完全接得上了。
 */
private val sharedCoverBitmaps = lru<String, ImageBitmap>(8)

fun putSharedCoverBitmap(key: String, bitmap: ImageBitmap?) {
    if (bitmap == null) {
        sharedCoverBitmaps.remove(key)
    } else {
        sharedCoverBitmaps[key] = bitmap
    }
}

fun getSharedCoverBitmap(key: String): ImageBitmap? = sharedCoverBitmaps[key]
