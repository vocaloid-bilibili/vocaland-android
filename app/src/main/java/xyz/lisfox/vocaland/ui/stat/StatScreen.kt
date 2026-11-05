package xyz.lisfox.vocaland.ui.stat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.model.SearchEntityDto
import xyz.lisfox.vocaland.data.model.SongListItemDto
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.data.stat.StatFilterState
import xyz.lisfox.vocaland.data.stat.StatOrderBy
import xyz.lisfox.vocaland.data.stat.StatRepository
import xyz.lisfox.vocaland.data.stat.VocaRole
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.PagerRow
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.util.artistLine
import xyz.lisfox.vocaland.util.formatNumber
import xyz.lisfox.vocaland.util.songDisplayName

@Composable
fun StatScreen(
    repository: StatRepository,
    session: AuthSession,
    onOpenSong: (Int) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSponsor: (() -> Unit)? = null,
) {
    val viewModel: StatViewModel = viewModel(
        factory = viewModelFactory {
            initializer { StatViewModel(repository, session) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    StatContent(
        state = state,
        onOpenSong = onOpenSong,
        onLogin = onLogin,
        onOpenSponsor = onOpenSponsor,
        onFilterChange = viewModel::applyFilter,
        onReset = viewModel::resetFilter,
        onSelectOrder = viewModel::selectOrderBy,
        onToggleOrder = viewModel::toggleOrder,
        onIncludeAllChange = viewModel::setIncludeAll,
        onPrevious = viewModel::previousPage,
        onNext = viewModel::nextPage,
        onRetry = viewModel::refresh,
        onSearchEntities = { type, keyword ->
            viewModel.searchEntities(type, keyword).getOrDefault(emptyList())
        },
        modifier = modifier,
    )
}

@Composable
private fun StatContent(
    state: StatUiState,
    onOpenSong: (Int) -> Unit,
    onLogin: () -> Unit,
    onOpenSponsor: (() -> Unit)?,
    onFilterChange: (StatFilterState) -> Unit,
    onReset: () -> Unit,
    onSelectOrder: (StatOrderBy) -> Unit,
    onToggleOrder: () -> Unit,
    onIncludeAllChange: (Boolean) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onSearchEntities: suspend (String, String) -> List<SearchEntityDto>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column {
                Text(
                    text = "数据统计",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "按数据项排序、筛选全站歌曲",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            StatToolbar(
                state = state,
                onSelectOrder = onSelectOrder,
                onToggleOrder = onToggleOrder,
                onIncludeAllChange = onIncludeAllChange,
            )
        }

        item {
            StatFilterPanel(
                filter = state.filter,
                total = state.total,
                enabled = state.canFilter,
                onFilterChange = onFilterChange,
                onReset = onReset,
                onSearchEntities = onSearchEntities,
                onLogin = onLogin,
                onOpenSponsor = onOpenSponsor,
            )
        }

        state.error?.let { error ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRetry) { Text("重试") }
                }
            }
        }

        if (state.data == null) {
            if (state.loading) {
                item {
                    LoadingPane(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        text = "正在加载统计数据…",
                    )
                }
            } else {
                item {
                    ScreenMessage(
                        title = "统计数据加载失败",
                        message = "请稍后重试",
                        actionLabel = "重新加载",
                        onAction = onRetry,
                    )
                }
            }
            return@LazyColumn
        }

        val data = state.data ?: return@LazyColumn
        if (state.loading) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }
        if (data.items.isEmpty() && !state.loading) {
            item { EmptyHint(text = "没有符合条件的歌曲") }
        } else {
            itemsIndexed(
                items = data.items.chunked(2),
                key = { index, row -> "stat-${state.effectivePage}-$index-${row.firstOrNull()?.song?.id ?: 0}" },
            ) { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEachIndexed { column, item ->
                        StatSongCard(
                            item = item,
                            rank = (data.page - 1) * 50 + index * 2 + column + 1,
                            orderBy = state.data?.query?.orderBy ?: state.orderBy,
                            onClick = { onOpenSong(item.song.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        if (data.items.isNotEmpty()) {
            item {
                val maxPage = state.maxAccessiblePage
                val accessiblePages = maxPage?.let { minOf(data.totalPages, it) } ?: data.totalPages
                PagerRow(
                    page = data.page,
                    totalPages = accessiblePages,
                    total = data.total,
                    loading = state.loading,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    unit = "首",
                )
                if (maxPage != null && data.totalPages > accessiblePages) {
                    LockedPagesHint(
                        isGuest = state.role == VocaRole.Guest,
                        onLogin = onLogin,
                        onOpenSponsor = onOpenSponsor,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatToolbar(
    state: StatUiState,
    onSelectOrder: (StatOrderBy) -> Unit,
    onToggleOrder: () -> Unit,
    onIncludeAllChange: (Boolean) -> Unit,
) {
    Column {
        if (state.canIncludeAll) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "包含全部（含未收录）",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(
                    checked = state.includeAll,
                    onCheckedChange = onIncludeAllChange,
                )
            }
        }
        if (!state.canSort) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "登录后可使用排序功能（当前按播放降序）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SortMenu(
                    selected = state.orderBy,
                    onSelect = onSelectOrder,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleOrder) {
                    Icon(
                        imageVector = if (state.order.key == "desc") {
                            Icons.Filled.ArrowDownward
                        } else {
                            Icons.Filled.ArrowUpward
                        },
                        contentDescription = if (state.order.key == "desc") "降序" else "升序",
                    )
                }
            }
        }
    }
}

@Composable
private fun SortMenu(
    selected: StatOrderBy,
    onSelect: (StatOrderBy) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("排序：${selected.label}")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            StatOrderBy.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun StatSongCard(
    item: SongListItemDto,
    rank: Int,
    orderBy: StatOrderBy,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column {
            Box {
                NetworkImage(
                    url = item.video?.thumbnail ?: item.song.thumbnail,
                    contentDescription = songDisplayName(item.song),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                )
                StatusPill(
                    text = rank.toString(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                ) {
                    Text(
                        text = statValue(item, orderBy),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = songDisplayName(item.song),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.song.artistLine(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LockedPagesHint(
    isGuest: Boolean,
    onLogin: () -> Unit,
    onOpenSponsor: (() -> Unit)?,
) {
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
            Text(
                text = if (isGuest) "登录后可浏览更多数据" else "赞助后可查看全部数据",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(
                onClick = {
                    if (isGuest || onOpenSponsor == null) onLogin() else onOpenSponsor()
                },
            ) {
                Text(if (isGuest) "去登录" else "前往赞助")
            }
        }
    }
}

private fun statValue(item: SongListItemDto, orderBy: StatOrderBy): String {
    val snapshot = item.snapshot
    return when (orderBy) {
        StatOrderBy.View -> formatNumber(snapshot?.view ?: 0)
        StatOrderBy.Favorite -> formatNumber(snapshot?.favorite ?: 0)
        StatOrderBy.Coin -> formatNumber(snapshot?.coin ?: 0)
        StatOrderBy.Like -> formatNumber(snapshot?.like ?: 0)
        StatOrderBy.Danmaku -> formatNumber(snapshot?.danmaku ?: 0)
        StatOrderBy.Reply -> formatNumber(snapshot?.reply ?: 0)
        StatOrderBy.Share -> formatNumber(snapshot?.share ?: 0)
        StatOrderBy.Pubdate -> item.video?.pubdate?.take(10).orEmpty()
        StatOrderBy.DailyCount -> "${item.count?.daily ?: 0} 次"
        StatOrderBy.WeeklyCount -> "${item.count?.weekly ?: 0} 次"
    }
}
