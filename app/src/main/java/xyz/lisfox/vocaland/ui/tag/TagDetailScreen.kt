package xyz.lisfox.vocaland.ui.tag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
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
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.repository.TagDetailRepository
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.PagerRow
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.ui.components.songCoverGrid

private val tagSorts = listOf(
    "latest" to "最新",
    "hot" to "最热",
)

@Composable
fun TagDetailScreen(
    repository: TagDetailRepository,
    tagId: Int,
    onOpenSong: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TagDetailViewModel = viewModel(
        key = "tag-$tagId",
        factory = viewModelFactory {
            initializer { TagDetailViewModel(repository, tagId) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.loading && state.tag == null -> {
            LoadingPane(modifier = modifier)
        }

        state.tag == null -> {
            Box(modifier = modifier.padding(16.dp)) {
                ScreenMessage(
                    title = "标签加载失败",
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
                    state.tag?.let { tag ->
                        TagHero(tag = tag, total = state.total)
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(
                            title = "歌曲",
                            subtitle = "共 ${state.total} 首",
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            tagSorts.forEach { (key, label) ->
                                FilterChip(
                                    selected = state.sort == key,
                                    onClick = { viewModel.selectSort(key) },
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                }

                if (state.songsLoading) {
                    item {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                if (state.error != null && state.songs.isNotEmpty()) {
                    item {
                        ScreenMessage(
                            title = "歌曲加载失败",
                            message = state.error ?: "请稍后重试",
                            actionLabel = "重试",
                            onAction = viewModel::retry,
                        )
                    }
                }

                if (state.songs.isEmpty() && !state.songsLoading) {
                    item {
                        EmptyHint("暂无歌曲")
                    }
                } else {
                    songCoverGrid(
                        songs = state.songs,
                        keyPrefix = "tag-${tagId}-${state.sort}",
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
private fun TagHero(
    tag: TagItemDto,
    total: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NetworkImage(
            url = tag.picture,
            contentDescription = tag.name,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp)),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.headlineSmall,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tag.category
                    .takeIf { it.isNotBlank() }
                    ?.let { StatusPill(tagCategoryLabel(it)) }
                if (total > 0) {
                    StatusPill("$total 首")
                }
            }

            if (tag.description.isNotBlank()) {
                Text(
                    text = tag.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (tag.scope.isNotEmpty()) {
                Text(
                    text = tag.scope.joinToString(" / ") { tagScopeLabel(it) },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun tagCategoryLabel(category: String): String = when (category) {
    "content" -> "内容"
    "meta" -> "元数据"
    "audience" -> "受众"
    else -> category
}

private fun tagScopeLabel(scope: String): String = when (scope) {
    "song" -> "歌曲"
    "producer" -> "P主"
    "vocalist" -> "歌手"
    "synthesizer" -> "合成器"
    "uploader" -> "UP主"
    "all" -> "全部"
    else -> scope
}
