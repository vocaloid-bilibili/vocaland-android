package xyz.lisfox.vocaland.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.lisfox.vocaland.data.collection.CollectionEntityItem
import xyz.lisfox.vocaland.data.collection.CollectionEntry
import xyz.lisfox.vocaland.data.collection.CollectionHeader
import xyz.lisfox.vocaland.data.collection.LitEntityType
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.util.artistLine
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.formatNumber
import xyz.lisfox.vocaland.util.songDisplayName

@Composable
internal fun CollectionCover(
    coverPicture: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (coverPicture.isNullOrBlank()) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            NetworkImage(
                url = coverPicture,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun CollectionProgress(
    header: CollectionHeader,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "已点亮 ${formatNumber(header.litCount.toLong())} / " +
                    formatNumber(header.entityCount.toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${(header.progress.coerceIn(0.0, 1.0) * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LinearProgressIndicator(
            progress = header.progress.toFloat().coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun CollectionEntryTile(
    entry: CollectionEntry,
    entityType: String,
    onOpenSong: (Int) -> Unit,
    onOpenArtist: (ArtistKind, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entity = entry.entity
    val onClick: () -> Unit = when (entity) {
        is CollectionEntityItem.Song -> {
            val songId = entity.song.id
            { onOpenSong(songId) }
        }

        is CollectionEntityItem.Artist -> {
            val artistKind = entity.kind
            val artistId = entity.artist.id
            { onOpenArtist(artistKind, artistId) }
        }

        null -> {
            val fallbackKind = artistKindOf(entityType)
            if (fallbackKind == null) {
                { onOpenSong(entry.entityId) }
            } else {
                val resolvedKind = fallbackKind
                { onOpenArtist(resolvedKind, entry.entityId) }
            }
        }
    }

    val title = when (entity) {
        is CollectionEntityItem.Song -> songDisplayName(entity.song)
        is CollectionEntityItem.Artist -> entity.artist.name
        null -> "内容已删除"
    }
    val subtitle = when (entity) {
        is CollectionEntityItem.Song -> entity.song.artistLine()
        is CollectionEntityItem.Artist -> entity.kind.label
        null -> entityTypeLabel(entityType)
    }
    val picture = when (entity) {
        is CollectionEntityItem.Song -> entity.song.coverUrl()
        is CollectionEntityItem.Artist -> entity.artist.picture
        null -> null
    }

    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box {
            NetworkImage(
                url = picture,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp)),
            )
            if (entry.litByMe) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text = "已点亮",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${formatNumber(entry.litCount.toLong())} 人点亮",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun artistKindOf(entityType: String): ArtistKind? = when (entityType) {
    "producer" -> ArtistKind.Producer
    "vocalist" -> ArtistKind.Vocalist
    else -> null
}

internal fun entityTypeLabel(entityType: String): String =
    LitEntityType.fromKey(entityType).label
