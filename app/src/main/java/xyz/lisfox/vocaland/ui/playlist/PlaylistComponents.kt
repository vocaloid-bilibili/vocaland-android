package xyz.lisfox.vocaland.ui.playlist

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.lisfox.vocaland.data.model.PlaylistCoverDto
import xyz.lisfox.vocaland.data.model.PublicPlaylistDto
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.util.formatNumber

@Composable
fun PlaylistPreviewSection(
    playlists: List<PublicPlaylistDto>,
    onOpenPlaylist: (Int) -> Unit,
    onOpenAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "最新歌单",
            action = {
                TextButton(onClick = onOpenAll) {
                    Text("歌单广场")
                }
            },
        )
        Spacer(Modifier.height(12.dp))

        if (playlists.isEmpty()) {
            EmptyHint("暂无公开歌单")
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistPreviewCard(
                        playlist = playlist,
                        onClick = { onOpenPlaylist(playlist.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistPreviewCard(
    playlist: PublicPlaylistDto,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(210.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column {
            PlaylistCover(
                cover = playlist.coverVideo,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = playlistMeta(playlist),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun PlaylistListCard(
    playlist: PublicPlaylistDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlaylistCover(
                cover = playlist.coverVideo,
                modifier = Modifier.size(88.dp),
                iconSize = 30.dp,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(88.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                playlist.description
                    ?.takeIf { it.isNotBlank() }
                    ?.let { description ->
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = playlistMeta(playlist),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun PlaylistCover(
    cover: PlaylistCoverDto?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 36.dp,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (cover?.thumbnail.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Filled.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            NetworkImage(
                url = cover?.thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun playlistMeta(playlist: PublicPlaylistDto): String {
    val songs = "${formatNumber(playlist.songCount.toLong())} 首"
    return if (playlist.clickCount > 0) {
        "$songs · ${formatNumber(playlist.clickCount.toLong())} 次浏览"
    } else {
        songs
    }
}
