package xyz.lisfox.vocaland.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.util.artistLine
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.songDisplayName

fun LazyListScope.songCoverGrid(
    songs: List<SongDto>,
    keyPrefix: String,
    onOpenSong: (Int) -> Unit,
) {
    items(
        items = songs.chunked(2),
        key = { row -> "$keyPrefix-${row.joinToString("-") { it.id.toString() }}" },
    ) { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.forEach { song ->
                SongCoverCard(
                    title = songDisplayName(song),
                    subtitle = song.artistLine(),
                    thumbnail = song.coverUrl(),
                    onClick = { onOpenSong(song.id) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size == 1) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
