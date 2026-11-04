package xyz.lisfox.vocaland.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.model.BgmTrack
import xyz.lisfox.vocaland.data.model.PlaylistDetailDto
import xyz.lisfox.vocaland.data.model.PlaylistOwnerDto
import xyz.lisfox.vocaland.data.model.PlaylistSongDto
import xyz.lisfox.vocaland.data.player.playlistTracks
import xyz.lisfox.vocaland.data.repository.PlaylistRepository
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.util.artistLine
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.formatNumber
import xyz.lisfox.vocaland.util.songDisplayName

@Composable
fun PlaylistDetailScreen(
    repository: PlaylistRepository,
    playlistId: Int,
    onOpenSong: (Int) -> Unit,
    onOpenUser: (Int) -> Unit,
    onPlayPlaylist: (List<BgmTrack>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PlaylistDetailViewModel = viewModel(
        key = "playlist-$playlistId",
        factory = viewModelFactory {
            initializer { PlaylistDetailViewModel(repository, playlistId) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.loading && state.detail == null -> {
            LoadingPane(modifier = modifier)
        }

        state.detail == null -> {
            Box(modifier = modifier.padding(16.dp)) {
                ScreenMessage(
                    title = "歌单加载失败",
                    message = state.error ?: "歌单不存在",
                    actionLabel = "重试",
                    onAction = viewModel::load,
                )
            }
        }

        else -> {
            PlaylistDetailContent(
                detail = state.detail ?: return,
                onOpenSong = onOpenSong,
                onOpenUser = onOpenUser,
                onPlayPlaylist = onPlayPlaylist,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun PlaylistDetailContent(
    detail: PlaylistDetailDto,
    onOpenSong: (Int) -> Unit,
    onOpenUser: (Int) -> Unit,
    onPlayPlaylist: (List<BgmTrack>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playlist = detail.playlist
    val playableTracks = remember(detail) { playlistTracks(detail) }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PlaylistCover(
                cover = playlist.coverVideo,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                iconSize = 56.dp,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                playlist.description
                    ?.takeIf { it.isNotBlank() }
                    ?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                Text(
                    text = buildString {
                        append("${formatNumber(playlist.songCount.toLong())} 首")
                        if (playlist.clickCount > 0) {
                            append(" · ${formatNumber(playlist.clickCount.toLong())} 次浏览")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { onPlayPlaylist(playableTracks) },
                    enabled = playableTracks.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("播放该歌单")
                }
            }
        }

        playlist.owner?.let { owner ->
            item {
                OwnerRow(owner = owner, onOpenUser = onOpenUser)
            }
        }

        item {
            SectionHeader(
                title = "歌曲",
                subtitle = detail.songs.size.takeIf { it > 0 }?.let { "共 $it 首" },
            )
        }

        if (detail.songs.isEmpty()) {
            item {
                EmptyHint("歌单里还没有歌曲")
            }
        } else {
            items(detail.songs.size, key = { detail.songs[it].id }) { index ->
                PlaylistSongRow(
                    index = index + 1,
                    item = detail.songs[index],
                    onOpenSong = onOpenSong,
                )
            }
        }
    }
}

@Composable
private fun OwnerRow(
    owner: PlaylistOwnerDto,
    onOpenUser: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = owner.id > 0) { onOpenUser(owner.id) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NetworkImage(
                url = ownerAvatarUrl(owner.avatar),
                contentDescription = owner.nickname,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = owner.nickname.ifBlank { owner.username },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "歌单创建者",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun ownerAvatarUrl(avatar: String?): String? {
    val value = avatar?.trim().orEmpty()
    if (value.isEmpty()) return null
    return if (value.startsWith("http://") || value.startsWith("https://")) {
        value
    } else {
        "https://api.vocabili.top/v2/static/avatars/$value"
    }
}

@Composable
private fun PlaylistSongRow(
    index: Int,
    item: PlaylistSongDto,
    onOpenSong: (Int) -> Unit,
) {
    val song = item.song
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = song != null) {
                song?.let { onOpenSong(it.id) }
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = index.toString(),
            modifier = Modifier.width(28.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
        NetworkImage(
            url = song?.coverUrl(),
            contentDescription = song?.let { songDisplayName(it) },
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song?.let { songDisplayName(it) } ?: "歌曲已删除",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            song?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = it.artistLine(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            item.note
                ?.takeIf { it.isNotBlank() }
                ?.let { note ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
        }
    }
}
