package xyz.lisfox.vocaland.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.lisfox.vocaland.data.share.CollectionShareCardData
import xyz.lisfox.vocaland.data.share.SHARE_CARD_GRID_COLUMNS
import xyz.lisfox.vocaland.data.share.SHARE_CARD_HEIGHT_DP
import xyz.lisfox.vocaland.data.share.SHARE_CARD_SONG_COUNT
import xyz.lisfox.vocaland.data.share.SHARE_CARD_WIDTH_DP
import xyz.lisfox.vocaland.data.share.ShareCardSong
import xyz.lisfox.vocaland.ui.components.BrandMark
import xyz.lisfox.vocaland.ui.components.NetworkImage

private val SharePaper = Color(0xFFF7F3EA)
private val SharePaperDeep = Color(0xFFEDE7DA)
private val ShareInk = Color(0xFF22201C)
private val ShareInkSoft = Color(0xFF5E5A52)
private val ShareInkFaint = Color(0xFF8F897D)
private val ShareLine = Color(0xFFD8D1C2)
private val ShareBrand = Color(0xFFB25B4B)
private val ShareBrandDeep = Color(0xFF7E3B31)

/**
 * 收藏册分享卡：固定 540×960（9:16）竖版，导出时 2 倍放大到 1080×1920。
 *
 * 与网站版本保持同一信息层次：站点标识 → 册子 → 分享者 → 进度 → 条目缩略图 → 页脚。
 * 卡片使用固定浅色外观（不跟随深色主题），保证导出图与预览一致。
 */
@Composable
fun CollectionShareCard(
    data: CollectionShareCardData,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .requiredWidth(SHARE_CARD_WIDTH_DP.dp)
            .requiredHeight(SHARE_CARD_HEIGHT_DP.dp)
            .background(SharePaper)
            .padding(horizontal = 28.dp, vertical = 28.dp)
            .clipToBounds(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark(size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "VocaLand",
                    color = ShareInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "术力口数据库",
                    color = ShareInkFaint,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = data.watermark,
                color = ShareInkFaint,
                fontSize = 11.sp,
            )
        }

        Spacer(Modifier.height(26.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = data.title,
                color = ShareInk,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (data.subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = data.subtitle,
                    color = ShareInkSoft,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (data.ownerName != null) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NetworkImage(
                    url = data.ownerAvatar,
                    contentDescription = data.ownerName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = data.ownerName,
                        color = ShareInk,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Vocaland 收集册",
                        color = ShareInkFaint,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        if (data.stats.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                data.stats.take(3).forEach { stat ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(SharePaperDeep, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stat.value,
                            color = ShareInk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stat.label,
                            color = ShareInkFaint,
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        data.progress?.let { progress ->
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ShareBrand,
                trackColor = ShareLine,
            )
        }

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "收录条目",
                color = ShareInkFaint,
                fontSize = 11.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${data.songs.size} 项",
                color = ShareInkFaint,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds(),
        ) {
            EntryGrid(data = data)
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = data.footer,
            modifier = Modifier.fillMaxWidth(),
            color = ShareInkFaint,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EntryGrid(data: CollectionShareCardData) {
    val limit = if (data.squareThumbnails) 30 else SHARE_CARD_SONG_COUNT
    val items = data.songs.take(limit)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        items.chunked(SHARE_CARD_GRID_COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                row.forEach { song ->
                    EntryCell(
                        song = song,
                        square = data.squareThumbnails,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(SHARE_CARD_GRID_COLUMNS - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EntryCell(
    song: ShareCardSong,
    square: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(if (square) 1f else 16f / 9f)
            .background(SharePaperDeep)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        NetworkImage(
            url = song.thumbnail,
            contentDescription = song.title,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { saturate = if (song.lit) 1f else 0f },
        )
        song.badge
            ?.takeIf { it.isNotBlank() }
            ?.let { badge ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .background(ShareBrandDeep, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 8.sp,
                        maxLines = 1,
                    )
                }
            }
    }
}
