package xyz.lisfox.vocaland.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.lisfox.vocaland.data.model.PlaylistSortMode
import xyz.lisfox.vocaland.data.model.PlaylistSongDto
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.util.songDisplayName

@Composable
fun PlaylistMetaDialog(
    title: String,
    initialName: String,
    initialDescription: String?,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var description by remember(initialDescription) {
        mutableStateOf(initialDescription.orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(100) },
                    label = { Text("歌单名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(2000) },
                    label = { Text("简介（可选）") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) {
                        onConfirm(
                            trimmed,
                            description.trim().takeIf { it.isNotEmpty() },
                        )
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
fun PlaylistNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改歌单名称") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(100) },
                label = { Text("歌单名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
fun PlaylistDescriptionDialog(
    initialDescription: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var description by remember(initialDescription) {
        mutableStateOf(initialDescription.orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑歌单简介") },
        text = {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(2000) },
                label = { Text("简介") },
                minLines = 4,
                maxLines = 7,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(description.trim().takeIf { it.isNotEmpty() })
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
fun PlaylistNoteDialog(
    songTitle: String,
    initialNote: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var note by remember(initialNote) { mutableStateOf(initialNote.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("歌曲备注") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = songTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    label = { Text("备注（留空即清除）") },
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(note.trim().takeIf { it.isNotEmpty() })
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
fun PlaylistSortModeDialog(
    current: PlaylistSortMode,
    onDismiss: () -> Unit,
    onConfirm: (PlaylistSortMode) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("歌曲排序方式") },
        text = {
            Column {
                PlaylistSortMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(mode) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = current == mode,
                            onClick = { onConfirm(mode) },
                        )
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

private data class CoverChoice(
    val bvid: String?,
    val thumbnail: String?,
    val label: String,
)

@Composable
fun PlaylistCoverDialog(
    songs: List<PlaylistSongDto>,
    currentBvid: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var selected by remember(currentBvid) { mutableStateOf(currentBvid) }
    val choices = remember(songs) {
        buildList {
            add(CoverChoice(bvid = null, thumbnail = null, label = "无封面"))
            songs.forEach { item ->
                val song = item.song ?: return@forEach
                val video = song.videos.firstOrNull {
                    it.bvid.isNotBlank() && !it.disabled
                } ?: return@forEach
                add(
                    CoverChoice(
                        bvid = video.bvid,
                        thumbnail = video.thumbnail,
                        label = songDisplayName(song),
                    ),
                )
            }
        }.distinctBy { it.bvid }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置歌单封面") },
        text = {
            if (choices.size <= 1) {
                Text("歌单里还没有可用的视频封面")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.heightIn(max = 360.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(choices, key = { it.bvid ?: "none" }) { choice ->
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selected = choice.bvid },
                        ) {
                            NetworkImage(
                                url = choice.thumbnail,
                                contentDescription = choice.label,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp),
                            )
                            Text(
                                text = if (selected == choice.bvid) {
                                    "✓ ${choice.label}"
                                } else {
                                    choice.label
                                },
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected) },
                enabled = choices.size > 1 || currentBvid != null,
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
fun PlaylistDeleteDialog(
    playlistName: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除歌单") },
        text = {
            Text("确定删除歌单「$playlistName」？删除后无法恢复。")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !busy,
            ) {
                Text(if (busy) "删除中..." else "确认删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
