package xyz.lisfox.vocaland.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.SEARCH_TARGET_OPTIONS
import xyz.lisfox.vocaland.data.model.SearchEntityDto
import xyz.lisfox.vocaland.data.model.SearchGroupDto
import xyz.lisfox.vocaland.data.model.searchTargetLabel
import xyz.lisfox.vocaland.data.repository.SearchRepository
import xyz.lisfox.vocaland.ui.components.InlineLoading
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.PagerRow
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SongCoverCard
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.formatNumber
import xyz.lisfox.vocaland.util.songArtistLine
import xyz.lisfox.vocaland.util.songDisplayName

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    repository: SearchRepository,
    onOpenSong: (Int) -> Unit,
    onOpenArtist: (ArtistKind, Int) -> Unit,
    onOpenTag: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SearchViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SearchViewModel(repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val submitSearch: () -> Unit = { viewModel.submit() }

    // 提交搜索后收起键盘：放在 effect 里执行，避免与输入法的按键分发互相覆盖
    LaunchedEffect(state.searched) {
        if (state.searched) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                            submitSearch()
                            true
                        } else {
                            false
                        }
                    },
                placeholder = { Text("搜索歌曲、P主、歌手…") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(Icons.Filled.Close, contentDescription = "清空")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { submitSearch() },
                ),
            )
            FilledIconButton(
                onClick = submitSearch,
                enabled = state.query.isNotBlank(),
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "搜索",
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SEARCH_TARGET_OPTIONS, key = { it.key ?: "all" }) { option ->
                FilterChip(
                    selected = state.target == option.key,
                    onClick = { viewModel.selectTarget(option.key) },
                    label = { Text(option.label) },
                )
            }
        }

        when {
            !state.searched -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    EmptyHint("输入关键词搜索歌曲、P主、歌手与标签")
                }
            }

            state.loading && state.groups.isEmpty() -> {
                LoadingPane(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    text = "正在搜索…",
                )
            }

            state.error != null && state.groups.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                ) {
                    ScreenMessage(
                        title = "搜索失败",
                        message = state.error ?: "请检查网络后重试",
                        actionLabel = "重试",
                        onAction = viewModel::retry,
                    )
                }
            }

            else -> {
                val groups = remember(state.groups) {
                    state.groups.filter { it.data.isNotEmpty() }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (state.loading) {
                        item {
                            InlineLoading()
                        }
                    }
                    if (state.error != null) {
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
                    if (groups.isEmpty()) {
                        item {
                            EmptyHint("没有找到与「${state.submittedQuery}」相关的内容")
                        }
                    }
                    groups.forEach { group ->
                        item(key = "header-${group.type}") {
                            SearchGroupHeader(
                                group = group,
                                showAllAction = state.target == null,
                                onShowAll = { viewModel.selectTarget(group.type) },
                            )
                        }
                        if (group.type == "song") {
                            itemsIndexed(
                                items = group.data.chunked(2),
                                key = { _, row ->
                                    "song-row-${group.type}-" + row.joinToString("-") { entity ->
                                        entity.bvid.ifBlank { entity.id.toString() }
                                    }
                                },
                                contentType = { _, _ -> "song-row" },
                            ) { _, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    row.forEach { entity ->
                                        val title = entity.songDisplayName()
                                        SongCoverCard(
                                            title = title,
                                            subtitle = entity.songArtistLine(),
                                            thumbnail = entity.coverUrl(),
                                            onClick = {
                                                (entity.song?.id ?: entity.id)
                                                    .takeIf { it > 0 }
                                                    ?.let(onOpenSong)
                                            },
                                            modifier = Modifier.weight(1f),
                                            sharedKey = "song-cover-${entity.song?.id ?: entity.id}",
                                        )
                                    }
                                    if (row.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            items(
                                items = group.data,
                                key = { entity ->
                                    "${group.type}-${entity.bvid.ifBlank { entity.id.toString() }}"
                                },
                                contentType = { "entity-row" },
                            ) { entity ->
                                SearchEntityRow(
                                    type = group.type,
                                    entity = entity,
                                    modifier = Modifier.animateItem(),
                                    onClick = {
                                        when (group.type) {
                                            "video" -> {
                                                (entity.song?.id ?: entity.songId)
                                                    .takeIf { it > 0 }
                                                    ?.let(onOpenSong)
                                            }

                                            "uploader" -> {
                                                onOpenArtist(ArtistKind.Uploader, entity.id)
                                            }

                                            "producer", "vocalist", "synthesizer" -> {
                                                ArtistKind.fromApiType(group.type)
                                                    ?.let { onOpenArtist(it, entity.id) }
                                            }

                                            "tag" -> {
                                                onOpenTag(entity.id)
                                            }

                                            "song" -> {
                                                (entity.song?.id ?: entity.id)
                                                    .takeIf { it > 0 }
                                                    ?.let(onOpenSong)
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                    if (state.target != null && state.totalPages > 1) {
                        item {
                            SearchPagination(
                                page = state.page,
                                totalPages = state.totalPages,
                                total = state.total,
                                onPrevious = viewModel::previousPage,
                                onNext = viewModel::nextPage,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchGroupHeader(
    group: SearchGroupDto,
    showAllAction: Boolean,
    onShowAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = searchTargetLabel(group.type),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${group.total} 条",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        if (showAllAction && group.total > group.data.size) {
            TextButton(onClick = onShowAll) {
                Text("查看全部")
            }
        }
    }
}

@Composable
private fun SearchEntityRow(
    type: String,
    entity: SearchEntityDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVideo = type == "video"
    val title = entityTitle(type, entity)
    val subtitle = entitySubtitle(type, entity)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isVideo) {
                NetworkImage(
                    url = entity.thumbnail
                        ?: entity.song?.thumbnail
                        ?: entity.videos.firstOrNull()?.thumbnail,
                    contentDescription = title,
                    modifier = Modifier
                        .width(104.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp)),
                )
            } else {
                NetworkImage(
                    url = entity.picture ?: entity.thumbnail,
                    contentDescription = title,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(if (type == "tag") RoundedCornerShape(12.dp) else CircleShape),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchPagination(
    page: Int,
    totalPages: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    PagerRow(
        page = page,
        totalPages = totalPages,
        loading = false,
        onPrevious = onPrevious,
        onNext = onNext,
        total = total,
        unit = "条",
    )
}

private fun entityTitle(type: String, entity: SearchEntityDto): String = when (type) {
    "video" -> entity.title?.trim()?.takeIf { it.isNotEmpty() }
        ?: entity.song?.let { songDisplayName(it) }
        ?: entity.name.ifBlank { "未命名视频" }

    "tag" -> entity.name.ifBlank { "未知标签" }

    else -> entity.displayName?.trim()?.takeIf { it.isNotEmpty() }
        ?: entity.name.ifBlank { "未知条目" }
}

private fun entitySubtitle(type: String, entity: SearchEntityDto): String = when (type) {
    "video" -> listOfNotNull(
        entity.uploader?.name?.takeIf { it.isNotBlank() },
        entity.latestSnapshot?.view?.takeIf { it > 0 }?.let { "${formatNumber(it)} 播放" },
    ).joinToString(" · ").ifBlank { "视频" }

    "tag" -> entity.description?.trim()?.takeIf { it.isNotEmpty() }
        ?: "${entity.associationCount} 个关联条目"

    else -> listOfNotNull(
        searchTargetLabel(type),
        entity.abbreviation?.trim()?.takeIf { it.isNotEmpty() },
    ).joinToString(" · ")
}
