package xyz.lisfox.vocaland.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.lisfox.vocaland.data.model.BgmTrack
import xyz.lisfox.vocaland.data.player.BgmPlayerController
import xyz.lisfox.vocaland.data.player.PlayMode
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader

@Composable
fun PlayerScreen(
    controller: BgmPlayerController,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        controller.ensureLoaded()
    }

    when {
        state.loadingPlaylist && state.tracks.isEmpty() -> {
            LoadingPane(modifier = modifier)
        }

        state.tracks.isEmpty() -> {
            Box(modifier = modifier.padding(16.dp)) {
                ScreenMessage(
                    title = "播放列表为空",
                    message = state.error ?: "暂时没有可播放的曲目",
                    actionLabel = "重试",
                    onAction = controller::retry,
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    PlayerHero(track = state.currentTrack)
                }

                item {
                    PlayerControls(
                        controller = controller,
                        loadingTrack = state.loadingTrack,
                        playing = state.playing,
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        hasTrack = state.currentTrack != null,
                        playMode = state.playMode,
                    )
                }

                state.error
                    ?.takeIf { it.isNotBlank() }
                    ?.let { error ->
                        item {
                            Text(
                                text = error,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                item {
                    SectionHeader(
                        title = "播放列表",
                        subtitle = "共 ${state.tracks.size} 首",
                    )
                }

                itemsIndexed(
                    items = state.tracks,
                    key = { _, track -> track.bvid },
                ) { index, track ->
                    TrackRow(
                        index = index,
                        track = track,
                        selected = index == state.currentIndex,
                        onClick = { controller.playAt(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerHero(track: BgmTrack?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (track?.cover.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                NetworkImage(
                    url = track?.cover,
                    contentDescription = track?.title,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = track?.title ?: "未选择曲目",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = track?.artist.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlayerControls(
    controller: BgmPlayerController,
    loadingTrack: Boolean,
    playing: Boolean,
    positionMs: Long,
    durationMs: Long,
    hasTrack: Boolean,
    playMode: PlayMode,
) {
    val duration = durationMs.coerceAtLeast(1L)
    var dragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(positionMs, dragging, duration) {
        if (!dragging) {
            dragPosition = positionMs.toFloat().coerceIn(0f, duration.toFloat())
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Slider(
            value = if (dragging) dragPosition else positionMs.toFloat().coerceIn(0f, duration.toFloat()),
            onValueChange = {
                dragging = true
                dragPosition = it
            },
            onValueChangeFinished = {
                controller.seekTo(dragPosition.toLong())
                dragging = false
            },
            valueRange = 0f..duration.toFloat(),
            enabled = hasTrack && durationMs > 0,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = controller::previous,
                enabled = hasTrack && !loadingTrack,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "上一首",
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.width(20.dp))
            FilledIconButton(
                onClick = controller::toggle,
                modifier = Modifier.size(66.dp),
                enabled = hasTrack && !loadingTrack,
            ) {
                if (loadingTrack) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "暂停" else "播放",
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            IconButton(
                onClick = controller::next,
                enabled = hasTrack && !loadingTrack,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "下一首",
                    modifier = Modifier.size(34.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                8.dp,
                Alignment.CenterHorizontally,
            ),
        ) {
            PlayMode.entries.forEach { mode ->
                FilterChip(
                    selected = playMode == mode,
                    onClick = { controller.setPlayMode(mode) },
                    label = { Text(mode.label) },
                    border = null,
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    index: Int,
    track: BgmTrack,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) scheme.primaryContainer else scheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = (index + 1).toString(),
            modifier = Modifier.width(28.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
        NetworkImage(
            url = track.cover,
            contentDescription = track.title,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(9.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    scheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    scheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatDuration(track.durationSeconds * 1000L),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0) / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
