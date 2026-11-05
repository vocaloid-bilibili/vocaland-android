package xyz.lisfox.vocaland.ui.share

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import xyz.lisfox.vocaland.data.collection.CollectionEntityItem
import xyz.lisfox.vocaland.data.collection.CollectionEntry
import xyz.lisfox.vocaland.data.collection.CollectionHeader
import xyz.lisfox.vocaland.data.collection.CollectionRepository
import xyz.lisfox.vocaland.data.collection.CollectionTarget
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.share.CollectionShareCardData
import xyz.lisfox.vocaland.data.share.ShareCardSong
import xyz.lisfox.vocaland.data.share.ShareCardStat
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.util.artistLine
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.formatNumber
import xyz.lisfox.vocaland.util.songDisplayName

private data class ShareContent(
    val header: CollectionHeader,
    val entries: List<CollectionEntry>,
)

@Composable
fun CollectionShareScreen(
    repository: CollectionRepository,
    target: CollectionTarget,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var content by remember(target) { mutableStateOf<ShareContent?>(null) }
    var loading by remember(target) { mutableStateOf(true) }
    var error by remember(target) { mutableStateOf<String?>(null) }
    var reloadKey by remember(target) { mutableStateOf(0) }

    LaunchedEffect(target, reloadKey) {
        loading = true
        error = null
        repository.loadDetail(target = target, pageSize = 48)
            .onSuccess { result ->
                content = ShareContent(header = result.header, entries = result.entries)
                loading = false
            }
            .onFailure { failure ->
                error = failure.toUserMessage()
                loading = false
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            loading && content == null -> LoadingPane(text = "正在准备分享卡")

            content == null -> ScreenMessage(
                title = "分享卡加载失败",
                message = error ?: "请稍后重试",
                modifier = Modifier.padding(16.dp),
                actionLabel = "重试",
                onAction = { reloadKey += 1 },
            )

            else -> content?.let { current ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    ShareCardPreview(
                        data = current.toShareCardData(),
                        onSaved = {
                            Toast.makeText(context, "分享图已保存到相册", Toast.LENGTH_SHORT).show()
                        },
                        onError = { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }
    }
}

private fun ShareContent.toShareCardData(): CollectionShareCardData {
    val songs = entries.mapNotNull { entry ->
        when (val entity = entry.entity) {
            is CollectionEntityItem.Song -> ShareCardSong(
                title = songDisplayName(entity.song),
                subtitle = entity.song.artistLine(),
                thumbnail = entity.song.thumbnail?.takeIf { it.isNotBlank() }
                    ?: entity.song.coverUrl(),
                badge = entry.litCount.takeIf { it > 1 }?.let { "×$it" },
                lit = entry.litByMe,
            )

            is CollectionEntityItem.Artist -> ShareCardSong(
                title = entity.artist.name,
                subtitle = entity.kind.label,
                thumbnail = entity.artist.picture,
                lit = entry.litByMe,
            )

            null -> null
        }
    }

    return CollectionShareCardData(
        title = header.name,
        subtitle = header.description.orEmpty(),
        stats = listOf(
            ShareCardStat("条目", formatNumber(header.entityCount.toLong())),
            ShareCardStat("已点亮", formatNumber(header.litCount.toLong())),
            ShareCardStat("进度", "${(header.progress.coerceIn(0.0, 1.0) * 100).toInt()}%"),
        ),
        progress = header.progress.toFloat().coerceIn(0f, 1f),
        squareThumbnails = header.entityType != "song",
        songs = songs,
        footer = "VocaLand 收集册 · ${header.name}",
    )
}
