package xyz.lisfox.vocaland.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import xyz.lisfox.vocaland.R
import xyz.lisfox.vocaland.ui.theme.ComponentSize
import xyz.lisfox.vocaland.ui.theme.Corners
import xyz.lisfox.vocaland.ui.theme.Spacing
import xyz.lisfox.vocaland.util.toHttps

@Composable
fun NetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    fallbackIcon: ImageVector = Icons.Filled.MusicNote,
) {
    val resolved = toHttps(url)
    BoxWithConstraints(
        modifier = modifier.background(placeholderColor),
        contentAlignment = Alignment.Center,
    ) {
        val useShimmer = maxWidth >= 64.dp && maxHeight >= 64.dp
        if (resolved == null) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        } else {
            var imageState by remember { mutableStateOf<AsyncImagePainter.State?>(null) }
            val loading = imageState == null || imageState is AsyncImagePainter.State.Loading
            val failed = imageState is AsyncImagePainter.State.Error
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(resolved)
                    .crossfade(true)
                    .build(),
                contentDescription = if (failed) null else contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onState = { imageState = it },
            )
            if (loading && useShimmer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(placeholderColor.copy(alpha = 0.92f))
                        .shimmer(),
                )
            } else if (failed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(placeholderColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = contentDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Image(
        painter = painterResource(R.drawable.app_icon),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3)),
        contentScale = ContentScale.Crop,
    )
}

@Composable
fun BrandTitle(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark(size = 30.dp)
        Spacer(Modifier.size(10.dp))
        Column {
            Text(
                text = stringResource(R.string.brand_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.brand_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (action != null) {
            Spacer(Modifier.size(Spacing.md))
            action()
        }
    }
}

@Composable
fun ScreenMessage(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = Corners.xxl,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xxl, vertical = Spacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(32.dp)
                    .clearAndSetSemantics {},
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(Spacing.lg))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun LoadingPane(modifier: Modifier = Modifier, text: String? = null) {
    val label = text ?: stringResource(R.string.loading)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun InlineLoading(modifier: Modifier = Modifier, text: String? = null) {
    val label = text ?: stringResource(R.string.loading)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label },
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun EmptyHint(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = Corners.md,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier
                    .size(ComponentSize.iconSmall)
                    .clearAndSetSemantics {},
            )
            Spacer(Modifier.size(Spacing.sm))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
