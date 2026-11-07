package xyz.lisfox.vocaland.ui.ranking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import xyz.lisfox.vocaland.data.discovery.ArtistRankingItemDto
import xyz.lisfox.vocaland.data.discovery.ArtistRankingSongsPage
import xyz.lisfox.vocaland.data.discovery.DiscoveryAuth
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.RankingBoard
import xyz.lisfox.vocaland.data.model.RankingDto
import xyz.lisfox.vocaland.data.model.RankingOrder

import xyz.lisfox.vocaland.data.model.RankingTab
import xyz.lisfox.vocaland.data.model.rankingIssueDateRange
import xyz.lisfox.vocaland.data.repository.RankingRepository
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.ui.components.InlineLoading
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.PagerRow
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SongCoverCard
import xyz.lisfox.vocaland.ui.components.songCoverSharedKey
import xyz.lisfox.vocaland.util.artistLine
import xyz.lisfox.vocaland.util.compactParts
import xyz.lisfox.vocaland.util.formatNumber
import xyz.lisfox.vocaland.util.songDisplayName
import kotlin.math.roundToLong

@Composable
fun RankingScreen(
    repository: RankingRepository,
    onOpenSong: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onOpenArtist: (ArtistKind, Int) -> Unit = { _, _ -> },
    session: AuthSession? = null,
) {
    val viewModel: RankingViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RankingViewModel(repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sessionState = if (session != null) {
        session.state.collectAsStateWithLifecycle().value
    } else {
        null
    }

    LaunchedEffect(session, sessionState?.user?.role) {
        if (session != null) {
            DiscoveryAuth.bindSession(session)
        }
        viewModel.setRole(sessionState?.user?.role ?: "guest")
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BoardSelector(
                selected = state.board,
                onSelect = viewModel::selectBoard,
            )
        }
        item {
            TabSelector(
                state = state,
                onSelect = viewModel::selectTab,
            )
        }
        item {
            IssueSelector(
                state = state,
                onPrevious = viewModel::previousIssue,
                onNext = viewModel::nextIssue,
                onLatest = viewModel::goToLatestIssue,
            )
        }
        if (state.tab.isSongTab) {
            item {
                SongToolbar(
                    state = state,
                    onSelectOrder = viewModel::selectOrder,
                    onSelectViewMode = viewModel::setViewMode,
                )
            }
        }

        if (state.loading || state.tabLoading) {
            item {
                InlineLoading()
            }
        }

        state.permissionHint?.let { hint ->
            item {
                HintCard(hint)
            }
        }
        state.issueNotice?.let { notice ->
            item {
                HintCard(notice)
            }
        }

        when (state.tab) {
            RankingTab.Main, RankingTab.New -> {
                val currentData = state.data
                if (state.error != null && currentData == null) {
                    item {
                        ScreenMessage(
                            title = "榜单加载失败",
                            message = state.error ?: "请检查网络后重试",
                            actionLabel = "重试",
                            onAction = viewModel::retry,
                        )
                    }
                } else if (state.error != null) {
                    item {
                        InlineError(state.error, viewModel::retry)
                    }
                }

                if (currentData != null) {
                    if (currentData.items.isEmpty() && !state.loading) {
                        item {
                            EmptyHint("该期暂无榜单数据")
                        }
                    } else if (state.viewMode == RankingViewMode.Grid) {
                        items(
                            items = currentData.items.chunked(2),
                            key = { row ->
                                "grid-${row.joinToString("-") { it.id.toString() }}"
                            },
                        ) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                row.forEach { item ->
                                    SongCoverCard(
                                        title = songDisplayName(item.song),
                                        subtitle = item.song.artistLine(),
                                        thumbnail = item.video?.thumbnail
                                            ?: item.song.thumbnail,
                                        onClick = { onOpenSong(item.song.id) },
                                        modifier = Modifier.weight(1f),
                                        sharedKey = songCoverSharedKey(item.song.id),
                                        badge = "#${item.rank}",
                                        value = formatNumber(statValue(item, currentData.order)),
                                    )
                                }
                                if (row.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        item {
                            PagerRow(
                                page = currentData.page,
                                totalPages = currentData.totalPages,
                                total = currentData.total,
                                loading = false,
                                onPrevious = viewModel::previousPage,
                                onNext = viewModel::nextPage,
                            )
                        }
                    } else {
                        items(currentData.items, key = { it.id }) { item ->
                            RankingRow(
                                item = item,
                                order = currentData.order,
                                onClick = { onOpenSong(item.song.id) },
                            )
                        }
                        item {
                            PagerRow(
                                page = currentData.page,
                                totalPages = currentData.totalPages,
                                total = currentData.total,
                                loading = false,
                                onPrevious = viewModel::previousPage,
                                onNext = viewModel::nextPage,
                            )
                        }
                    }
                } else if (state.loading) {
                    item {
                        LoadingPane(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            text = "正在加载榜单",
                        )
                    }
                }
            }

            RankingTab.Vocalist, RankingTab.Producer -> {
                val artistType = if (state.tab == RankingTab.Vocalist) {
                    ArtistKind.Vocalist
                } else {
                    ArtistKind.Producer
                }
                if (state.tabError != null) {
                    item {
                        InlineError(state.tabError, viewModel::retry)
                    }
                }
                val artistData = state.artistData
                if (artistData != null && state.permissionHint == null) {
                    if (artistData.items.isEmpty() && !state.tabLoading) {
                        item {
                            EmptyHint("该期暂无艺人排行数据")
                        }
                    } else {
                        items(artistData.items, key = { "${artistType.apiType}-${it.artist.id}" }) { item ->
                            ArtistRow(
                                item = item,
                                selected = state.selectedArtist?.artist?.id == item.artist.id,
                                onClick = { viewModel.selectArtist(item) },
                                onOpenArtist = {
                                    onOpenArtist(artistType, item.artist.id)
                                },
                                onOpenSong = onOpenSong,
                            )
                        }
                        item {
                            PagerRow(
                                page = state.artistPage,
                                totalPages = artistData.totalPages,
                                total = artistData.total,
                                loading = state.tabLoading,
                                onPrevious = viewModel::previousPage,
                                onNext = viewModel::nextPage,
                                unit = "位",
                            )
                        }
                    }
                }

                state.selectedArtist?.let { selected ->
                    item(key = "artist-songs-${selected.artist.id}") {
                        ArtistSongPanel(
                            artist = selected,
                            pageData = state.artistSongs,
                            page = state.artistSongsPage,
                            loading = state.tabLoading,
                            error = state.tabError,
                            onOpenSong = onOpenSong,
                            onRetry = viewModel::retryArtistSongs,
                            onPrevious = viewModel::artistSongsPreviousPage,
                            onNext = viewModel::artistSongsNextPage,
                        )
                    }
                }
            }

            RankingTab.Milestone -> {
                if (state.tabError != null) {
                    item {
                        InlineError(state.tabError, viewModel::retry)
                    }
                }
                val milestoneData = state.milestoneData
                if (milestoneData != null && state.permissionHint == null) {
                    if (milestoneData.items.isEmpty() && !state.tabLoading) {
                        item {
                            EmptyHint("该期暂无里程碑达成")
                        }
                    } else {
                        items(
                            milestoneData.items,
                            key = { "milestone-${it.id}" },
                        ) { item ->
                            MilestoneRow(
                                item = item,
                                onClick = { onOpenSong(item.song.id) },
                            )
                        }
                        item {
                            PagerRow(
                                page = state.milestonePage,
                                totalPages = milestoneData.totalPages,
                                total = milestoneData.total,
                                loading = state.tabLoading,
                                onPrevious = viewModel::previousPage,
                                onNext = viewModel::nextPage,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardSelector(
    selected: RankingBoard,
    onSelect: (RankingBoard) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(RankingBoard.entries, key = { it.key }) { board ->
            FilterChip(
                selected = board == selected,
                onClick = { onSelect(board) },
                label = { Text(board.label) },
            )
        }
    }
}

@Composable
private fun TabSelector(
    state: RankingUiState,
    onSelect: (RankingTab) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.tabs, key = { it.key }) { tab ->
            FilterChip(
                selected = state.tab == tab,
                onClick = { onSelect(tab) },
                label = { Text(tab.label) },
            )
        }
    }
}

@Composable
private fun SongToolbar(
    state: RankingUiState,
    onSelectOrder: (RankingOrder) -> Unit,
    onSelectViewMode: (RankingViewMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SortMenu(
            selected = state.order,
            onSelect = onSelectOrder,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.viewMode == RankingViewMode.List,
                onClick = { onSelectViewMode(RankingViewMode.List) },
                label = { Text("列表") },
            )
            FilterChip(
                selected = state.viewMode == RankingViewMode.Grid,
                onClick = { onSelectViewMode(RankingViewMode.Grid) },
                label = { Text("网格") },
            )
        }
    }
}

@Composable
private fun IssueSelector(
    state: RankingUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLatest: () -> Unit,
) {
    val issue = state.displayIssue
    val latest = state.latestIssue ?: state.data?.latestIssue

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = issue != null && state.oldestIssue != null && issue > state.oldestIssue,
            ) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "上一期")
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (issue == null) "最新一期" else "第 $issue 期",
                    style = MaterialTheme.typography.titleLarge,
                )
                val range = state.board.let { board ->
                    issue?.let { rankingIssueDateRange(board, it) }
                }
                Text(
                    text = range ?: when {
                        latest == null -> "正在获取期号"
                        issue == latest -> "已是最新一期"
                        else -> "最新第 $latest 期"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (issue != null && latest != null && issue < latest) {
                    TextButton(
                        onClick = onLatest,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text(
                            text = "回到最新",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            IconButton(
                onClick = onNext,
                enabled = issue != null && latest != null && issue < latest,
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "下一期")
            }
        }
    }
}

@Composable
private fun SortMenu(
    selected: RankingOrder,
    onSelect: (RankingOrder) -> Unit,
) {
    val expanded = remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded.value = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = "排序：${selected.label}",
                style = MaterialTheme.typography.labelMedium,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
        ) {
            RankingOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label) },
                    onClick = {
                        expanded.value = false
                        onSelect(order)
                    },
                )
            }
        }
    }
}

@Composable
private fun HintCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun InlineError(
    message: String?,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message.orEmpty(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun RankingRow(
    item: RankingDto,
    order: RankingOrder,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.width(38.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = item.rank.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = when (item.rank) {
                        1 -> MaterialTheme.colorScheme.primary
                        2 -> MaterialTheme.colorScheme.tertiary
                        3 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                RankDelta(item)
            }
            Spacer(Modifier.width(10.dp))
            NetworkImage(
                url = item.video?.thumbnail ?: item.song.thumbnail,
                contentDescription = songDisplayName(item.song),
                modifier = Modifier
                    .width(86.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = songDisplayName(item.song),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.song.artistLine(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.count > 0) {
                    Text(
                        text = "上榜 ${item.count} 次",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatNumber(statValue(item, order)),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = order.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RankDelta(item: RankingDto) {
    val previous = item.last
    if (previous == null || previous.rank <= 0) {
        Text(
            text = "NEW",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        return
    }

    val delta = previous.rank - item.rank
    when {
        delta > 0 -> Text(
            text = "↑$delta",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        delta < 0 -> Text(
            text = "↓${-delta}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )

        else -> Text(
            text = "—",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


@Composable
private fun ArtistRow(
    item: ArtistRankingItemDto,
    selected: Boolean,
    onClick: () -> Unit,
    onOpenArtist: () -> Unit,
    onOpenSong: (Int) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.rank.toString(),
                    modifier = Modifier.width(36.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                NetworkImage(
                    url = item.artist.picture,
                    contentDescription = item.artist.name,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(50)),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.artist.name,
                        modifier = Modifier.clickable(onClick = onOpenArtist),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "${item.songCount} 首上榜",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatNumber(item.totalPoint.roundToLong()),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (item.songs.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                item.songs.take(3).forEach { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSong(song.id) }
                            .padding(vertical = 3.dp),
                    ) {
                        Text(
                            text = "#${song.rank}",
                            modifier = Modifier.width(38.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = song.displayName?.trim()
                                ?.takeIf { it.isNotEmpty() } ?: song.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = formatNumber(song.point.roundToLong()),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistSongPanel(
    artist: ArtistRankingItemDto,
    pageData: ArtistRankingSongsPage?,
    page: Int,
    loading: Boolean,
    error: String?,
    onOpenSong: (Int) -> Unit,
    onRetry: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${artist.artist.name} 的上榜歌曲",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            if (loading && pageData == null) {
                InlineLoading()
            }
            if (error != null) {
                InlineError(error, onRetry)
            }
            pageData?.songs?.forEach { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSong(song.id) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "#${song.rank}",
                        modifier = Modifier.width(40.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.displayName?.trim()
                                ?.takeIf { it.isNotEmpty() } ?: song.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = artistLine(song.producers, song.vocalists),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = formatNumber(song.point.roundToLong()),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            if (pageData != null && pageData.totalPages > 1) {
                PagerRow(
                    page = page,
                    totalPages = pageData.totalPages,
                    total = pageData.total,
                    loading = loading,
                    onPrevious = onPrevious,
                    onNext = onNext,
                )
            }
        }
    }
}

@Composable
private fun MilestoneRow(
    item: xyz.lisfox.vocaland.data.model.MilestoneDto,
    onClick: () -> Unit,
) {
    val (value, unit) = compactParts(item.milestone)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NetworkImage(
                url = item.video?.thumbnail ?: item.song.thumbnail,
                contentDescription = songDisplayName(item.song),
                modifier = Modifier
                    .width(86.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = songDisplayName(item.song),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.song.artistLine(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.achievedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "$value$unit",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun statValue(item: RankingDto, order: RankingOrder): Long = when (order) {
    RankingOrder.Point -> item.point.roundToLong()
    RankingOrder.View -> item.view
    RankingOrder.Favorite -> item.favorite
    RankingOrder.Coin -> item.coin
    RankingOrder.Like -> item.like
    RankingOrder.Danmaku -> item.danmaku
    RankingOrder.Reply -> item.reply
    RankingOrder.Share -> item.share
}
