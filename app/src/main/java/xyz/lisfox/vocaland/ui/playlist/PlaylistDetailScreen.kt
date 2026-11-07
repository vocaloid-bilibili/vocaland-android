package xyz.lisfox.vocaland.ui.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.model.BgmTrack
import xyz.lisfox.vocaland.data.model.PlaylistDetailDto
import xyz.lisfox.vocaland.data.model.PlaylistOwnerDto
import xyz.lisfox.vocaland.data.model.PlaylistSortMode
import xyz.lisfox.vocaland.data.model.PlaylistSongDto
import xyz.lisfox.vocaland.data.model.PlaylistVisibility
import xyz.lisfox.vocaland.data.player.playlistTracks
import xyz.lisfox.vocaland.data.repository.PlaylistRepository
import xyz.lisfox.vocaland.data.repository.SearchRepository
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.util.artistLine
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.decodeHtmlText
import xyz.lisfox.vocaland.util.formatNumber
import xyz.lisfox.vocaland.util.songDisplayName

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaylistDetailScreen(
    repository: PlaylistRepository,
    playlistId: Int,
    onOpenSong: (Int) -> Unit,
    onOpenUser: (Int) -> Unit,
    onPlayPlaylist: (List<BgmTrack>) -> Unit,
    modifier: Modifier = Modifier,
    searchRepository: SearchRepository? = null,
    session: AuthSession? = null,
    onLogin: () -> Unit = {},
    onPlaylistDeleted: () -> Unit = {},
    communitySlot: (@Composable () -> Unit)? = null,
) {
    val viewModel: PlaylistDetailViewModel = viewModel(
        key = "playlist-$playlistId",
        factory = viewModelFactory {
            initializer { PlaylistDetailViewModel(repository, playlistId) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val viewer = if (session != null) {
        session.state.collectAsStateWithLifecycle().value.user
    } else {
        null
    }

    var showNameDialog by remember { mutableStateOf(false) }
    var showDescriptionDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showCoverDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddSongs by remember { mutableStateOf(false) }
    var noteTarget by remember { mutableStateOf<PlaylistSongDto?>(null) }

    val playlist = state.detail?.playlist
    val canEdit = playlist?.canEdit == true
    val canDelete = canEdit &&
        viewer != null &&
        playlist?.owner?.id == viewer.id &&
        playlist?.isDefault != true

    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            viewModel.consumeDeleted()
            onPlaylistDeleted()
        }
    }

    when {
        state.loading && state.detail == null -> {
            LoadingPane(modifier = modifier)
        }

        state.detail == null -> {
            Box(modifier = modifier.padding(16.dp)) {
                ScreenMessage(
                    title = "歌单加载失败",
                    message = state.error ?: "歌单可能不存在或无权访问",
                    actionLabel = "重试",
                    onAction = viewModel::load,
                )
            }
        }

        else -> {
            PlaylistDetailContent(
                detail = state.detail ?: return,
                canEdit = canEdit,
                canDelete = canDelete,
                mutating = state.mutating,
                notice = state.notice,
                onDismissNotice = viewModel::clearNotice,
                onOpenSong = onOpenSong,
                onOpenUser = onOpenUser,
                onPlayPlaylist = onPlayPlaylist,
                communitySlot = communitySlot,
                onLogin = onLogin,
                loggedIn = viewer != null,
                canAddSongs = canEdit && searchRepository != null,
                onRequestAddSongs = { showAddSongs = true },
                onEditName = { showNameDialog = true },
                onEditDescription = { showDescriptionDialog = true },
                onEditSortMode = { showSortDialog = true },
                onEditCover = { showCoverDialog = true },
                onToggleVisibility = viewModel::toggleVisibility,
                onMoveUp = viewModel::moveUp,
                onMoveDown = viewModel::moveDown,
                onRemoveSong = viewModel::removeSong,
                onEditNote = { noteTarget = it },
                onDelete = { showDeleteDialog = true },
                modifier = modifier,
            )
        }
    }

    if (showNameDialog && playlist != null) {
        PlaylistNameDialog(
            initialName = playlist.name,
            onDismiss = { showNameDialog = false },
            onConfirm = {
                showNameDialog = false
                viewModel.updateName(it)
            },
        )
    }

    if (showDescriptionDialog && playlist != null) {
        PlaylistDescriptionDialog(
            initialDescription = decodeHtmlText(playlist.description).orEmpty(),
            onDismiss = { showDescriptionDialog = false },
            onConfirm = {
                showDescriptionDialog = false
                viewModel.updateDescription(it)
            },
        )
    }

    if (showSortDialog) {
        PlaylistSortModeDialog(
            current = PlaylistSortMode.fromKey(state.sortMode),
            onDismiss = { showSortDialog = false },
            onConfirm = { mode ->
                showSortDialog = false
                viewModel.changeSortMode(mode)
            },
        )
    }

    if (showCoverDialog && state.detail != null) {
        PlaylistCoverDialog(
            songs = state.detail?.songs.orEmpty(),
            currentBvid = playlist?.coverVideo?.bvid,
            onDismiss = { showCoverDialog = false },
            onConfirm = { bvid ->
                showCoverDialog = false
                viewModel.setCover(bvid)
            },
        )
    }

    if (showDeleteDialog && playlist != null) {
        PlaylistDeleteDialog(
            playlistName = playlist.name,
            busy = state.mutating,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deletePlaylist()
            },
        )
    }

    noteTarget?.let { target ->
        PlaylistNoteDialog(
            songTitle = target.song?.let { songDisplayName(it) } ?: "歌曲 #${target.songId}",
            initialNote = target.note,
            onDismiss = { noteTarget = null },
            onConfirm = { note ->
                noteTarget = null
                viewModel.saveNote(target.songId, note)
            },
        )
    }

    if (showAddSongs && searchRepository != null) {
        AddSongsToPlaylistSheet(
            repository = repository,
            searchRepository = searchRepository,
            playlistId = playlistId,
            onDismiss = { showAddSongs = false },
            onFinished = viewModel::load,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun PlaylistDetailContent(
    detail: PlaylistDetailDto,
    canEdit: Boolean,
    canDelete: Boolean,
    mutating: Boolean,
    notice: String?,
    onDismissNotice: () -> Unit,
    onOpenSong: (Int) -> Unit,
    onOpenUser: (Int) -> Unit,
    onPlayPlaylist: (List<BgmTrack>) -> Unit,
    communitySlot: (@Composable () -> Unit)?,
    onLogin: () -> Unit,
    loggedIn: Boolean,
    canAddSongs: Boolean,
    onRequestAddSongs: () -> Unit,
    onEditName: () -> Unit,
    onEditDescription: () -> Unit,
    onEditSortMode: () -> Unit,
    onEditCover: () -> Unit,
    onToggleVisibility: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemoveSong: (Int) -> Unit,
    onEditNote: (PlaylistSongDto) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playlist = detail.playlist
    val playableTracks = remember(detail) { playlistTracks(detail) }
    val visibility = PlaylistVisibility.fromKey(playlist.visibility)
    val sortMode = PlaylistSortMode.fromKey(playlist.sortMode)

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    if (canEdit) {
                        IconButton(onClick = onEditName) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "修改名称",
                            )
                        }
                    }
                }

                if (decodeHtmlText(playlist.description).isNullOrBlank()) {
                    if (canEdit) {
                        TextButton(onClick = onEditDescription) {
                            Text("添加简介")
                        }
                    }
                } else {
                    Text(
                        text = decodeHtmlText(playlist.description).orEmpty(),
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
                        append(" · ${visibility.label}")
                        append(" · ${sortMode.label}")
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
                    Text("播放全部")
                }

                if (canEdit) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canAddSongs) {
                            TextButton(onClick = onRequestAddSongs) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text("添加歌曲")
                            }
                        }
                        TextButton(onClick = onToggleVisibility) {
                            Text(
                                if (visibility == PlaylistVisibility.Public) {
                                    "设为私有"
                                } else {
                                    "设为公开"
                                },
                            )
                        }
                        TextButton(onClick = onEditSortMode) {
                            Text("排序方式")
                        }
                        TextButton(onClick = onEditCover) {
                            Text("设置封面")
                        }
                        if (canDelete) {
                            TextButton(
                                onClick = onDelete,
                                enabled = !mutating,
                            ) {
                                Text("删除歌单")
                            }
                        }
                    }
                    if (playlist.isDefault) {
                        Text(
                            text = "默认收藏夹由系统维护，不可删除",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (!loggedIn) {
                    TextButton(onClick = onLogin) {
                        Text("登录后可编辑歌单")
                    }
                }
            }
        }

        playlist.owner?.let { owner ->
            item {
                OwnerRow(owner = owner, onOpenUser = onOpenUser)
            }
        }

        notice?.let { message ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onDismissNotice) {
                            Text("知道了")
                        }
                    }
                }
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
            itemsIndexed(
                items = detail.songs,
                key = { _, item -> item.id },
                contentType = { _, _ -> "playlist-song" },
            ) { index, item ->
                PlaylistSongRow(
                    modifier = Modifier.animateItem(),
                    index = index,
                    item = item,
                    canEdit = canEdit,
                    canMove = canEdit && sortMode == PlaylistSortMode.Custom,
                    onOpenSong = onOpenSong,
                    onMoveUp = { onMoveUp(index) },
                    onMoveDown = { onMoveDown(index) },
                    onRemove = { onRemoveSong(item.songId) },
                    onEditNote = { onEditNote(item) },
                )
            }
        }

        communitySlot?.let { slot ->
            item { slot() }
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
                    fallbackIcon = Icons.Filled.Person,
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
    canEdit: Boolean,
    canMove: Boolean,
    onOpenSong: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onEditNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song = item.song
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.width(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (canMove) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "上移",
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "下移",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = song != null) {
                    song?.let { onOpenSong(it.id) }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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

        if (canEdit) {
            IconButton(
                onClick = onEditNote,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "编辑备注",
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "移除歌曲",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
