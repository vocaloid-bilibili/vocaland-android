package xyz.lisfox.vocaland.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.ui.components.InlineLoading
import xyz.lisfox.vocaland.data.model.PlaylistInfoDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.PlaylistRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    repository: PlaylistRepository,
    songId: Int,
    loggedIn: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onRequireLogin: () -> Unit = {},
    onOpenMyPlaylists: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var playlists by remember { mutableStateOf<List<PlaylistInfoDto>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var workingId by remember { mutableStateOf<Int?>(null) }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(loggedIn, reloadKey) {
        if (!loggedIn) return@LaunchedEffect
        loading = true
        error = null
        repository.loadMyPlaylists(songId)
            .onSuccess { page ->
                playlists = page.items
                loading = false
            }
            .onFailure { failure ->
                error = failure.toUserMessage()
                loading = false
            }
    }

    fun togglePlaylist(item: PlaylistInfoDto) {
        if (workingId != null) return
        workingId = item.id
        error = null
        scope.launch {
            val result = if (item.containsSong) {
                repository.removeSong(item.id, songId)
            } else {
                repository.addSongs(item.id, listOf(songId)).map { }
            }
            result
                .onSuccess {
                    playlists = playlists.map { current ->
                        if (current.id == item.id) {
                            current.copy(containsSong = !item.containsSong)
                        } else {
                            current
                        }
                    }
                }
                .onFailure { failure -> error = failure.toUserMessage() }
            workingId = null
        }
    }

    fun createAndAdd() {
        val name = newName.trim()
        if (name.isEmpty() || creating) return
        creating = true
        error = null
        scope.launch {
            repository.createPlaylist(name)
                .onSuccess { created ->
                    repository.addSongs(created.id, listOf(songId))
                        .onSuccess {
                            newName = ""
                            reloadKey += 1
                        }
                        .onFailure { failure -> error = failure.toUserMessage() }
                }
                .onFailure { failure -> error = failure.toUserMessage() }
            creating = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "加入歌单",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭",
                    )
                }
            }

            if (!loggedIn) {
                Text(
                    text = "登录后即可把歌曲加入自己的歌单",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        onDismiss()
                        onRequireLogin()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("去登录")
                }
            } else {
                if (loading && playlists.isEmpty()) {
                    InlineLoading()
                }

                error?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(playlists, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = workingId == null) {
                                    togglePlaylist(item)
                                }
                                .padding(horizontal = 4.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${item.songCount} 首",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = if (item.containsSong) {
                                    Icons.Filled.Check
                                } else {
                                    Icons.Filled.Add
                                },
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (item.containsSong) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                    if (!loading && playlists.isEmpty()) {
                        item {
                            Text(
                                text = "还没有歌单，先在下方新建一个",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it.take(100) },
                        modifier = Modifier.weight(1f),
                        label = { Text("新建歌单") },
                        singleLine = true,
                    )
                    Button(
                        onClick = { createAndAdd() },
                        enabled = newName.isNotBlank() && !creating,
                    ) {
                        Text(if (creating) "创建中" else "创建")
                    }
                }

                onOpenMyPlaylists?.let { open ->
                    TextButton(
                        onClick = {
                            onDismiss()
                            open()
                        },
                    ) {
                        Text("查看我的歌单")
                    }
                }

                Spacer(Modifier.size(8.dp))
            }
        }
    }
}
