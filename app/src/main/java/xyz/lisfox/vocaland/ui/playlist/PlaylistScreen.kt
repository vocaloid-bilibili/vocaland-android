package xyz.lisfox.vocaland.ui.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.repository.PlaylistRepository
import xyz.lisfox.vocaland.ui.components.InlineLoading
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.PagerRow
import xyz.lisfox.vocaland.ui.components.ScreenMessage

private val PLAYLIST_SORTS = listOf(
    "hot" to "热门",
    "new" to "最新",
    "updated" to "更新",
)

@Composable
fun PlaylistScreen(
    repository: PlaylistRepository,
    onOpenPlaylist: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onOpenMyPlaylists: (() -> Unit)? = null,
) {
    val viewModel: PlaylistViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PlaylistViewModel(repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        onOpenMyPlaylists?.let { open ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = open) {
                    Text("我的歌单")
                }
            }
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("搜索歌单") },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Search, contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = viewModel::submitSearch) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "搜索",
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.submitSearch() }),
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(PLAYLIST_SORTS, key = { it.first }) { (key, label) ->
                FilterChip(
                    selected = state.sort == key,
                    onClick = { viewModel.setSort(key) },
                    label = { Text(label) },
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when {
                state.loading && state.items.isEmpty() -> {
                    LoadingPane()
                }

                state.error != null && state.items.isEmpty() -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        ScreenMessage(
                            title = "歌单加载失败",
                            message = state.error ?: "请稍后重试",
                            actionLabel = "重试",
                            onAction = viewModel::refresh,
                        )
                    }
                }

                state.items.isEmpty() -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyHint("没有找到歌单")
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (state.loading) {
                            item {
                                InlineLoading()
                            }
                        }
                        items(state.items, key = { it.id }) { playlist ->
                            PlaylistListCard(
                                playlist = playlist,
                                onClick = { onOpenPlaylist(playlist.id) },
                            )
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
                                    unit = "个",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
