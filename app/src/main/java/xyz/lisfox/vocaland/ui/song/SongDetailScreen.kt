package xyz.lisfox.vocaland.ui.song

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.CommentDto
import xyz.lisfox.vocaland.data.model.RankingBoard
import xyz.lisfox.vocaland.data.model.RankingDto
import xyz.lisfox.vocaland.data.model.RatingDto
import xyz.lisfox.vocaland.data.model.RatingSummaryDto
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.SongRankingStatsDto
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.model.VideoDto
import xyz.lisfox.vocaland.data.repository.SongDetailRepository
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.MetricGrid
import xyz.lisfox.vocaland.ui.components.MetricItem
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.PagerRow
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.formatNumber
import xyz.lisfox.vocaland.util.formatShortDate
import xyz.lisfox.vocaland.util.openBilibiliUser
import xyz.lisfox.vocaland.util.openBilibiliVideo
import xyz.lisfox.vocaland.util.songDisplayName
import xyz.lisfox.vocaland.util.timeAgo
import java.util.Locale
import kotlin.math.roundToInt

private val songRankingBoards = listOf(
    RankingBoard.Daily to "日榜",
    RankingBoard.Weekly to "周榜",
    RankingBoard.Monthly to "月榜",
)

@Composable
fun SongDetailScreen(
    repository: SongDetailRepository,
    session: AuthSession,
    songId: Int,
    onOpenArtist: (ArtistKind, Int) -> Unit,
    onOpenTag: (Int) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SongDetailViewModel = viewModel(
        key = "song-$songId",
        factory = viewModelFactory {
            initializer { SongDetailViewModel(repository, session, songId) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.loading && state.song == null -> {
            LoadingPane(modifier = modifier)
        }

        state.song == null -> {
            Box(modifier = modifier.padding(16.dp)) {
                ScreenMessage(
                    title = "歌曲加载失败",
                    message = state.error ?: "请稍后重试",
                    actionLabel = "重试",
                    onAction = viewModel::retrySong,
                )
            }
        }

        else -> {
            state.song?.let { song ->
                SongDetailContent(
                    state = state,
                    song = song,
                    onOpenArtist = onOpenArtist,
                    onOpenTag = onOpenTag,
                    onLogin = onLogin,
                    onSelectRankingBoard = viewModel::selectRankingBoard,
                    onRetryRanking = viewModel::retryRanking,
                    onRetryRatings = viewModel::retryRatings,
                    onPreviousRatings = viewModel::previousRatingsPage,
                    onNextRatings = viewModel::nextRatingsPage,
                    onRetryComments = viewModel::retryComments,
                    onPreviousComments = viewModel::previousCommentsPage,
                    onNextComments = viewModel::nextCommentsPage,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun SongDetailContent(
    state: SongDetailUiState,
    song: SongDto,
    onOpenArtist: (ArtistKind, Int) -> Unit,
    onOpenTag: (Int) -> Unit,
    onLogin: () -> Unit,
    onSelectRankingBoard: (RankingBoard) -> Unit,
    onRetryRanking: () -> Unit,
    onRetryRatings: () -> Unit,
    onPreviousRatings: () -> Unit,
    onNextRatings: () -> Unit,
    onRetryComments: () -> Unit,
    onPreviousComments: () -> Unit,
    onNextComments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val producers = song.producers
        .map { it.producer }
        .filter { it.id > 0 }
        .distinctBy { it.id }
        .map { ArtistChip(it.id, it.name, ArtistKind.Producer) }
    val vocalists = song.vocalists
        .map { it.vocalist }
        .filter { it.id > 0 }
        .distinctBy { it.id }
        .map { ArtistChip(it.id, it.name, ArtistKind.Vocalist) }
    val synthesizers = song.vocalists
        .mapNotNull { it.synthesizer }
        .filter { it.id > 0 }
        .distinctBy { it.id }
        .map { ArtistChip(it.id, it.name, ArtistKind.Synthesizer) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SongHero(song)
        }

        if (producers.isNotEmpty()) {
            item {
                ArtistChipRow(
                    title = "P主",
                    chips = producers,
                    onOpenArtist = onOpenArtist,
                )
            }
        }

        if (vocalists.isNotEmpty()) {
            item {
                ArtistChipRow(
                    title = "歌手",
                    chips = vocalists,
                    onOpenArtist = onOpenArtist,
                )
            }
        }

        if (synthesizers.isNotEmpty()) {
            item {
                ArtistChipRow(
                    title = "合成器",
                    chips = synthesizers,
                    onOpenArtist = onOpenArtist,
                )
            }
        }

        if (state.tags.isNotEmpty()) {
            item {
                TagChipRow(
                    tags = state.tags,
                    onOpenTag = onOpenTag,
                )
            }
        }

        item {
            SongDataSection(
                videoCount = song.videos.size,
                totals = song.snapshotTotals(),
            )
        }

        item {
            SectionHeader(
                title = "视频",
                subtitle = if (song.videos.isEmpty()) null else "共 ${song.videos.size} 个投稿",
            )
        }

        if (song.videos.isEmpty()) {
            item {
                EmptyHint("暂无视频")
            }
        } else {
            itemsIndexed(
                items = song.videos,
                key = { index, video -> "${video.bvid}-$index" },
            ) { _, video ->
                VideoCard(video)
            }
        }

        item {
            SongRankingSection(
                state = state,
                onSelectBoard = onSelectRankingBoard,
                onRetry = onRetryRanking,
                onLogin = onLogin,
            )
        }

        item {
            SongRatingsSection(
                state = state,
                onRetry = onRetryRatings,
                onPrevious = onPreviousRatings,
                onNext = onNextRatings,
            )
        }

        item {
            SongCommentsSection(
                state = state,
                onLogin = onLogin,
                onRetry = onRetryComments,
                onPrevious = onPreviousComments,
                onNext = onNextComments,
            )
        }
    }
}

@Composable
private fun SongHero(song: SongDto) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NetworkImage(
            url = song.coverUrl(),
            contentDescription = songDisplayName(song),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(24.dp)),
        )

        Text(
            text = songDisplayName(song),
            style = MaterialTheme.typography.headlineSmall,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(song.type?.takeIf { it.isNotBlank() } ?: "歌曲")
            if (song.isForeign) {
                StatusPill("外语")
            }
            if (!song.collected) {
                StatusPill("未收录")
            }
        }
    }
}

private data class ArtistChip(
    val id: Int,
    val name: String,
    val kind: ArtistKind,
)

@Composable
private fun ArtistChipRow(
    title: String,
    chips: List<ArtistChip>,
    onOpenArtist: (ArtistKind, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chips, key = { "${it.kind.apiType}-${it.id}" }) { chip ->
                AssistChip(
                    onClick = { onOpenArtist(chip.kind, chip.id) },
                    label = {
                        Text(
                            text = chip.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChipRow(
    tags: List<TagItemDto>,
    onOpenTag: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "标签",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tags.forEach { tag ->
                AssistChip(
                    onClick = { onOpenTag(tag.id) },
                    label = {
                        Text(
                            text = tag.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
    }
}

private data class SnapshotTotals(
    val view: Long,
    val favorite: Long,
    val coin: Long,
    val like: Long,
    val danmaku: Long,
    val reply: Long,
    val share: Long,
    val snapshotCount: Int,
)

private fun SongDto.snapshotTotals(): SnapshotTotals? {
    val snapshots = videos.mapNotNull { it.latestSnapshot }
    if (snapshots.isEmpty()) return null

    return SnapshotTotals(
        view = snapshots.sumOf { it.view },
        favorite = snapshots.sumOf { it.favorite },
        coin = snapshots.sumOf { it.coin },
        like = snapshots.sumOf { it.like },
        danmaku = snapshots.sumOf { it.danmaku },
        reply = snapshots.sumOf { it.reply },
        share = snapshots.sumOf { it.share },
        snapshotCount = snapshots.size,
    )
}

@Composable
private fun SongDataSection(
    videoCount: Int,
    totals: SnapshotTotals?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "数据",
            subtitle = totals?.let { "按最新快照汇总 · ${it.snapshotCount} 个投稿" },
        )

        if (totals == null) {
            EmptyHint("暂无数据")
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                MetricGrid(
                    items = listOf(
                        MetricItem("播放", formatNumber(totals.view)),
                        MetricItem("收藏", formatNumber(totals.favorite)),
                        MetricItem("硬币", formatNumber(totals.coin)),
                        MetricItem("点赞", formatNumber(totals.like)),
                        MetricItem("弹幕", formatNumber(totals.danmaku)),
                        MetricItem("评论", formatNumber(totals.reply)),
                        MetricItem("分享", formatNumber(totals.share)),
                        MetricItem("投稿", videoCount.toString()),
                    ),
                    modifier = Modifier.padding(vertical = 12.dp),
                    columns = 4,
                )
            }
        }
    }
}

@Composable
private fun SongRankingSection(
    state: SongDetailUiState,
    onSelectBoard: (RankingBoard) -> Unit,
    onRetry: () -> Unit,
    onLogin: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "排行数据")

        if (!state.isLoggedIn) {
            LoginHintCard(
                message = "登录后查看这首歌的上榜历史与统计",
                onLogin = onLogin,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(songRankingBoards, key = { it.first.key }) { (board, label) ->
                    FilterChip(
                        selected = state.rankingBoard == board,
                        onClick = { onSelectBoard(board) },
                        label = { Text(label) },
                    )
                }
            }

            if (state.rankingLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                state.rankingError != null -> {
                    ScreenMessage(
                        title = "排行数据加载失败",
                        message = state.rankingError ?: "请稍后重试",
                        actionLabel = "重试",
                        onAction = onRetry,
                    )
                }

                state.rankingLoaded &&
                    state.rankingStats == null &&
                    state.rankingHistory.isEmpty() -> {
                    EmptyHint("该刊暂无上榜记录")
                }

                else -> {
                    state.rankingStats?.let { stats ->
                        RankingStatsCard(stats)
                    }

                    if (state.rankingHistory.isNotEmpty()) {
                        RankingHistoryCard(
                            history = state.rankingHistory,
                            total = state.rankingTotal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingStatsCard(stats: SongRankingStatsDto) {
    val latestRank = stats.latestRank
    val prevRank = stats.prevRank
    val trend = if (
        latestRank != null &&
        latestRank > 0 &&
        prevRank != null &&
        prevRank > 0
    ) {
        prevRank - latestRank
    } else {
        null
    }
    val latestSubtitle = when {
        trend == null -> null
        trend > 0 -> "较上期上升 $trend 名"
        trend < 0 -> "较上期下降 ${-trend} 名"
        else -> "与上期持平"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        MetricGrid(
            items = listOf(
                MetricItem(
                    label = "最佳排名",
                    value = stats.bestRank.takeIf { it > 0 }?.toString() ?: "-",
                    subtitle = stats.bestRankIssue?.let { "第 $it 期" },
                ),
                MetricItem(
                    label = "最新排名",
                    value = latestRank?.toString() ?: "-",
                    subtitle = latestSubtitle,
                ),
                MetricItem(
                    label = "平均排名",
                    value = stats.avgRank?.roundToInt()?.toString() ?: "-",
                    subtitle = "近 ${stats.recentCount} 期",
                ),
                MetricItem(
                    label = "上榜次数",
                    value = stats.top20Count.toString(),
                    subtitle = "其中第 1 名 ${stats.top1Count} 次",
                ),
                MetricItem(
                    label = "最高得分",
                    value = formatNumber(stats.bestPoint),
                    subtitle = stats.bestPointIssue?.let { "第 $it 期" },
                ),
                MetricItem(
                    label = "首次上榜",
                    value = "第 ${stats.firstIssue} 期",
                    subtitle = stats.firstIssueRank?.let { "排名 $it" },
                ),
            ),
            modifier = Modifier.padding(vertical = 12.dp),
            columns = 3,
        )
    }
}

@Composable
private fun RankingHistoryCard(
    history: List<RankingDto>,
    total: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column {
            history.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "#${item.rank}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "第 ${item.issue} 期",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = "${formatPoint(item.point)} 分",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (index != history.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }

            if (total > history.size) {
                Text(
                    text = "共 $total 次上榜，仅显示最近 ${history.size} 次",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SongRatingsSection(
    state: SongDetailUiState,
    onRetry: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val summary = state.ratingSummary
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "评分",
            subtitle = summary?.total?.takeIf { it > 0 }?.let { "共 $it 条" },
        )

        summary?.takeIf { it.total > 0 }?.let {
            RatingSummaryCard(it)
        }

        if (state.ratingsLoading && state.ratings.isEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        when {
            state.ratingsError != null -> {
                ScreenMessage(
                    title = "评分加载失败",
                    message = state.ratingsError ?: "请稍后重试",
                    actionLabel = "重试",
                    onAction = onRetry,
                )
            }

            state.ratings.isEmpty() && !state.ratingsLoading -> {
                EmptyHint("还没有人评分")
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.ratings.forEach { rating ->
                        RatingCard(rating)
                    }
                }
            }
        }

        if (state.ratingsTotalPages > 1) {
            PagerRow(
                page = state.ratingsPage,
                totalPages = state.ratingsTotalPages,
                total = summary?.total ?: state.ratings.size,
                loading = state.ratingsLoading,
                onPrevious = onPrevious,
                onNext = onNext,
                unit = "条",
            )
        }
    }
}

@Composable
private fun RatingSummaryCard(summary: RatingSummaryDto) {
    val average = summary.avgScore?.let {
        String.format(Locale.CHINA, "%.1f", it)
    } ?: "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(
                    text = average,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "平均分 / 10",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${summary.total} 人评分",
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun RatingCard(rating: RatingDto) {
    val name = if (rating.isAnonymous && !rating.isRevealed) {
        "匿名用户"
    } else {
        rating.user.nickname.ifBlank { "用户 #${rating.user.id}" }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NetworkImage(
                url = rating.user.avatar,
                contentDescription = name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${rating.score} 分",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = timeAgo(rating.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                rating.comment
                    ?.takeIf { it.isNotBlank() }
                    ?.let { comment ->
                        Text(
                            text = comment,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
            }
        }
    }
}

@Composable
private fun SongCommentsSection(
    state: SongDetailUiState,
    onLogin: () -> Unit,
    onRetry: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "评论",
            subtitle = state.commentsTotal.takeIf { it > 0 }?.let { "共 $it 条" },
        )

        if (!state.isLoggedIn) {
            LoginHintCard(
                message = "登录后即可查看与参与评论",
                onLogin = onLogin,
            )
        } else {
            if (state.commentsLoading && state.comments.isEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                state.commentsError != null -> {
                    ScreenMessage(
                        title = "评论加载失败",
                        message = state.commentsError ?: "请稍后重试",
                        actionLabel = "重试",
                        onAction = onRetry,
                    )
                }

                state.comments.isEmpty() && !state.commentsLoading -> {
                    EmptyHint("还没有评论，来抢沙发")
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.comments.forEach { comment ->
                            CommentCard(comment)
                        }
                    }
                }
            }

            if (state.commentsTotalPages > 1) {
                PagerRow(
                    page = state.commentsPage,
                    totalPages = state.commentsTotalPages,
                    total = state.commentsTotal,
                    loading = state.commentsLoading,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    unit = "条",
                )
            }
        }
    }
}

@Composable
private fun CommentCard(comment: CommentDto) {
    val name = comment.user.nickname.ifBlank { "用户 #${comment.user.id}" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NetworkImage(
                url = comment.user.avatar,
                contentDescription = name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = timeAgo(comment.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                comment.replyTo
                    ?.nickname
                    ?.takeIf { it.isNotBlank() }
                    ?.let { replyName ->
                        Text(
                            text = "回复 @$replyName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                Text(
                    text = if (comment.isDeleted) "该评论已删除" else comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (comment.isDeleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )

                if (comment.likeCount > 0 || comment.replyCount > 0) {
                    Text(
                        text = listOfNotNull(
                            comment.likeCount.takeIf { it > 0 }?.let { "$it 赞" },
                            comment.replyCount.takeIf { it > 0 }?.let { "$it 回复" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginHintCard(
    message: String,
    onLogin: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "需要登录",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onLogin) {
                Text("去登录")
            }
        }
    }
}

@Composable
private fun VideoCard(video: VideoDto) {
    val context = LocalContext.current
    val metrics = video.latestSnapshot
        ?.let { "${formatNumber(it.view)} 播放" }
        ?.takeIf { it.isNotBlank() }
    val date = formatShortDate(video.pubdate).takeIf { it.isNotBlank() }
    val meta = listOfNotNull(metrics, date).joinToString(" · ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openBilibiliVideo(context, video.bvid) }
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NetworkImage(
                url = video.thumbnail,
                contentDescription = video.title,
                modifier = Modifier
                    .width(124.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp)),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = video.title?.takeIf { it.isNotBlank() } ?: "未命名视频",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = video.uploader?.name?.takeIf { it.isNotBlank() } ?: "未知UP主",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(
                        enabled = !video.uploader?.uid.isNullOrBlank(),
                        onClick = {
                            openBilibiliUser(context, video.uploader?.uid)
                        },
                    ),
                )

                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (video.disabled) {
                    Text(
                        text = "视频已失效",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private fun formatPoint(value: Double): String =
    String.format(Locale.CHINA, "%.1f", value)
