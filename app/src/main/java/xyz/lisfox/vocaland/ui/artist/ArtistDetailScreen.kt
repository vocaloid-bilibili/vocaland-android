package xyz.lisfox.vocaland.ui.artist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import xyz.lisfox.vocaland.data.artist.RelatedArtist
import xyz.lisfox.vocaland.data.model.ArtistDetailDto
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.ArtistStatsSummaryDto
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.repository.ArtistDetailRepository
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.MetricGrid
import xyz.lisfox.vocaland.ui.components.MetricItem
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.PagerRow
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.ui.components.SongCoverCard
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.ui.components.songCoverGrid
import xyz.lisfox.vocaland.util.artistLine
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.formatNumber
import xyz.lisfox.vocaland.util.songDisplayName

@Composable
fun ArtistDetailScreen(
    repository: ArtistDetailRepository,
    kind: ArtistKind,
    artistId: Int,
    onOpenSong: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onOpenArtist: (ArtistKind, Int) -> Unit = { _, _ -> },
    onOpenTag: (Int) -> Unit = {},
    onShare: ((ArtistDetailDto) -> Unit)? = null,
    rating: (@Composable () -> Unit)? = null,
    comments: (@Composable () -> Unit)? = null,
) {
    val viewModel: ArtistDetailViewModel = viewModel(
        key = "artist-${kind.apiType}-$artistId",
        factory = viewModelFactory {
            initializer { ArtistDetailViewModel(repository, kind, artistId) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.loading && state.artist == null -> {
            LoadingPane(modifier = modifier)
        }

        state.artist == null -> {
            Box(modifier = modifier.padding(16.dp)) {
                ScreenMessage(
                    title = "加载失败",
                    message = state.error ?: "请稍后重试",
                    actionLabel = "重试",
                    onAction = viewModel::retry,
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    state.artist?.let { artist ->
                        ArtistHero(
                            artist = artist,
                            kind = kind,
                            stats = state.stats,
                            onShare = onShare?.let { share -> { share(artist) } },
                        )
                    }
                }

                if (state.extrasLoading) {
                    item {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                val extras = state.extras
                if (extras.topSongs.isNotEmpty()) {
                    item {
                        SongStrip(
                            title = "热门作品",
                            subtitle = "按播放数据",
                            songs = extras.topSongs,
                            onOpenSong = onOpenSong,
                        )
                    }
                }

                if (extras.latestSongs.isNotEmpty()) {
                    item {
                        SongStrip(
                            title = "最新作品",
                            subtitle = "按发布时间",
                            songs = extras.latestSongs,
                            onOpenSong = onOpenSong,
                        )
                    }
                }

                if (extras.related.isNotEmpty()) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SectionHeader(
                                title = "关联艺人",
                                subtitle = "合作过的对象",
                            )
                            RelatedArtistChips(
                                related = extras.related,
                                onOpenArtist = onOpenArtist,
                            )
                        }
                    }
                }

                if (extras.tags.isNotEmpty()) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SectionHeader(title = "标签")
                            TagChips(
                                tags = extras.tags,
                                onOpenTag = onOpenTag,
                            )
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = "作品",
                        subtitle = "共 ${state.total} 首",
                    )
                }

                if (state.songsLoading) {
                    item {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                if (state.error != null && state.songs.isNotEmpty()) {
                    item {
                        ScreenMessage(
                            title = "作品加载失败",
                            message = state.error ?: "请稍后重试",
                            actionLabel = "重试",
                            onAction = viewModel::retry,
                        )
                    }
                }

                if (state.songs.isEmpty() && !state.songsLoading) {
                    item {
                        EmptyHint("暂无作品")
                    }
                } else {
                    songCoverGrid(
                        songs = state.songs,
                        keyPrefix = "artist-${kind.apiType}-$artistId",
                        onOpenSong = onOpenSong,
                    )
                }

                if (state.totalPages > 1) {
                    item {
                        PagerRow(
                            page = state.page,
                            totalPages = state.totalPages,
                            total = state.total,
                            loading = state.songsLoading,
                            onPrevious = viewModel::previousPage,
                            onNext = viewModel::nextPage,
                        )
                    }
                }

                rating?.let { slot ->
                    item { slot() }
                }

                comments?.let { slot ->
                    item { slot() }
                }
            }
        }
    }
}

@Composable
private fun ArtistHero(
    artist: ArtistDetailDto,
    kind: ArtistKind,
    stats: ArtistStatsSummaryDto?,
    onShare: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NetworkImage(
                url = artist.picture,
                contentDescription = artist.name,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.headlineSmall,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(kind.label)
                    if (artist.isVs) {
                        StatusPill("VS")
                    }
                    artist.abbreviation
                        ?.takeIf { it.isNotBlank() }
                        ?.let { StatusPill(it) }
                }
            }

            if (onShare != null) {
                TextButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("分享")
                }
            }
        }

        if (stats != null) {
            StatsGrid(stats)
        }
    }
}

@Composable
private fun StatsGrid(stats: ArtistStatsSummaryDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        MetricGrid(
            items = listOf(
                MetricItem("播放", formatNumber(stats.view)),
                MetricItem("收藏", formatNumber(stats.favorite)),
                MetricItem("投币", formatNumber(stats.coin)),
                MetricItem("点赞", formatNumber(stats.like)),
                MetricItem("弹幕", formatNumber(stats.danmaku)),
                MetricItem("评论", formatNumber(stats.reply)),
                MetricItem("分享", formatNumber(stats.share)),
                MetricItem("作品", formatNumber(stats.songCount.toLong())),
            ),
            modifier = Modifier.padding(vertical = 12.dp),
            columns = 4,
        )
    }
}

@Composable
private fun SongStrip(
    title: String,
    subtitle: String,
    songs: List<SongDto>,
    onOpenSong: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = title, subtitle = subtitle)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = songs, key = { it.id }) { song ->
                SongCoverCard(
                    title = songDisplayName(song),
                    subtitle = song.artistLine(),
                    thumbnail = song.coverUrl(),
                    onClick = { onOpenSong(song.id) },
                    modifier = Modifier.width(220.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelatedArtistChips(
    related: List<RelatedArtist>,
    onOpenArtist: (ArtistKind, Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        related.forEach { artist ->
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenArtist(artist.kind, artist.id) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    NetworkImage(
                        url = artist.picture,
                        contentDescription = artist.name,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape),
                    )
                    Column {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${artist.kind.label} · ${artist.count}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChips(
    tags: List<TagItemDto>,
    onOpenTag: (Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenTag(tag.id) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (tag.associationCount > 0) {
                        Text(
                            text = tag.associationCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}
