package xyz.lisfox.vocaland.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.lisfox.vocaland.data.danmaku.DanmakuRepository
import xyz.lisfox.vocaland.data.model.BgmTrack
import xyz.lisfox.vocaland.data.model.DanmakuItem
import xyz.lisfox.vocaland.data.player.BgmPlayerController
import xyz.lisfox.vocaland.data.player.PlayMode
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.MotionDurations
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import kotlin.math.roundToInt

private enum class PlayButtonState { Loading, Playing, Paused }

private const val DANMAKU_LIMIT_MIN = 50
private const val DANMAKU_LIMIT_MAX = 1000
private const val DANMAKU_LIMIT_STEP = 50
private const val DANMAKU_LIMIT_DEFAULT = 500
private const val DANMAKU_LIMIT_UNLIMITED = -1

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    controller: BgmPlayerController,
    danmakuRepository: DanmakuRepository,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsStateWithLifecycle()
    var editingQueue by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var danmakuEnabled by rememberSaveable { mutableStateOf(true) }
    var danmakuLimit by rememberSaveable { mutableIntStateOf(DANMAKU_LIMIT_DEFAULT) }
    val danmaku by produceState(
        initialValue = emptyList<DanmakuItem>(),
        key1 = state.cid,
        key2 = danmakuEnabled,
    ) {
        val cid = state.cid
        if (danmakuEnabled && cid != null) {
            val loaded = danmakuRepository.load(cid).getOrDefault(emptyList())
            if (cid == state.cid) {
                value = loaded
            }
        } else {
            value = emptyList()
        }
    }

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
            Box(modifier = modifier.fillMaxSize()) {
                if (danmakuEnabled) {
                    DanmakuBackground(
                        danmaku = danmaku,
                        cid = state.cid,
                        playing = state.playing,
                        positionMs = state.positionMs,
                        maxActive = danmakuLimit.takeIf { it > 0 },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
                            onOpenSettings = { showSettings = true },
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
                            action = {
                                TextButton(onClick = { editingQueue = !editingQueue }) {
                                    Text(if (editingQueue) "完成" else "编辑")
                                }
                            },
                        )
                    }

                    itemsIndexed(
                        items = state.tracks,
                        key = { _, track -> track.bvid },
                        contentType = { _, _ -> "track" },
                    ) { index, track ->
                        TrackRow(
                            modifier = Modifier.animateItem(),
                            index = index,
                            track = track,
                            selected = index == state.currentIndex,
                            editing = editingQueue,
                            canMoveUp = index > 0,
                            canMoveDown = index < state.tracks.lastIndex,
                            onClick = { controller.playAt(index) },
                            onMoveUp = { controller.moveTrack(index, index - 1) },
                            onMoveDown = { controller.moveTrack(index, index + 1) },
                            onRemove = { controller.removeTrack(index) },
                        )
                    }
                }
            }
            if (showSettings) {
                PlayerSettingsSheet(
                    controller = controller,
                    volume = state.volume,
                    pauseWhenFinished = state.pauseWhenFinished,
                    playMode = state.playMode,
                    danmakuEnabled = danmakuEnabled,
                    onDanmakuChange = { danmakuEnabled = it },
                    danmakuLimit = danmakuLimit,
                    onDanmakuLimitChange = { danmakuLimit = it },
                    onDismiss = { showSettings = false },
                )
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
    onOpenSettings: () -> Unit,
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
            val positionText = remember(positionMs) { formatDuration(positionMs) }
            val durationText = remember(durationMs) { formatDuration(durationMs) }
            Text(
                text = positionText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = durationText,
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
                Crossfade(
                    targetState = when {
                        loadingTrack -> PlayButtonState.Loading
                        playing -> PlayButtonState.Playing
                        else -> PlayButtonState.Paused
                    },
                    animationSpec = tween(MotionDurations.FAST),
                    label = "playPauseCrossfade",
                ) { buttonState ->
                    when (buttonState) {
                        PlayButtonState.Loading -> CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )

                        PlayButtonState.Playing -> Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "暂停",
                            modifier = Modifier.size(36.dp),
                        )

                        PlayButtonState.Paused -> Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "播放",
                            modifier = Modifier.size(36.dp),
                        )
                    }
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

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = {
                    controller.setPlayMode(
                        PlayMode.entries[(playMode.ordinal + 1) % PlayMode.entries.size],
                    )
                },
            ) {
                Icon(
                    imageVector = when (playMode) {
                        PlayMode.SINGLE -> Icons.Filled.RepeatOne
                        PlayMode.LIST -> Icons.Filled.Repeat
                        PlayMode.SHUFFLE -> Icons.Filled.Shuffle
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(playMode.label)
            }
            TextButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("设置")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSettingsSheet(
    controller: BgmPlayerController,
    volume: Float,
    pauseWhenFinished: Boolean,
    playMode: PlayMode,
    danmakuEnabled: Boolean,
    onDanmakuChange: (Boolean) -> Unit,
    danmakuLimit: Int,
    onDanmakuLimitChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val limitSliderValue = if (danmakuLimit == DANMAKU_LIMIT_UNLIMITED) {
        DANMAKU_LIMIT_MAX.toFloat()
    } else {
        danmakuLimit.toFloat()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "播放设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (volume == 0f) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = volume,
                    onValueChange = controller::setVolume,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${(volume * 100).roundToInt()}%",
                    modifier = Modifier.width(40.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }

            SettingsSwitchRow(
                title = "弹幕",
                subtitle = "在播放器背景显示弹幕",
                checked = danmakuEnabled,
                onCheckedChange = onDanmakuChange,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "显示数量",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (danmakuLimit == DANMAKU_LIMIT_UNLIMITED) {
                            "无限制"
                        } else {
                            "$danmakuLimit 条"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Slider(
                    value = limitSliderValue,
                    onValueChange = { raw ->
                        val count = raw.roundToInt()
                        onDanmakuLimitChange(
                            if (count >= DANMAKU_LIMIT_MAX) {
                                DANMAKU_LIMIT_UNLIMITED
                            } else {
                                count
                            },
                        )
                    },
                    valueRange = DANMAKU_LIMIT_MIN.toFloat()..DANMAKU_LIMIT_MAX.toFloat(),
                    steps = (DANMAKU_LIMIT_MAX - DANMAKU_LIMIT_MIN) / DANMAKU_LIMIT_STEP - 1,
                    enabled = danmakuEnabled,
                )
                Text(
                    text = "拉到最右为无限制",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SettingsSwitchRow(
                title = "播完暂停",
                subtitle = when (playMode) {
                    PlayMode.SINGLE -> "单曲播放结束后暂停"
                    PlayMode.LIST -> "列表最后一首播放结束后暂停"
                    PlayMode.SHUFFLE -> "一轮随机播放结束后暂停"
                },
                checked = pauseWhenFinished,
                onCheckedChange = controller::setPauseWhenFinished,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun TrackRow(
    index: Int,
    track: BgmTrack,
    selected: Boolean,
    editing: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) scheme.primaryContainer else scheme.surfaceContainerLow,
        animationSpec = tween(MotionDurations.FAST),
        label = "trackRowBackground",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .clickable(enabled = !editing, onClick = onClick)
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
        if (editing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "上移",
                        modifier = Modifier.size(18.dp),
                        tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "下移",
                        modifier = Modifier.size(18.dp),
                        tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "从队列移除",
                        modifier = Modifier.size(18.dp),
                        tint = scheme.error,
                    )
                }
            }
        } else {
            Text(
                text = formatDuration(track.durationSeconds * 1000L),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0) / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
