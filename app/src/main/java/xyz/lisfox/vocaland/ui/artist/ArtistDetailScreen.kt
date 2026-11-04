package xyz.lisfox.vocaland.ui.artist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.model.ArtistDetailDto
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.ArtistStatsSummaryDto
import xyz.lisfox.vocaland.data.repository.ArtistDetailRepository
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.MetricGrid
import xyz.lisfox.vocaland.ui.components.MetricItem
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.PagerRow
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.ui.components.songCoverGrid
import xyz.lisfox.vocaland.util.formatNumber

@Composable
fun ArtistDetailScreen(
    repository: ArtistDetailRepository,
    kind: ArtistKind,
    artistId: Int,
    onOpenSong: (Int) -> Unit,
    modifier: Modifier = Modifier,
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
                        )
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
            }
        }
    }
}

@Composable
private fun ArtistHero(
    artist: ArtistDetailDto,
    kind: ArtistKind,
    stats: ArtistStatsSummaryDto?,
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(kind.label)
                    if (artist.isVs) {
                        StatusPill("VS")
                    }
                    artist.abbreviation
                        ?.takeIf { it.isNotBlank() }
                        ?.let { StatusPill(it) }
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
