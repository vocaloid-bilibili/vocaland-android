package xyz.lisfox.vocaland.ui.collection

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import xyz.lisfox.vocaland.data.collection.CollectionDto
import xyz.lisfox.vocaland.data.collection.CollectionHeader
import xyz.lisfox.vocaland.data.collection.CollectionRepository
import xyz.lisfox.vocaland.data.collection.CollectionTarget
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.InlineLoading
import xyz.lisfox.vocaland.ui.components.ScreenMessage

@Composable
fun CollectionListScreen(
    repository: CollectionRepository,
    onOpenCollection: (CollectionTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CollectionListViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CollectionListViewModel(repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "收藏册",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "按册点亮歌曲与艺人",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = viewModel::load, enabled = !state.loading) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text("刷新")
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.loading && state.items.isEmpty() -> LoadingPane()

                state.error != null && state.items.isEmpty() -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        ScreenMessage(
                            title = "加载失败",
                            message = state.error ?: "请稍后重试",
                            actionLabel = "重试",
                            onAction = viewModel::load,
                        )
                    }
                }

                state.items.isEmpty() -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyHint("暂无收藏册")
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (state.loading) {
                            item {
                                InlineLoading()
                            }
                        }

                        if (state.error != null) {
                            item {
                                ScreenMessage(
                                    title = "部分数据加载失败",
                                    message = state.error ?: "请稍后重试",
                                    actionLabel = "重试",
                                    onAction = viewModel::load,
                                )
                            }
                        }

                        items(state.items, key = { it.id }) { collection ->
                            CollectionCard(
                                collection = collection,
                                onOpen = {
                                    onOpenCollection(CollectionTarget.Manual(collection.id))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionCard(
    collection: CollectionDto,
    onOpen: () -> Unit,
) {
    val header = CollectionHeader(
        name = collection.name,
        description = collection.description,
        entityType = collection.entityType,
        coverPicture = collection.coverPicture,
        entityCount = collection.entityCount,
        litCount = collection.litCount,
        progress = collection.progress,
        manualId = collection.id,
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CollectionCover(
                coverPicture = collection.coverPicture,
                modifier = Modifier.size(88.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                collection.description
                    ?.takeIf { it.isNotBlank() }
                    ?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                Text(
                    text = "${entityTypeLabel(collection.entityType)} · " +
                        "${collection.entityCount} 项",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CollectionProgress(header = header)
            }
        }
    }
}
