@file:OptIn(ExperimentalSharedTransitionApi::class)

package xyz.lisfox.vocaland.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
    val sharedModifier = if (sharedKey != null && sharedScope != null && visibilityScope != null) {
        with(sharedScope) {
            Modifier.sharedElement(
                state = rememberSharedContentState(sharedKey),
                animatedVisibilityScope = visibilityScope,
            )
        }
    } else {
        Modifier
    }
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box {
            NetworkImage(
                url = thumbnail,
                contentDescription = null,
                modifier = sharedModifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp)),
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
