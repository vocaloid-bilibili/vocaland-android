package xyz.lisfox.vocaland.ui.tag

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.discovery.TagIndexRepository
import xyz.lisfox.vocaland.ui.components.InlineLoading
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.PagerRow
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.ui.components.StatusPill

private val scopeOptions = listOf(
    null to "全部",
    "song" to "歌曲",
    "vocalist" to "歌手",
    "producer" to "P主",
    "synthesizer" to "合成器",
    "uploader" to "UP主",
)

@Composable
fun TagIndexScreen(
    repository: TagIndexRepository,
    onOpenTag: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TagIndexViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TagIndexViewModel(repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "标签图鉴",
                subtitle = if (state.filtered) {
                    "共 ${state.total} 个标签"
                } else {
                    "共 ${state.totalTags} 个标签"
                },
            )
        }

        item {
            OutlinedTextField(
                value = state.search,
                onValueChange = viewModel::setSearch,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索标签") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (state.search.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearch("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "清空")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(scopeOptions, key = { it.first ?: "all" }) { (scope, label) ->
                    FilterChip(
                        selected = state.scope == scope,
                        onClick = { viewModel.setScope(scope) },
                        label = { Text(label) },
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "category-all") {
                    FilterChip(
                        selected = state.category == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text("全部分类") },
                    )
                }
                items(state.categories, key = { it.category }) { category ->
                    FilterChip(
                        selected = state.category == category.category,
                        onClick = { viewModel.setCategory(category.category) },
                        label = {
                            Text("${tagCategoryLabel(category.category)} ${category.count}")
                        },
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.sort == "count",
                        onClick = { viewModel.setSort("count") },
                        label = { Text("按热度") },
                    )
                    FilterChip(
                        selected = state.sort == "id",
                        onClick = { viewModel.setSort("id") },
                        label = { Text("最新") },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.filtered) {
                        TextButton(onClick = viewModel::clearFilters) {
                            Text("清除筛选")
                        }
                    }
                    TextButton(
                        onClick = {
                            viewModel.randomTag()?.let { onOpenTag(it.id) }
                        },
                    ) {
                        Text("随机逛")
                    }
                }
            }
        }

        if (state.loading) {
            item {
                InlineLoading()
            }
        }

        if (state.error != null && state.tags.isEmpty() && state.groups.isEmpty()) {
            item {
                ScreenMessage(
                    title = "标签加载失败",
                    message = state.error ?: "请稍后重试",
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

        if (state.filtered) {
            if (state.tags.isEmpty() && !state.loading) {
                item {
                    EmptyHint("没有匹配的标签，换个关键词或放宽筛选试试")
                }
            } else {
                item {
                    SectionHeader(
                        title = "筛选结果",
                        subtitle = "共 ${state.total} 个标签",
                    )
                }
                items(state.tags.chunked(2), key = { row ->
                    "flat-${row.joinToString("-") { it.id.toString() }}"
                }) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { tag ->
                            TagTile(
                                tag = tag,
                                onClick = { onOpenTag(tag.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                if (state.totalPages > 1) {
                    item {
                        PagerRow(
                            page = state.page,
                            totalPages = state.totalPages,
                            total = state.total,
                            loading = state.loading,
                            onPrevious = viewModel::previousPage,
                            onNext = viewModel::nextPage,
                            unit = "个标签",
                        )
                    }
                }
            }
        } else if (state.groups.isEmpty() && !state.loading) {
            item {
                EmptyHint("暂无标签")
            }
        } else {
            state.groups.forEach { group ->
                item(key = "group-${group.category}") {
                    SectionHeader(
                        title = group.category,
                        subtitle = "共 ${group.total} 个",
                    )
                }
                items(
                    items = group.tags.chunked(2),
                    key = { row ->
                        "wall-${group.category}-${row.joinToString("-") { it.id.toString() }}"
                    },
                ) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { tag ->
                            TagTile(
                                tag = tag,
                                onClick = { onOpenTag(tag.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagTile(
    tag: TagItemDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NetworkImage(
                url = tag.picture,
                contentDescription = tag.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tag.category.isNotBlank()) {
                        StatusPill(tagCategoryLabel(tag.category))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = "${tag.associationCount} 个关联",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
