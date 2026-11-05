package xyz.lisfox.vocaland.ui.customranking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlinx.coroutines.delay
import xyz.lisfox.vocaland.data.customranking.CUSTOM_RANKING_PRESET_BOARDS
import xyz.lisfox.vocaland.data.customranking.CUSTOM_RANKING_STAT_KEYS
import xyz.lisfox.vocaland.data.customranking.CUSTOM_RANKING_STAT_LABELS
import xyz.lisfox.vocaland.data.customranking.CustomRankingEntryDto
import xyz.lisfox.vocaland.data.customranking.CustomRankingFormula
import xyz.lisfox.vocaland.data.customranking.CustomRankingFormulaMode
import xyz.lisfox.vocaland.data.customranking.CustomRankingSource
import xyz.lisfox.vocaland.data.customranking.CustomRankingSourceType
import xyz.lisfox.vocaland.data.customranking.statRank
import xyz.lisfox.vocaland.data.customranking.statValue
import xyz.lisfox.vocaland.data.model.PublicPlaylistDto
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.MetricGrid
import xyz.lisfox.vocaland.ui.components.MetricItem
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.ui.stat.DebouncedFilterTextField
import xyz.lisfox.vocaland.util.artistLine
import xyz.lisfox.vocaland.util.formatNumber
import xyz.lisfox.vocaland.util.songDisplayName

@Composable
fun SourcePicker(
    source: CustomRankingSource,
    playlistName: String?,
    onSelectType: (CustomRankingSourceType) -> Unit,
    onOpenPlaylistPicker: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("候选来源", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = source.typeEnum == CustomRankingSourceType.Songs,
                    onClick = { onSelectType(CustomRankingSourceType.Songs) },
                    label = { Text("歌曲筛选") },
                )
                FilterChip(
                    selected = source.typeEnum == CustomRankingSourceType.Playlist,
                    onClick = { onSelectType(CustomRankingSourceType.Playlist) },
                    label = { Text("歌单") },
                )
            }
            if (source.typeEnum == CustomRankingSourceType.Playlist) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenPlaylistPicker,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = playlistName
                            ?: source.playlistId?.let { "歌单 #$it" }
                            ?: "选择歌单",
                    )
                }
                Text(
                    text = "歌单来源的候选池固定为该歌单内的歌曲，筛选条件不适用。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun FormulaEditor(
    formula: CustomRankingFormula,
    onChange: (CustomRankingFormula) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("算分公式", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CustomRankingFormulaMode.entries.forEach { mode ->
                    FilterChip(
                        selected = formula.modeEnum == mode,
                        onClick = {
                            onChange(formula.copy(mode = mode.key))
                        },
                        label = { Text(mode.label) },
                    )
                }
            }

            when (formula.modeEnum) {
                CustomRankingFormulaMode.Preset -> {
                    Spacer(Modifier.height(8.dp))
                    BoardMenu(
                        board = formula.board,
                        onSelect = { onChange(formula.copy(board = it)) },
                    )
                }

                CustomRankingFormulaMode.Linear -> {
                    Spacer(Modifier.height(8.dp))
                    CUSTOM_RANKING_STAT_KEYS.chunked(2).forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowKeys.forEach { key ->
                                WeightField(
                                    label = CUSTOM_RANKING_STAT_LABELS[key] ?: key,
                                    value = formula.weights[key] ?: 0.0,
                                    onChange = { value ->
                                        onChange(
                                            formula.copy(
                                                weights = formula.weights.toMutableMap()
                                                    .apply { put(key, value) },
                                            ),
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowKeys.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }

                CustomRankingFormulaMode.Expression -> {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = formula.expression,
                        onValueChange = { onChange(formula.copy(expression = it)) },
                        label = { Text("表达式") },
                        placeholder = { Text("例：view * 0.3 + favorite * 20") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "可用字段：${CUSTOM_RANKING_STAT_KEYS.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DebouncedFilterTextField(
                    label = "起始日期",
                    value = formula.oldDate,
                    placeholder = "2024-01-01（留空为累计）",
                    onCommit = { onChange(formula.copy(oldDate = it)) },
                    modifier = Modifier.weight(1f),
                )
                DebouncedFilterTextField(
                    label = "截止日期",
                    value = formula.newDate,
                    placeholder = "2024-12-31（留空为最新）",
                    onCommit = { onChange(formula.copy(newDate = it)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BoardMenu(
    board: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = CUSTOM_RANKING_PRESET_BOARDS.firstOrNull { it.first == board }?.second ?: board
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("预设榜单：$label")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CUSTOM_RANKING_PRESET_BOARDS.forEach { (key, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        expanded = false
                        onSelect(key)
                    },
                )
            }
        }
    }
}

@Composable
private fun WeightField(
    label: String,
    value: Double,
    onChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(value) { mutableStateOf(trimNumber(value)) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input
            input.toDoubleOrNull()?.let(onChange)
        },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun PlaylistPickerDialog(
    playlists: List<PublicPlaylistDto>,
    loading: Boolean,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (PublicPlaylistDto) -> Unit,
) {
    var keyword by remember { mutableStateOf("") }
    LaunchedEffect(keyword) {
        delay(400)
        onSearch(keyword.trim())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择歌单") },
        text = {
            Column {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("搜索公开歌单") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (playlists.isEmpty() && !loading) {
                    EmptyHint(text = "没有找到歌单")
                }
                playlists.forEach { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(playlist) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NetworkImage(
                            url = playlist.coverVideo?.thumbnail,
                            contentDescription = playlist.name,
                            modifier = Modifier
                                .width(52.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${playlist.songCount} 首",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
fun CustomRankingResultRow(
    entry: CustomRankingEntryDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(text = entry.rank.toString())
                Spacer(Modifier.width(10.dp))
                NetworkImage(
                    url = entry.video?.thumbnail ?: entry.song.thumbnail,
                    contentDescription = songDisplayName(entry.song),
                    modifier = Modifier
                        .width(84.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = songDisplayName(entry.song),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = entry.song.artistLine(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.CHINA, "%.2f", entry.point),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "得分",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            MetricGrid(
                items = CUSTOM_RANKING_STAT_KEYS.map { key ->
                    MetricItem(
                        label = CUSTOM_RANKING_STAT_LABELS[key] ?: key,
                        value = formatNumber(entry.statValue(key)),
                        subtitle = "#${entry.statRank(key)}",
                    )
                },
                columns = 4,
            )
        }
    }
}

private fun trimNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else value.toString()
