package xyz.lisfox.vocaland.ui.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.collection.CollectionHeader
import xyz.lisfox.vocaland.data.collection.CollectionRepository
import xyz.lisfox.vocaland.data.collection.CollectionTarget
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.ui.components.InlineLoading
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.PagerRow
import xyz.lisfox.vocaland.ui.components.ScreenMessage

@Composable
fun CollectionDetailScreen(
    repository: CollectionRepository,
    target: CollectionTarget,
    onOpenSong: (Int) -> Unit,
    onOpenArtist: (ArtistKind, Int) -> Unit,
    modifier: Modifier = Modifier,
    onShare: ((CollectionHeader) -> Unit)? = null,
) {
    val targetKey = target.key()
    val viewModel: CollectionDetailViewModel = viewModel(
        key = "collection-$targetKey",
        factory = viewModelFactory {
            initializer { CollectionDetailViewModel(repository, target) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.loading && state.header == null -> LoadingPane(modifier = modifier)

        state.header == null -> {
            Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                ScreenMessage(
                    title = "加载失败",
                    message = state.error ?: "收藏册可能不存在",
                    actionLabel = "重试",
                    onAction = { viewModel.load() },
                )
            }
        }

        else -> {
            val header = state.header ?: return
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 24.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CollectionDetailHeader(
                        header = header,
                        onShare = onShare,
                    )
                }

                if (state.loading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        InlineLoading()
                    }
                }

                if (state.error != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ScreenMessage(
                            title = "加载失败",
                            message = state.error ?: "请稍后重试",
                            actionLabel = "重试",
                            onAction = { viewModel.load() },
                        )
                    }
                }

                if (state.entries.isEmpty() && !state.loading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyHint("这个收藏册还没有内容")
                    }
                } else {
                    items(
                        items = state.entries,
                        key = { entry -> "entry-${entry.entityId}" },
                    ) { entry ->
                        CollectionEntryTile(
                            entry = entry,
                            entityType = header.entityType,
                            onOpenSong = onOpenSong,
                            onOpenArtist = onOpenArtist,
                        )
                    }
                }

                if (state.totalPages > 1) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        PagerRow(
                            page = state.page,
                            totalPages = state.totalPages,
                            total = state.total,
                            loading = state.loading,
                            onPrevious = viewModel::previousPage,
                            onNext = viewModel::nextPage,
                            unit = "项",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionDetailHeader(
    header: CollectionHeader,
    onShare: ((CollectionHeader) -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CollectionCover(
                    coverPicture = header.coverPicture,
                    modifier = Modifier.size(88.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = header.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${entityTypeLabel(header.entityType)} · " +
                            "${header.entityCount} 项",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                onShare?.let { share ->
                    TextButton(onClick = { share(header) }) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(4.dp))
                        Text("分享")
                    }
                }
            }

            header.description
                ?.takeIf { it.isNotBlank() }
                ?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            CollectionProgress(header = header)
        }
    }
}

private fun CollectionTarget.key(): String = when (this) {
    is CollectionTarget.Manual -> "manual-$id"
    is CollectionTarget.Derived -> "derived-${kind.key}-$sourceId-${entityType.key}"
}
