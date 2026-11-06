package xyz.lisfox.vocaland.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.lisfox.vocaland.data.model.SongBriefDto
import xyz.lisfox.vocaland.data.model.SongListItemDto
import xyz.lisfox.vocaland.data.model.TOP_STATS
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.ui.components.SongCoverCard
import xyz.lisfox.vocaland.util.artistLine
import xyz.lisfox.vocaland.util.formatNumber
import xyz.lisfox.vocaland.util.songDisplayName

@Composable
fun TopSongsSection(
    selectedStat: String,
    items: List<SongListItemDto>,
    loading: Boolean,
    error: String?,
    onSelectStat: (String) -> Unit,
    onOpenSong: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "数据总榜", subtitle = "按全站累计数据排序")
        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TOP_STATS, key = { it.key }) { stat ->
                FilterChip(
                    selected = stat.key == selectedStat,
                    onClick = { onSelectStat(stat.key) },
                    label = { Text(stat.label) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            loading -> LoadingRow()
            !error.isNullOrBlank() -> EmptyHint(error)
            items.isEmpty() -> EmptyHint("暂无数据")
            else -> {
                val unit = TOP_STATS.firstOrNull { it.key == selectedStat }?.label ?: ""
                RankCard {
                    items.forEachIndexed { index, item ->
                        SongRankRow(
                            rank = index + 1,
                            title = songDisplayName(item.song),
                            artist = item.song.artistLine(),
                            thumbnail = item.video?.thumbnail,
                            value = statValue(item, selectedStat),
                            unit = unit,
                            onClick = { onOpenSong(item.song.id) },
                        )
                        if (index != items.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnThisDaySection(
    month: Int,
    day: Int,
    items: List<SongListItemDto>,
    onOpenSong: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "历史上的今天", subtitle = "${month}月${day}日")
        Spacer(Modifier.height(10.dp))

        if (items.isEmpty()) {
            EmptyHint("今天还没有收录记录")
        } else {
            RankCard {
                items.take(10).forEachIndexed { index, item ->
                    SongRankRow(
                        rank = index + 1,
                        title = songDisplayName(item.song),
                        artist = item.song.artistLine(),
                        thumbnail = item.video?.thumbnail,
                        value = item.snapshot?.view,
                        unit = "播放",
                        onClick = { onOpenSong(item.song.id) },
                    )
                    if (index != items.take(10).lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LatestSongsSection(
    songs: List<SongBriefDto>,
    onOpenSong: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "最新入库")
        Spacer(Modifier.height(12.dp))

        if (songs.isEmpty()) {
            EmptyHint("暂时还没有新收录的歌曲")
        } else {
            songs.chunked(2).forEach { rowSongs ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowSongs.forEach { song ->
                        LatestSongCard(
                            song = song,
                            onClick = { onOpenSong(song.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowSongs.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LatestSongCard(
    song: SongBriefDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SongCoverCard(
        title = songDisplayName(song),
        subtitle = song.artistLine(),
        thumbnail = song.thumbnail,
        onClick = onClick,
        modifier = modifier,
        sharedKey = "song-cover-${song.id}",
    )
}

@Composable
private fun RankCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            content()
        }
    }
}

@Composable
private fun SongRankRow(
    rank: Int,
    title: String,
    artist: String,
    thumbnail: String?,
    value: Long?,
    unit: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            modifier = Modifier.width(24.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NetworkImage(
            url = thumbnail,
            contentDescription = title,
            modifier = Modifier
                .width(72.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(9.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatNumber(value),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(26.dp))
    }
}

private fun statValue(item: SongListItemDto, stat: String): Long? {
    val snapshot = item.snapshot ?: return null
    return when (stat) {
        "view" -> snapshot.view
        "favorite" -> snapshot.favorite
        "coin" -> snapshot.coin
        "like" -> snapshot.like
        "danmaku" -> snapshot.danmaku
        "reply" -> snapshot.reply
        "share" -> snapshot.share
        else -> null
    }
}
