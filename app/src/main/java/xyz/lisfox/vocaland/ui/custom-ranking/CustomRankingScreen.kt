package xyz.lisfox.vocaland.ui.customranking

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.customranking.CustomRankingQuery
import xyz.lisfox.vocaland.data.customranking.CustomRankingRepository
import xyz.lisfox.vocaland.data.customranking.CustomRankingSourceType
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.data.stat.StatFilterState
import xyz.lisfox.vocaland.ui.components.InlineLoading
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.PagerRow
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.stat.StatFilterPanel

@Composable
fun CustomRankingScreen(
    repository: CustomRankingRepository,
    session: AuthSession,
    onOpenSong: (Int) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSponsor: (() -> Unit)? = null,
    initialQuery: CustomRankingQuery? = null,
) {
    val viewModel: CustomRankingViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CustomRankingViewModel(repository, session, initialQuery) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var playlistPickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    if (playlistPickerOpen) {
        PlaylistPickerDialog(
            playlists = state.playlists,
            loading = state.playlistsLoading,
            onSearch = { keyword -> viewModel.loadPlaylists(keyword) },
            onDismiss = { playlistPickerOpen = false },
            onSelect = { playlist ->
                viewModel.selectPlaylist(playlist)
                playlistPickerOpen = false
            },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "自定义数据项权重与筛选条件，按公式实时算分排名",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!state.canUse) {
            item {
                ScreenMessage(
                    title = "需要赞助权限",
                    message = "仅赞助者及以上用户可使用自定义公式生成排行。",
                    actionLabel = if (onOpenSponsor != null) "前往赞助" else "去登录",
                    onAction = {
                        if (onOpenSponsor != null) onOpenSponsor() else onLogin()
                    },
                )
            }
            return@LazyColumn
        }

        item {
            SourcePicker(
                source = state.draft.source,
                playlistName = state.playlists
                    .firstOrNull { it.id == state.draft.source.playlistId }
                    ?.name,
                onSelectType = viewModel::selectSourceType,
                onOpenPlaylistPicker = {
                    viewModel.loadPlaylists()
                    playlistPickerOpen = true
                },
            )
        }

        item {
            FormulaEditor(
                formula = state.draft.formula,
                onChange = viewModel::updateFormula,
            )
        }

        if (state.draft.source.typeEnum == CustomRankingSourceType.Songs) {
            item {
                StatFilterPanel(
                    filter = state.draft.filter,
                    total = state.page?.total ?: 0,
                    enabled = true,
                    onFilterChange = viewModel::updateFilter,
                    onReset = { viewModel.updateFilter(StatFilterState()) },
                    onSearchEntities = viewModel::searchEntities,
                )
            }
        }

        item {
            RankingOptions(
                state = state,
                onIncludeAllChange = viewModel::setIncludeAll,
                onDedupChange = viewModel::setDedup,
                onLimitChange = viewModel::setLimit,
            )
        }

        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = viewModel::generate,
                        enabled = !state.loading,
                    ) {
                        Text(if (state.dirty) "生成排行" else "重新计算")
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (state.dirty) "参数已修改，点「生成排行」后生效" else "结果与当前参数一致",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
                    TextButton(onClick = viewModel::refresh) { Text("重试") }
                }
            }
        }

        val page = state.page
        if (page == null) {
            if (state.loading) {
                item {
                    LoadingPane(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        text = "计算中…",
                    )
                }
            } else {
                item {
                    EmptyHint(text = "设置好来源、公式与筛选条件后，点「生成排行」开始计算")
                }
            }
            return@LazyColumn
        }

        if (state.loading) {
            item { InlineLoading() }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "共 ${page.total} 条",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = dateRangeLabel(page.oldDate, page.newDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { viewModel.export(context) },
                    enabled = !state.exporting,
                ) {
                    Text(if (state.exporting) "导出中…" else "导出表格")
                }
            }
        }

        if (page.entries.isEmpty()) {
            item { EmptyHint(text = "没有符合条件的结果") }
        } else {
            itemsIndexed(
                items = page.entries,
                key = { _, entry -> "${entry.songId}-${entry.bvid}-${entry.rank}" },
            ) { _, entry ->
                CustomRankingResultRow(
                    entry = entry,
                    onClick = { onOpenSong(entry.songId) },
                )
            }
            item {
                PagerRow(
                    page = page.page,
                    totalPages = page.totalPages,
                    total = page.total,
                    loading = state.loading,
                    onPrevious = viewModel::previousPage,
                    onNext = viewModel::nextPage,
                )
            }
        }
    }
}

@Composable
private fun RankingOptions(
    state: CustomRankingUiState,
    onIncludeAllChange: (Boolean) -> Unit,
    onDedupChange: (Boolean) -> Unit,
    onLimitChange: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("排行选项", style = MaterialTheme.typography.titleMedium)
            if (state.canIncludeAll && state.draft.source.typeEnum == CustomRankingSourceType.Songs) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "包含全部（含未收录）",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Switch(
                        checked = state.draft.includeAll,
                        onCheckedChange = onIncludeAllChange,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "按歌曲去重",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                )
                Switch(
                    checked = state.draft.dedup,
                    onCheckedChange = onDedupChange,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "最多条目",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                )
                LimitField(
                    value = state.draft.limit,
                    onChange = onLimitChange,
                    modifier = Modifier.width(110.dp),
                )
            }
        }
    }
}

@Composable
private fun LimitField(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input.filter(Char::isDigit).take(3)
            text.toIntOrNull()?.let(onChange)
        },
        singleLine = true,
        modifier = modifier,
    )
}

private fun dateRangeLabel(oldDate: String?, newDate: String?): String = when {
    !oldDate.isNullOrBlank() && !newDate.isNullOrBlank() -> "$oldDate ~ $newDate"
    !newDate.isNullOrBlank() -> "截至 $newDate"
    !oldDate.isNullOrBlank() -> "自 $oldDate"
    else -> "累计数据"
}
