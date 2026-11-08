package xyz.lisfox.vocaland.ui.components

import android.graphics.drawable.BitmapDrawable
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import coil.size.Size
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
    decodeWidth: Dp? = null,
    cachedBitmap: ImageBitmap? = null,
    onLoaded: ((ImageBitmap) -> Unit)? = null,
) {
    val resolved = toHttps(url)
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier.background(placeholderColor),
        contentAlignment = Alignment.Center,
    ) {
        val useShimmer = maxWidth >= 64.dp && maxHeight >= 64.dp
        // 共享元素两端需要同一份解码结果，显式指定解码宽度而不是跟随当前布局尺寸
        val requestSize = decodeWidth
            ?.takeIf { maxWidth > 0.dp && maxHeight.value.isFinite() }
            ?.let { target ->
                val scale = target / maxWidth
                Size(
                    with(density) { target.roundToPx() }.coerceAtLeast(1),
                    with(density) { (maxHeight * scale).roundToPx() }.coerceAtLeast(1),
                )
            }
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
            // 先把上一次解码好的位图铺上，请求回来后再由 AsyncImage 覆盖，
            // 避免共享元素转场的第一帧只画出一个占位色块
            val cached = cachedBitmap?.takeIf { imageState !is AsyncImagePainter.State.Success }
            if (cached != null) {
                Image(
                    bitmap = cached,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
            }
            // 未显式指定解码宽度时，跟随布局尺寸降采样，减少内存与解码耗时；
            // 小图关闭 crossfade，避免缩略图闪烁
            val effectiveSize = requestSize ?: run {
                if (maxWidth > 0.dp && maxHeight.value.isFinite()) {
                    Size(
                        with(density) { maxWidth.toPx().toInt() }.coerceAtLeast(1),
                        with(density) { maxHeight.toPx().toInt() }.coerceAtLeast(1),
                    )
                } else {
                    null
                }
            }
            val isSmall = maxWidth < 96.dp || maxHeight < 96.dp
            val request = ImageRequest.Builder(LocalContext.current)
                .data(resolved)
                .crossfade(!isSmall)
            effectiveSize?.let { request.size(it) }
            AsyncImage(
                model = request.build(),
                contentDescription = if (failed) null else contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onState = { state ->
                    imageState = state
                    if (state is AsyncImagePainter.State.Success && onLoaded != null) {
                        val bitmap = (state.result.drawable as? BitmapDrawable)?.bitmap
                        if (bitmap != null) onLoaded(bitmap.asImageBitmap())
                    }
                },
            )
            if (loading && useShimmer && cached == null) {
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
