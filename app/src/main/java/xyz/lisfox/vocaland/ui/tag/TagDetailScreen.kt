package xyz.lisfox.vocaland.ui.tag

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import xyz.lisfox.vocaland.data.discovery.TagArtistPage
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.repository.TagDetailRepository
import xyz.lisfox.vocaland.ui.components.InlineLoading
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.PagerRow
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.ui.components.songCoverGrid

@Composable
fun TagDetailScreen(
    repository: TagDetailRepository,
    tagId: Int,
    onOpenSong: (Int) -> Unit,
    onOpenArtist: (ArtistKind, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    communitySlot: (@Composable () -> Unit)? = null,
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
                        TagHero(tag = tag, total = state.hotTotal)
                    }
                }

                state.artists.forEach { (kind, pageData) ->
                    if (pageData.artists.isNotEmpty()) {
                        item(key = "artist-header-${kind.apiType}") {
                            SectionHeader(
                                title = "${kind.label}（${pageData.total}）",
                            )
                        }
                        items(
                            items = pageData.artists.chunked(2),
                            key = { row ->
                                "artist-${kind.apiType}-${row.joinToString("-") { it.id.toString() }}"
                            },
                        ) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                row.forEach { artist ->
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                onOpenArtist(kind, artist.id)
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            NetworkImage(
                                                url = artist.picture,
                                                contentDescription = artist.name,
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape),
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                                if (row.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        item(key = "artist-pager-${kind.apiType}") {
                            ArtistPager(
                                pageData = pageData,
                                page = state.artistPages[kind] ?: 1,
                                loading = state.artistLoading == kind,
                                error = if (state.artistErrorKind == kind) {
                                    state.artistError
                                } else {
                                    null
                                },
                                onPrevious = { viewModel.previousArtistPage(kind) },
                                onNext = { viewModel.nextArtistPage(kind) },
                                onRetry = { viewModel.retryArtist(kind) },
                            )
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = "最热歌曲",
                        subtitle = "共 ${state.hotTotal} 首",
                    )
                }
                if (state.hotSongs.isEmpty()) {
                    item {
                        EmptyHint("暂无歌曲")
                    }
                } else {
                    songCoverGrid(
                        songs = state.hotSongs,
                        keyPrefix = "tag-$tagId-hot",
                        onOpenSong = onOpenSong,
                    )
                }

                item {
                    SectionHeader(
                        title = "最新歌曲",
                        subtitle = "共 ${state.latestTotal} 首",
                    )
                }
                if (state.latestSongs.isEmpty()) {
                    item {
                        EmptyHint("暂无歌曲")
                    }
                } else {
                    songCoverGrid(
                        songs = state.latestSongs,
                        keyPrefix = "tag-$tagId-latest",
                        onOpenSong = onOpenSong,
                    )
                }

                communitySlot?.let { slot ->
                    item { slot() }
                }
            }
        }
    }
}

@Composable
private fun ArtistPager(
    pageData: TagArtistPage,
    page: Int,
    loading: Boolean,
    error: String?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
) {
    Column {
        if (loading) {
            InlineLoading()
        }
        if (error != null) {
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
                TextButton(onClick = onRetry) {
                    Text("重试")
                }
            }
        }
        if (pageData.totalPages > 1) {
            PagerRow(
                page = page,
                totalPages = pageData.totalPages,
                total = pageData.total,
                loading = loading,
                onPrevious = onPrevious,
                onNext = onNext,
                unit = "位",
            )
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

internal fun tagCategoryLabel(category: String): String = when (category) {
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
