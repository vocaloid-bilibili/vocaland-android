package xyz.lisfox.vocaland.ui.stat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import xyz.lisfox.vocaland.data.model.SearchEntityDto
import xyz.lisfox.vocaland.data.stat.StatArtistRef
import xyz.lisfox.vocaland.data.stat.StatFilterState
import xyz.lisfox.vocaland.data.stat.StatRange
import xyz.lisfox.vocaland.data.stat.StatVocalistRule
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.util.songDisplayName

private data class RangePreset(val label: String, val min: Long? = null, val max: Long? = null)

private val VIEW_PRESETS = listOf(
    RangePreset("1000万+", min = 10_000_000),
    RangePreset("100万+", min = 1_000_000),
    RangePreset("10万+", min = 100_000),
    RangePreset("1万+", min = 10_000),
    RangePreset("1万以下", max = 10_000),
)

private val STAT_PRESETS = listOf(
    RangePreset("100万+", min = 1_000_000),
    RangePreset("10万+", min = 100_000),
    RangePreset("1万+", min = 10_000),
    RangePreset("1000+", min = 1_000),
    RangePreset("1000以下", max = 1_000),
)

private val COUNT_PRESETS = listOf(
    RangePreset("100+", min = 100),
    RangePreset("50+", min = 50),
    RangePreset("20+", min = 20),
    RangePreset("10+", min = 10),
    RangePreset("1+", min = 1),
)

private val SONG_TYPES = listOf("原创", "翻唱", "本家重置", "串烧")
private val COPYRIGHT_OPTIONS = listOf(
    1 to "自制",
    2 to "转载",
    3 to "自制投未转",
    4 to "转载投未转",
    100 to "自制投转载",
    101 to "转载投自制",
)

/**
 * 统计页 / 自定义排行榜共用的筛选面板（受控组件）。
 * 数据维度与 website SongFilterPanel 对齐；歌手 / P主 / 引擎 / UP主 / 标签 /
 * 原曲 / 衍生曲通过 [onSearchEntities] 搜索选择（为 null 时隐藏艺人分组）。
 */
@Composable
fun StatFilterPanel(
    filter: StatFilterState,
    total: Int,
    enabled: Boolean,
    onFilterChange: (StatFilterState) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    onSearchEntities: (suspend (type: String, keyword: String) -> List<SearchEntityDto>)? = null,
    onLogin: () -> Unit = {},
    onOpenSponsor: (() -> Unit)? = null,
) {
    if (!enabled) {
        LockedFilterCard(
            modifier = modifier,
            onLogin = onLogin,
            onOpenSponsor = onOpenSponsor,
        )
        return
    }

    val active = filter.activeCount()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "筛选条件",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (active > 0) {
                    TextButton(onClick = onReset) {
                        Text("重置（$active）")
                    }
                }
                Text(
                    text = "共 $total 条",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FilterSection(title = "数据维度") {
                RangeFilterButton(
                    label = "播放",
                    ranges = filter.viewRanges,
                    presets = VIEW_PRESETS,
                    onChange = { onFilterChange(filter.copy(viewRanges = it)) },
                )
                RangeFilterButton(
                    label = "收藏",
                    ranges = filter.favoriteRanges,
                    presets = STAT_PRESETS,
                    onChange = { onFilterChange(filter.copy(favoriteRanges = it)) },
                )
                RangeFilterButton(
                    label = "硬币",
                    ranges = filter.coinRanges,
                    presets = STAT_PRESETS,
                    onChange = { onFilterChange(filter.copy(coinRanges = it)) },
                )
                RangeFilterButton(
                    label = "点赞",
                    ranges = filter.likeRanges,
                    presets = STAT_PRESETS,
                    onChange = { onFilterChange(filter.copy(likeRanges = it)) },
                )
                RangeFilterButton(
                    label = "弹幕",
                    ranges = filter.danmakuRanges,
                    presets = STAT_PRESETS,
                    onChange = { onFilterChange(filter.copy(danmakuRanges = it)) },
                )
                RangeFilterButton(
                    label = "评论",
                    ranges = filter.replyRanges,
                    presets = STAT_PRESETS,
                    onChange = { onFilterChange(filter.copy(replyRanges = it)) },
                )
                RangeFilterButton(
                    label = "分享",
                    ranges = filter.shareRanges,
                    presets = STAT_PRESETS,
                    onChange = { onFilterChange(filter.copy(shareRanges = it)) },
                )
                RangeFilterButton(
                    label = "日刊上榜",
                    ranges = filter.dailyCountRanges,
                    presets = COUNT_PRESETS,
                    onChange = { onFilterChange(filter.copy(dailyCountRanges = it)) },
                )
                RangeFilterButton(
                    label = "周刊上榜",
                    ranges = filter.weeklyCountRanges,
                    presets = COUNT_PRESETS,
                    onChange = { onFilterChange(filter.copy(weeklyCountRanges = it)) },
                )
            }

            RankRow(
                dailyRank = filter.dailyRank,
                weeklyRank = filter.weeklyRank,
                onDailyChange = { onFilterChange(filter.copy(dailyRank = it)) },
                onWeeklyChange = { onFilterChange(filter.copy(weeklyRank = it)) },
            )

            DebouncedFilterTextField(
                label = "发布日期",
                value = filter.pubdate,
                placeholder = "2024 / 2024.3 / 2024~2025",
                onCommit = { onFilterChange(filter.copy(pubdate = it)) },
            )

            if (onSearchEntities != null) {
                FilterColumn(title = "艺人维度") {
                    ArtistFilterField(
                        label = "歌手",
                        selected = filter.vocalists.map { it.vocalist },
                        onAdd = { artist ->
                            onFilterChange(
                                filter.copy(
                                    vocalists = filter.vocalists + StatVocalistRule(
                                        vocalist = artist,
                                    ),
                                ),
                            )
                        },
                        onRemove = { id ->
                            onFilterChange(
                                filter.copy(
                                    vocalists = filter.vocalists.filterNot {
                                        it.vocalist.id == id
                                    },
                                ),
                            )
                        },
                        onCycleMode = { id ->
                            onFilterChange(
                                filter.copy(
                                    vocalists = filter.vocalists.map {
                                        if (it.vocalist.id == id) {
                                            val mode = StatArtistRef.nextMode(it.mode)
                                            it.copy(
                                                mode = mode,
                                                vocalist = it.vocalist.copy(mode = mode),
                                            )
                                        } else {
                                            it
                                        }
                                    },
                                ),
                            )
                        },
                        searchType = "vocalist",
                        onSearchEntities = onSearchEntities,
                    )
                    ArtistFilterField(
                        label = "P主",
                        selected = filter.producers,
                        onAdd = { onFilterChange(filter.copy(producers = filter.producers + it)) },
                        onRemove = { id ->
                            onFilterChange(filter.copy(producers = filter.producers.filterNot { it.id == id }))
                        },
                        onCycleMode = { id ->
                            onFilterChange(
                                filter.copy(
                                    producers = filter.producers.map {
                                        if (it.id == id) it.copy(mode = StatArtistRef.nextMode(it.mode)) else it
                                    },
                                ),
                            )
                        },
                        searchType = "producer",
                        onSearchEntities = onSearchEntities,
                    )
                    ArtistFilterField(
                        label = "引擎",
                        selected = filter.synthesizers,
                        onAdd = { onFilterChange(filter.copy(synthesizers = filter.synthesizers + it)) },
                        onRemove = { id ->
                            onFilterChange(filter.copy(synthesizers = filter.synthesizers.filterNot { it.id == id }))
                        },
                        onCycleMode = { id ->
                            onFilterChange(
                                filter.copy(
                                    synthesizers = filter.synthesizers.map {
                                        if (it.id == id) it.copy(mode = StatArtistRef.nextMode(it.mode)) else it
                                    },
                                ),
                            )
                        },
                        searchType = "synthesizer",
                        onSearchEntities = onSearchEntities,
                    )
                    ArtistFilterField(
                        label = "UP主",
                        selected = filter.uploaders,
                        onAdd = { onFilterChange(filter.copy(uploaders = filter.uploaders + it)) },
                        onRemove = { id ->
                            onFilterChange(filter.copy(uploaders = filter.uploaders.filterNot { it.id == id }))
                        },
                        onCycleMode = { id ->
                            onFilterChange(
                                filter.copy(
                                    uploaders = filter.uploaders.map {
                                        if (it.id == id) it.copy(mode = StatArtistRef.nextMode(it.mode)) else it
                                    },
                                ),
                            )
                        },
                        searchType = "uploader",
                        onSearchEntities = onSearchEntities,
                    )
                    ArtistFilterField(
                        label = "标签",
                        selected = filter.tags,
                        onAdd = { onFilterChange(filter.copy(tags = filter.tags + it)) },
                        onRemove = { id ->
                            onFilterChange(filter.copy(tags = filter.tags.filterNot { it.id == id }))
                        },
                        onCycleMode = { id ->
                            onFilterChange(
                                filter.copy(
                                    tags = filter.tags.map {
                                        if (it.id == id) it.copy(mode = StatArtistRef.nextMode(it.mode)) else it
                                    },
                                ),
                            )
                        },
                        searchType = "tag",
                        onSearchEntities = onSearchEntities,
                    )
                    ArtistFilterField(
                        label = "原曲（查衍生）",
                        selected = filter.originalSongs,
                        onAdd = { onFilterChange(filter.copy(originalSongs = filter.originalSongs + it)) },
                        onRemove = { id ->
                            onFilterChange(filter.copy(originalSongs = filter.originalSongs.filterNot { it.id == id }))
                        },
                        onCycleMode = { id ->
                            onFilterChange(
                                filter.copy(
                                    originalSongs = filter.originalSongs.map {
                                        if (it.id == id) it.copy(mode = StatArtistRef.nextMode(it.mode)) else it
                                    },
                                ),
                            )
                        },
                        searchType = "song",
                        onSearchEntities = onSearchEntities,
                    )
                    ArtistFilterField(
                        label = "衍生曲（查原曲）",
                        selected = filter.derivativeSongs,
                        onAdd = { onFilterChange(filter.copy(derivativeSongs = filter.derivativeSongs + it)) },
                        onRemove = { id ->
                            onFilterChange(filter.copy(derivativeSongs = filter.derivativeSongs.filterNot { it.id == id }))
                        },
                        onCycleMode = { id ->
                            onFilterChange(
                                filter.copy(
                                    derivativeSongs = filter.derivativeSongs.map {
                                        if (it.id == id) it.copy(mode = StatArtistRef.nextMode(it.mode)) else it
                                    },
                                ),
                            )
                        },
                        searchType = "song",
                        onSearchEntities = onSearchEntities,
                    )
                }
            }

            FilterSection(title = "类别") {
                SONG_TYPES.forEach { type ->
                    val checked = type in filter.types
                    FilterChip(
                        selected = checked,
                        onClick = {
                            onFilterChange(
                                filter.copy(
                                    types = if (checked) {
                                        filter.types - type
                                    } else {
                                        filter.types + type
                                    },
                                ),
                            )
                        },
                        label = { Text(type) },
                    )
                }
                COPYRIGHT_OPTIONS.forEach { (value, label) ->
                    val checked = value in filter.copyright
                    FilterChip(
                        selected = checked,
                        onClick = {
                            onFilterChange(
                                filter.copy(
                                    copyright = if (checked) {
                                        filter.copyright - value
                                    } else {
                                        filter.copyright + value
                                    },
                                ),
                            )
                        },
                        label = { Text(label) },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "仅外语曲",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = filter.foreignOnly,
                        onCheckedChange = {
                            onFilterChange(filter.copy(foreignOnly = it))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LockedFilterCard(
    modifier: Modifier,
    onLogin: () -> Unit,
    onOpenSponsor: (() -> Unit)?,
) {
    ScreenMessage(
        modifier = modifier,
        title = "筛选需要赞助权限",
        message = "登录并赞助后可解锁完整筛选，按条件精准查找歌曲。",
        actionLabel = if (onOpenSponsor != null) "前往赞助" else "去登录",
        onAction = {
            if (onOpenSponsor != null) onOpenSponsor() else onLogin()
        },
    )
}

@Composable
private fun FilterColumn(
    title: String,
    content: @Composable () -> Unit,
) {
    Spacer(Modifier.height(10.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        content()
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Spacer(Modifier.height(10.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun RangeFilterButton(
    label: String,
    ranges: List<StatRange>,
    presets: List<RangePreset>,
    onChange: (List<StatRange>) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    FilterChip(
        selected = ranges.isNotEmpty(),
        onClick = { open = true },
        label = {
            Text(
                text = if (ranges.isEmpty()) label else {
                    "$label ${ranges.joinToString(", ") { it.label() }}"
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )

    if (open) {
        RangeFilterDialog(
            label = label,
            initial = ranges,
            presets = presets,
            onDismiss = { open = false },
            onConfirm = {
                onChange(it)
                open = false
            },
        )
    }
}

@Composable
private fun RangeFilterDialog(
    label: String,
    initial: List<StatRange>,
    presets: List<RangePreset>,
    onDismiss: () -> Unit,
    onConfirm: (List<StatRange>) -> Unit,
) {
    var ranges by remember(initial) { mutableStateOf(initial) }
    var minText by remember { mutableStateOf("") }
    var maxText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$label 范围") },
        text = {
            Column {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = ranges.isEmpty(),
                        onClick = { ranges = emptyList() },
                        label = { Text("不限") },
                    )
                    presets.forEach { preset ->
                        val active = ranges.size == 1 &&
                            ranges[0].min == preset.min &&
                            ranges[0].max == preset.max
                        FilterChip(
                            selected = active,
                            onClick = {
                                ranges = if (active) {
                                    emptyList()
                                } else {
                                    listOf(StatRange(min = preset.min, max = preset.max))
                                }
                            },
                            label = { Text(preset.label) },
                        )
                    }
                }
                if (ranges.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ranges.forEach { range ->
                            InputChip(
                                selected = true,
                                onClick = {
                                    ranges = ranges - range
                                },
                                label = { Text(range.label()) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = minText,
                        onValueChange = {
                            minText = it.filter(Char::isDigit)
                            error = null
                        },
                        label = { Text("最小") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    OutlinedTextField(
                        value = maxText,
                        onValueChange = {
                            maxText = it.filter(Char::isDigit)
                            error = null
                        },
                        label = { Text("最大") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(
                    onClick = {
                        val min = minText.toLongOrNull()
                        val max = maxText.toLongOrNull()
                        if (min == null && max == null) {
                            error = "请至少填写一个值"
                            return@TextButton
                        }
                        if (min != null && max != null && min > max) {
                            error = "最小值不能大于最大值"
                            return@TextButton
                        }
                        val next = StatRange(min = min, max = max)
                        if (ranges.size == 1 && presets.any {
                                it.min == ranges[0].min && it.max == ranges[0].max
                            }
                        ) {
                            ranges = listOf(next)
                        } else if (next !in ranges) {
                            ranges = ranges + next
                        }
                        minText = ""
                        maxText = ""
                    },
                ) {
                    Text("添加自定义范围")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(ranges) }) {
                Text("完成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun RankRow(
    dailyRank: String,
    weeklyRank: String,
    onDailyChange: (String) -> Unit,
    onWeeklyChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DebouncedFilterTextField(
            label = "日刊前 N 名",
            value = dailyRank,
            placeholder = "如 20",
            numeric = true,
            onCommit = onDailyChange,
            modifier = Modifier.weight(1f),
        )
        DebouncedFilterTextField(
            label = "周刊前 N 名",
            value = weeklyRank,
            placeholder = "如 20",
            numeric = true,
            onCommit = onWeeklyChange,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun DebouncedFilterTextField(
    label: String,
    value: String,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    numeric: Boolean = false,
) {
    var local by remember(value) { mutableStateOf(value) }
    LaunchedEffect(local) {
        delay(500)
        val normalized = if (numeric) local.filter(Char::isDigit) else local.trim()
        if (normalized != value) {
            onCommit(normalized)
        }
    }
    OutlinedTextField(
        value = local,
        onValueChange = {
            local = if (numeric) it.filter(Char::isDigit) else it
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ArtistFilterField(
    label: String,
    selected: List<StatArtistRef>,
    onAdd: (StatArtistRef) -> Unit,
    onRemove: (Int) -> Unit,
    onCycleMode: (Int) -> Unit,
    searchType: String,
    onSearchEntities: suspend (type: String, keyword: String) -> List<SearchEntityDto>,
) {
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf(emptyList<SearchEntityDto>()) }

    LaunchedEffect(query) {
        if (query.trim().isEmpty()) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        delay(400)
        searching = true
        results = runCatching { onSearchEntities(searchType, query.trim()) }
            .getOrDefault(emptyList())
        searching = false
    }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (selected.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                selected.forEach { artist ->
                    InputChip(
                        selected = true,
                        onClick = { onCycleMode(artist.id) },
                        label = {
                            Text("${artist.name} · ${StatArtistRef.modeLabel(artist.mode)}")
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "移除",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onRemove(artist.id) },
                            )
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text("搜索$label") },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (searching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (results.isNotEmpty()) {
            Column {
                results.take(6).forEach { entity ->
                    val title = when (searchType) {
                        "song" -> entity.song?.let { songDisplayName(it) }
                            ?: entity.displayName
                            ?: entity.name
                        else -> entity.displayName?.takeIf { it.isNotBlank() }
                            ?: entity.name
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(
                            onClick = {
                                onAdd(
                                    StatArtistRef(
                                        id = entity.id,
                                        name = title,
                                    ),
                                )
                                query = ""
                                results = emptyList()
                            },
                        ) {
                            Text("添加")
                        }
                    }
                }
            }
        } else if (query.isNotBlank() && !searching) {
            EmptyHint(text = "没有找到匹配项")
        }
    }
}
