@file:OptIn(ExperimentalSharedTransitionApi::class)

package xyz.lisfox.vocaland.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.lisfox.vocaland.ui.theme.Spacing

@Composable
fun SongCoverCard(
    title: String,
    subtitle: String,
    thumbnail: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedKey: Any? = null,
    badge: String? = null,
    value: String? = null,
) {
    val sharedScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalSharedElementVisibilityScope.current
    val sharedModifier: Modifier
    val shared: Boolean
    if (sharedKey != null && sharedScope != null && visibilityScope != null) {
        shared = true
        sharedModifier = with(sharedScope) {
            Modifier.sharedElement(
                state = rememberSharedContentState(sharedKey),
                animatedVisibilityScope = visibilityScope,
            )
        }
    } else {
        shared = false
        sharedModifier = Modifier
    }
    if (shared) {
        val key = sharedKey.toString()
        SideEffect { putSharedCoverHint(key, thumbnail, title) }
    }
    val decodeWidth = if (isSongCoverSharedKey(sharedKey)) sharedCoverDecodeWidth() else null
    val coverKey = sharedKey?.toString()?.takeIf { isSongCoverSharedKey(it) }
    val cachedCover = remember(coverKey) { coverKey?.let { getSharedCoverBitmap(it) } }

    Column(modifier = modifier.clickable(onClick = onClick)) {
        // 角标画在共享元素内部：返回列表时它会跟着封面一起飞回去，
        // 否则会被转场中绘制在上层的封面挡住，等封面落位后才闪出来
        Box(
            modifier = sharedModifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            NetworkImage(
                url = thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                decodeWidth = decodeWidth,
                cachedBitmap = cachedCover,
                onLoaded = { bitmap -> coverKey?.let { putSharedCoverBitmap(it, bitmap) } },
            )
            if (badge != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.sm),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (value != null) {
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
