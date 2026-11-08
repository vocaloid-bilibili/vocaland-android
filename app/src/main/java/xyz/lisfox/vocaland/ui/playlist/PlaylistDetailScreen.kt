package xyz.lisfox.vocaland.ui.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.model.BgmTrack
import xyz.lisfox.vocaland.data.model.PlaylistDetailDto
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

@OptIn(ExperimentalFoundationApi::class)
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            var showManage by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PlaylistCover(
                        cover = playlist.coverVideo,
                        modifier = Modifier.size(96.dp),
                        iconSize = 32.dp,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (canEdit) {
                                IconButton(
                                    onClick = onEditName,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "修改名称",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
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
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        playlist.owner?.let { owner ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = owner.id > 0) { onOpenUser(owner.id) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                NetworkImage(
                                    url = ownerAvatarUrl(owner.avatar),
                                    fallbackIcon = Icons.Filled.Person,
                                    contentDescription = owner.nickname,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape),
                                )
                                Text(
                                    text = "${owner.nickname.ifBlank { owner.username }} · 歌单创建者",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                val description = decodeHtmlText(playlist.description)
                when {
                    !description.isNullOrBlank() -> {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    canEdit -> {
                        TextButton(
                            onClick = onEditDescription,
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text("添加简介")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { onPlayPlaylist(playableTracks) },
                        enabled = playableTracks.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("播放全部")
                    }
                    if (canEdit) {
                        OutlinedButton(
                            onClick = { showManage = true },
                            contentPadding = PaddingValues(horizontal = 14.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("管理")
                        }
                    }
                }

                if (!loggedIn) {
                    TextButton(
                        onClick = onLogin,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text("登录后可编辑歌单")
                    }
                }
            }

            if (showManage) {
                ManagePlaylistSheet(
                    canAddSongs = canAddSongs,
                    canDelete = canDelete,
                    mutating = mutating,
                    isPublic = visibility == PlaylistVisibility.Public,
                    isDefault = playlist.isDefault,
                    onDismiss = { showManage = false },
                    onAddSongs = { showManage = false; onRequestAddSongs() },
                    onToggleVisibility = { showManage = false; onToggleVisibility() },
                    onEditSortMode = { showManage = false; onEditSortMode() },
                    onEditCover = { showManage = false; onEditCover() },
                    onDelete = { showManage = false; onDelete() },
                )
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

private fun ownerAvatarUrl(avatar: String?): String? {
    val value = avatar?.trim().orEmpty()
    if (value.isEmpty()) return null
    return if (value.startsWith("http://") || value.startsWith("https://")) {
        value
    } else {
        "https://api.vocabili.top/v2/static/avatars/$value"
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ManagePlaylistSheet(
    canAddSongs: Boolean,
    canDelete: Boolean,
    mutating: Boolean,
    isPublic: Boolean,
    isDefault: Boolean,
    onDismiss: () -> Unit,
    onAddSongs: () -> Unit,
    onToggleVisibility: () -> Unit,
    onEditSortMode: () -> Unit,
    onEditCover: () -> Unit,
    onDelete: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "管理歌单",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            if (canAddSongs) {
                ManageActionRow(label = "添加歌曲", onClick = onAddSongs)
            }
            ManageActionRow(
                label = if (isPublic) "设为私有" else "设为公开",
                onClick = onToggleVisibility,
            )
            ManageActionRow(label = "排序方式", onClick = onEditSortMode)
            ManageActionRow(label = "设置封面", onClick = onEditCover)
            if (canDelete) {
                ManageActionRow(
                    label = "删除歌单",
                    onClick = onDelete,
                    enabled = !mutating,
                    isDestructive = true,
                )
            }
            if (isDefault) {
                Text(
                    text = "默认收藏夹由系统维护，不可删除",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ManageActionRow(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                isDestructive -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
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
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "${index + 1}",
            modifier = Modifier.width(18.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
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
                    .size(44.dp)
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

        if (canMove) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "上移",
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "下移",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        if (canEdit) {
            IconButton(
                onClick = onEditNote,
                modifier = Modifier.size(30.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "编辑备注",
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(30.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "移除歌曲",
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
