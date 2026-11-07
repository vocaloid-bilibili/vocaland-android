package xyz.lisfox.vocaland.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.model.PlaylistInfoDto
import xyz.lisfox.vocaland.data.model.PlaylistVisibility
import xyz.lisfox.vocaland.data.repository.PlaylistRepository
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.InlineLoading
import xyz.lisfox.vocaland.ui.components.ScreenMessage

@Composable
fun MyPlaylistsScreen(
    repository: PlaylistRepository,
    loggedIn: Boolean,
    onLogin: () -> Unit,
    onOpenPlaylist: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!loggedIn) {
        Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            ScreenMessage(
                title = "需要登录",
                message = "登录后可以创建、整理自己的歌单",
                actionLabel = "去登录",
                onAction = onLogin,
            )
        }
        return
    }

    val viewModel: MyPlaylistsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { MyPlaylistsViewModel(repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<PlaylistInfoDto?>(null) }
    var deleteTarget by remember { mutableStateOf<PlaylistInfoDto?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (state.limit > 0) {
                    Text(
                        text = "${state.items.size} / ${state.limit} 个歌单",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = { showCreateDialog = true },
                enabled = !state.mutating &&
                    (state.limit <= 0 || state.items.size < state.limit),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text("新建")
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.loading && state.items.isEmpty() -> LoadingPane()

                state.error != null && state.items.isEmpty() -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        ScreenMessage(
                            title = "加载失败",
                            message = state.error ?: "请稍后重试",
                            actionLabel = "重试",
                            onAction = viewModel::load,
                        )
                    }
                }

                state.items.isEmpty() -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyHint("还没有歌单，点右上角新建一个")
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (state.loading || state.mutating) {
                            item {
                                InlineLoading()
                            }
                        }

                        state.notice?.let { message ->
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            start = 14.dp,
                                            top = 6.dp,
                                            bottom = 6.dp,
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = message,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                        )
                                        TextButton(onClick = viewModel::clearNotice) {
                                            Text("知道了")
                                        }
                                    }
                                }
                            }
                        }

                        items(state.items, key = { it.id }) { playlist ->
                            MyPlaylistCard(
                                playlist = playlist,
                                enabled = !state.mutating,
                                onOpen = { onOpenPlaylist(playlist.id) },
                                onEdit = { editTarget = playlist },
                                onDelete = { deleteTarget = playlist },
                            )
                        }

                        if (state.error != null) {
                            item {
                                ScreenMessage(
                                    title = "部分数据加载失败",
                                    message = state.error ?: "请稍后重试",
                                    actionLabel = "重试",
                                    onAction = viewModel::load,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        PlaylistMetaDialog(
            title = "新建歌单",
            initialName = "",
            initialDescription = null,
            confirmLabel = "创建",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description ->
                showCreateDialog = false
                viewModel.create(name, description)
            },
        )
    }

    editTarget?.let { target ->
        PlaylistMetaDialog(
            title = "编辑歌单",
            initialName = target.name,
            initialDescription = target.description,
            confirmLabel = "保存",
            onDismiss = { editTarget = null },
            onConfirm = { name, description ->
                editTarget = null
                viewModel.update(target.id, name, description)
            },
        )
    }

    deleteTarget?.let { target ->
        PlaylistDeleteDialog(
            playlistName = target.name,
            busy = state.mutating,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                viewModel.delete(target.id)
            },
        )
    }
}

@Composable
private fun MyPlaylistCard(
    playlist: PlaylistInfoDto,
    enabled: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val visibility = PlaylistVisibility.fromKey(playlist.visibility)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${playlist.songCount} 首 · ${visibility.label}" +
                        (if (playlist.isDefault) " · 默认收藏夹" else ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onEdit,
                enabled = enabled,
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "编辑",
                )
            }
            IconButton(
                onClick = onDelete,
                enabled = enabled && !playlist.isDefault,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                )
            }
        }
    }
}
