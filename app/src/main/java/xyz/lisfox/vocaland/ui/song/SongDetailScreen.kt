@file:OptIn(ExperimentalSharedTransitionApi::class)

package xyz.lisfox.vocaland.ui.song

import android.widget.Toast
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.VocaLandApplication
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.BgmTrack
import xyz.lisfox.vocaland.data.model.CommentDto
import xyz.lisfox.vocaland.data.model.RankingBoard
import xyz.lisfox.vocaland.data.model.RankingDto
import xyz.lisfox.vocaland.data.model.RatingDto
import xyz.lisfox.vocaland.data.model.RatingSummaryDto
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.SongRankingStatsDto
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.model.VideoDto
import xyz.lisfox.vocaland.data.player.BgmPlayerController
import xyz.lisfox.vocaland.data.player.BgmPlayerState
import xyz.lisfox.vocaland.data.repository.SongDetailRepository
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.data.songactions.SongActionsRepository
import xyz.lisfox.vocaland.data.songactions.SongListenStatus
import xyz.lisfox.vocaland.data.songactions.songVideoTrack
import xyz.lisfox.vocaland.data.songactions.sortSongVideos
import xyz.lisfox.vocaland.ui.components.InlineLoading
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LocalSharedElementVisibilityScope
import xyz.lisfox.vocaland.ui.components.LocalSharedTransitionScope
import xyz.lisfox.vocaland.ui.components.MetricGrid
import xyz.lisfox.vocaland.ui.components.MetricItem
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.PagerRow
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.ui.components.SharedCoverHint
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.ui.components.getSharedCoverBitmap
import xyz.lisfox.vocaland.ui.components.getSharedCoverHint
import xyz.lisfox.vocaland.ui.components.putSharedCoverBitmap
import xyz.lisfox.vocaland.ui.components.sharedCoverDecodeWidth
import xyz.lisfox.vocaland.ui.components.shimmer
import xyz.lisfox.vocaland.ui.components.songCoverSharedKey
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.formatNumber
import xyz.lisfox.vocaland.util.formatScore
import xyz.lisfox.vocaland.util.formatShortDate
import xyz.lisfox.vocaland.util.openBilibiliUser
import xyz.lisfox.vocaland.util.openBilibiliVideo
import xyz.lisfox.vocaland.util.songDisplayName
import xyz.lisfox.vocaland.util.timeAgo
import kotlin.math.roundToInt

private val songRankingBoards = listOf(
    RankingBoard.Daily to "日榜",
    RankingBoard.Weekly to "周榜",
    RankingBoard.Monthly to "月榜",
)

@Composable
private fun rememberPlayerState(controller: BgmPlayerController?): BgmPlayerState? {
    if (controller == null) return null
    val state by controller.state.collectAsStateWithLifecycle()
    return state
}

@Composable
fun SongDetailScreen(
    repository: SongDetailRepository,
    session: AuthSession,
    songId: Int,
    onOpenArtist: (ArtistKind, Int) -> Unit,
    onOpenTag: (Int) -> Unit,
    onOpenUser: (Int) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    playerController: BgmPlayerController? = null,
    songActionsRepository: SongActionsRepository? = null,
    onAddToPlaylist: ((Int) -> Unit)? = null,
    communitySlot: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as? VocaLandApplication)?.container
    val controller = playerController ?: appContainer?.bgmPlayerController
    val playerState = rememberPlayerState(controller)
    val viewModel: SongDetailViewModel = viewModel(
        key = "song-$songId",
        factory = viewModelFactory {
            initializer {
                SongDetailViewModel(repository, session, songId, songActionsRepository)
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 列表页已经知道的封面与标题：数据到达前先拿它们占位，转场第一帧就能画出完整封面
    val coverHint = remember(songId) { getSharedCoverHint(songCoverSharedKey(songId)) }

    when {
        state.song == null && !state.loading -> {
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
            SongDetailContent(
                state = state,
                song = state.song,
                songId = songId,
                coverHint = coverHint,
                playerState = playerState,
                playerAvailable = controller != null,
                listenAvailable = songActionsRepository != null,
                onOpenArtist = onOpenArtist,
                onOpenTag = onOpenTag,
                onOpenUser = onOpenUser,
                onLogin = onLogin,
                onSelectVideo = viewModel::selectVideo,
                onSetListenStatus = viewModel::setListenStatus,
                onClearListenStatus = viewModel::clearListenStatus,
                onRetryListen = viewModel::retryListen,
                onAddToPlaylist = onAddToPlaylist,
                onPlayTrack = { track ->
                    if (controller == null) {
                        Toast.makeText(context, "播放器尚未就绪", Toast.LENGTH_SHORT).show()
                    } else {
                        controller.playTrackNow(track)
                    }
                },
                onTogglePlayback = { controller?.toggle() },
                onAddToQueue = { track ->
                    val added = controller?.addToQueue(track)
                    val message = when (added) {
                        null -> "播放器尚未就绪"
                        true -> "已加入播放队列"
                        false -> "已在播放队列中"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                },
                onSelectRankingBoard = viewModel::selectRankingBoard,
                onRetryRanking = viewModel::retryRanking,
                communitySlot = communitySlot,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun SongDetailContent(
    state: SongDetailUiState,
    song: SongDto?,
    songId: Int,
    coverHint: SharedCoverHint?,
    playerState: BgmPlayerState?,
    playerAvailable: Boolean,
    listenAvailable: Boolean,
    onOpenArtist: (ArtistKind, Int) -> Unit,
    onOpenTag: (Int) -> Unit,
    onOpenUser: (Int) -> Unit,
    onLogin: () -> Unit,
    onSelectVideo: (String) -> Unit,
    onSetListenStatus: (SongListenStatus) -> Unit,
    onClearListenStatus: () -> Unit,
    onRetryListen: () -> Unit,
    onAddToPlaylist: ((Int) -> Unit)?,
    onPlayTrack: (BgmTrack) -> Unit,
    onTogglePlayback: () -> Unit,
    onAddToQueue: (BgmTrack) -> Unit,
    onSelectRankingBoard: (RankingBoard) -> Unit,
    onRetryRanking: () -> Unit,
    modifier: Modifier = Modifier,
    communitySlot: @Composable () -> Unit,
) {
    val videos = remember(songId, song?.videos) {
        song?.let { sortSongVideos(it.videos) } ?: emptyList()
    }
    val selectedVideo = videos.firstOrNull { it.bvid == state.selectedBvid }
        ?: videos.firstOrNull()

    val producers = (song?.producers ?: emptyList())
        .map { it.producer }
        .filter { it.id > 0 }
        .distinctBy { it.id }
        .map { ArtistChip(it.id, it.name, ArtistKind.Producer) }
    val vocalists = (song?.vocalists ?: emptyList())
        .map { it.vocalist }
        .filter { it.id > 0 }
        .distinctBy { it.id }
        .map { ArtistChip(it.id, it.name, ArtistKind.Vocalist) }
    val synthesizers = (song?.vocalists ?: emptyList())
        .mapNotNull { it.synthesizer }
        .filter { it.id > 0 }
        .distinctBy { it.id }
        .map { ArtistChip(it.id, it.name, ArtistKind.Synthesizer) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 首帧就挂上共享元素容器：加载期间不换根布局，数据到达后只做重组，
        // 否则转场会因为没有目标元素而整段消失
        item(key = "hero") {
            SongHero(song = song, songId = songId, coverHint = coverHint)
        }

        if (song == null) {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            return@LazyColumn
        }

        item {
            SongActionSection(
                songId = song.id,
                state = state,
                listenAvailable = listenAvailable,
                onLogin = onLogin,
                onAddToPlaylist = onAddToPlaylist,
                onSetListenStatus = onSetListenStatus,
                onClearListenStatus = onClearListenStatus,
                onRetryListen = onRetryListen,
            )
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
                title = "投稿视频",
                subtitle = if (videos.isEmpty()) null else "共 ${videos.size} 个投稿",
            )
        }

        if (videos.isEmpty()) {
            item {
                EmptyHint("暂无视频")
            }
        } else {
            if (videos.size > 1) {
                item {
                    SubmissionSelector(
                        videos = videos,
                        selectedBvid = selectedVideo?.bvid,
                        onSelect = onSelectVideo,
                    )
                }
            }

            selectedVideo?.let { video ->
                item {
                    SubmissionDetailCard(
                        song = song,
                        video = video,
                        playerState = playerState,
                        playerAvailable = playerAvailable,
                        onPlayTrack = onPlayTrack,
                        onTogglePlayback = onTogglePlayback,
                        onAddToQueue = onAddToQueue,
                    )
                }
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

        item { communitySlot() }
    }
}

@Composable
private fun SongHero(
    song: SongDto?,
    songId: Int,
    coverHint: SharedCoverHint?,
) {
    val sharedScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalSharedElementVisibilityScope.current
    val coverModifier = if (sharedScope != null && visibilityScope != null) {
        with(sharedScope) {
            Modifier.sharedElement(
                state = rememberSharedContentState(songCoverSharedKey(songId)),
                animatedVisibilityScope = visibilityScope,
            )
        }
    } else {
        Modifier
    }
    val sharedKey = songCoverSharedKey(songId)
    val cachedCover = remember(songId) { getSharedCoverBitmap(sharedKey) }
    val title = song?.let { songDisplayName(it) } ?: coverHint?.title

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NetworkImage(
            url = song?.coverUrl() ?: coverHint?.url,
            contentDescription = title,
            modifier = coverModifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(24.dp)),
            decodeWidth = sharedCoverDecodeWidth(),
            cachedBitmap = cachedCover,
            onLoaded = { bitmap -> putSharedCoverBitmap(sharedKey, bitmap) },
        )

        if (title.isNullOrBlank()) {
            LoadingBar(Modifier.width(180.dp).height(26.dp))
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        if (song == null) return

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

@Composable
private fun LoadingBar(modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .shimmer(),
    )
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
                InlineLoading()
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
                        modifier = Modifier.width(48.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "第 ${item.issue} 期",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = "${formatScore(item.point)} 分",
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
private fun SongActionSection(
    songId: Int,
    state: SongDetailUiState,
    listenAvailable: Boolean,
    onLogin: () -> Unit,
    onAddToPlaylist: ((Int) -> Unit)?,
    onSetListenStatus: (SongListenStatus) -> Unit,
    onClearListenStatus: () -> Unit,
    onRetryListen: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val statusEnabled = listenAvailable && !state.listenUpdating

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SongActionItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        label = "加入歌单",
                        enabled = onAddToPlaylist != null,
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                        onClick = { onAddToPlaylist?.invoke(songId) },
                    )
                    SongActionItem(
                        modifier = Modifier.weight(1f),
                        icon = if (state.listenStatus == SongListenStatus.Want) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Filled.FavoriteBorder
                        },
                        label = SongListenStatus.Want.label,
                        selected = state.listenStatus == SongListenStatus.Want,
                        enabled = !state.isLoggedIn || statusEnabled,
                        onClick = {
                            if (state.isLoggedIn) {
                                onSetListenStatus(SongListenStatus.Want)
                            } else {
                                onLogin()
                            }
                        },
                    )
                    SongActionItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Check,
                        label = SongListenStatus.Listened.label,
                        selected = state.listenStatus == SongListenStatus.Listened,
                        enabled = !state.isLoggedIn || statusEnabled,
                        onClick = {
                            if (state.isLoggedIn) {
                                onSetListenStatus(SongListenStatus.Listened)
                            } else {
                                onLogin()
                            }
                        },
                    )
                }
            }
        }

        if (!listenAvailable) {
            Text(
                text = "标记功能尚未接入",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        } else {
            val error = state.listenError
            val hint = when {
                error != null -> error
                state.listenLoading || state.listenUpdating -> "正在同步标记…"
                !state.isLoggedIn -> "登录后可以标记想听 / 听过"
                state.listenStatus == SongListenStatus.Want -> "已标记为「想听」"
                state.listenStatus == SongListenStatus.Listened -> "已标记为「听过」"
                state.listenLitCount > 0 ->
                    "已有 ${formatNumber(state.listenLitCount.toLong())} 人听过"

                else -> "标记你的收听状态"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = hint,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (error != null) scheme.error else scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                when {
                    error != null -> {
                        TextButton(onClick = onRetryListen) {
                            Text("重试")
                        }
                    }

                    state.listenStatus != null -> {
                        TextButton(
                            onClick = onClearListenStatus,
                            enabled = statusEnabled,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.size(4.dp))
                            Text("清除标记")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val scheme = MaterialTheme.colorScheme
    val background = when {
        !enabled -> scheme.surfaceContainerHigh
        selected -> scheme.secondaryContainer
        else -> containerColor
    }
    val foreground = when {
        !enabled -> scheme.onSurface.copy(alpha = 0.38f)
        selected -> scheme.onSecondaryContainer
        else -> contentColor
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = foreground,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SubmissionSelector(
    videos: List<VideoDto>,
    selectedBvid: String?,
    onSelect: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(videos, key = { "${it.bvid}-${it.page ?: 0}" }) { video ->
            val selected = video.bvid == selectedBvid
            Column(
                modifier = Modifier
                    .width(148.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = 2.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(video.bvid) }
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NetworkImage(
                    url = video.thumbnail,
                    contentDescription = video.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Text(
                    text = video.title?.takeIf { it.isNotBlank() } ?: "未命名视频",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubmissionDetailCard(
    song: SongDto,
    video: VideoDto,
    playerState: BgmPlayerState?,
    playerAvailable: Boolean,
    onPlayTrack: (BgmTrack) -> Unit,
    onTogglePlayback: () -> Unit,
    onAddToQueue: (BgmTrack) -> Unit,
) {
    val context = LocalContext.current
    val track = remember(song.id, video.bvid) { songVideoTrack(song, video) }
    val isCurrent = playerState?.currentTrack?.bvid == video.bvid
    val isPlaying = isCurrent && playerState?.playing == true
    val inQueue = playerState?.tracks?.any { it.bvid == video.bvid } == true
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
        Column {
            NetworkImage(
                url = video.thumbnail,
                contentDescription = video.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = video.title?.takeIf { it.isNotBlank() } ?: "未命名视频",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "UP主",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val uploaderName = video.uploader?.name?.takeIf { it.isNotBlank() }
                    val uploaderUid = video.uploader?.uid?.takeIf { it.isNotBlank() }
                    Text(
                        text = uploaderName ?: "未知UP主",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uploaderUid != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            enabled = uploaderUid != null,
                            onClick = { openBilibiliUser(context, uploaderUid) },
                        ),
                    )
                }

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

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (isCurrent) {
                                onTogglePlayback()
                            } else {
                                track?.let(onPlayTrack)
                            }
                        },
                        enabled = playerAvailable && track != null && !video.disabled,
                    ) {
                        if (isPlaying) {
                            Icon(
                                imageVector = Icons.Filled.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.size(6.dp))
                        Text(
                            when {
                                !playerAvailable -> "播放器未就绪"
                                isPlaying -> "暂停"
                                isCurrent -> "继续播放"
                                else -> "站内播放"
                            },
                        )
                    }

                    OutlinedButton(
                        onClick = { track?.let(onAddToQueue) },
                        enabled = playerAvailable && track != null && !inQueue && !video.disabled,
                    ) {
                        Icon(
                            imageVector = if (inQueue) Icons.Filled.Check else Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(if (inQueue) "已在队列" else "加入队列")
                    }

                    TextButton(
                        onClick = { openBilibiliVideo(context, video.bvid) },
                    ) {
                        Text("B站观看")
                    }
                }
            }
        }
    }
}

