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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.model.RankingBoard
import xyz.lisfox.vocaland.data.model.RankingDto
import xyz.lisfox.vocaland.data.model.RankingOrder
import xyz.lisfox.vocaland.data.model.RankingPage
import xyz.lisfox.vocaland.data.repository.RankingRepository
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.util.artistLine
import xyz.lisfox.vocaland.util.formatNumber
import xyz.lisfox.vocaland.util.songDisplayName
import kotlin.math.roundToLong

@Composable
fun RankingScreen(
    repository: RankingRepository,
    onOpenFeature: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: RankingViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RankingViewModel(repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            IssueSelector(
                state = state,
                onPrevious = viewModel::previousIssue,
                onNext = viewModel::nextIssue,
                onLatest = viewModel::goToLatestIssue,
            )
        }
        item {
            FilterBar(
                state = state,
                onSelectPart = viewModel::selectPart,
                onSelectOrder = viewModel::selectOrder,
            )
        }
        if (state.loading) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        state.issueNotice?.let { notice ->
            item {
                IssueNotice(notice)
            }
        }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.error.orEmpty(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = viewModel::retry) {
                        Text("重试")
                    }
                }
            }
        }

        if (currentData != null) {
            if (currentData.items.isEmpty() && !state.loading) {
                item {
                    EmptyHint("该期暂无榜单数据")
                }
            }
            items(currentData.items, key = { it.id }) { item ->
                RankingRow(
                    item = item,
                    order = currentData.order,
                    onClick = { onOpenFeature("歌曲：${songDisplayName(item.song)}") },
                )
            }
            if (currentData.items.isNotEmpty()) {
                item {
                    RankingPagination(
                        data = currentData,
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
                    text = "正在加载榜单…",
                )
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
private fun IssueSelector(
    state: RankingUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLatest: () -> Unit,
) {
    val issue = state.data?.issue ?: state.issue
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
                enabled = issue != null && issue > 1,
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
                val hint = when {
                    latest == null -> "正在获取期号"
                    issue == latest -> "已是最新一期"
                    else -> "最新第 $latest 期"
                }
                if (issue != null && latest != null && issue < latest) {
                    Text(
                        text = "$hint · 回到最新",
                        modifier = Modifier.clickable(onClick = onLatest),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
private fun FilterBar(
    state: RankingUiState,
    onSelectPart: (String) -> Unit,
    onSelectOrder: (RankingOrder) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.board.supportsNewPart) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.part == "main",
                    onClick = { onSelectPart("main") },
                    label = { Text("主榜") },
                )
                FilterChip(
                    selected = state.part == "new",
                    onClick = { onSelectPart("new") },
                    label = { Text("新曲") },
                )
            }
        } else {
            Text(
                text = "主榜",
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SortMenu(
            selected = state.order,
            onSelect = onSelectOrder,
        )
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
private fun IssueNotice(text: String) {
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
                    style = MaterialTheme.typography.titleMedium,
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
private fun RankingPagination(
    data: RankingPage,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onPrevious,
            enabled = data.page > 1,
        ) {
            Text("上一页")
        }
        Text(
            text = "第 ${data.page} / ${data.totalPages} 页 · 共 ${data.total} 条",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = onNext,
            enabled = data.page < data.totalPages,
        ) {
            Text("下一页")
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
