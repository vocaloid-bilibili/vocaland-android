package xyz.lisfox.vocaland.ui.song

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.model.VideoDto
import xyz.lisfox.vocaland.data.repository.SongDetailRepository
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.formatNumber
import xyz.lisfox.vocaland.util.formatShortDate
import xyz.lisfox.vocaland.util.openBilibiliUser
import xyz.lisfox.vocaland.util.openBilibiliVideo
import xyz.lisfox.vocaland.util.songDisplayName

@Composable
fun SongDetailScreen(
    repository: SongDetailRepository,
    songId: Int,
    onOpenArtist: (ArtistKind, Int) -> Unit,
    onOpenTag: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SongDetailViewModel = viewModel(
        key = "song-$songId",
        factory = viewModelFactory {
            initializer { SongDetailViewModel(repository, songId) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.loading && state.song == null -> {
            LoadingPane(modifier = modifier)
        }

        state.song == null -> {
            Box(modifier = modifier.padding(16.dp)) {
                ScreenMessage(
                    title = "歌曲加载失败",
                    message = state.error ?: "请稍后重试",
                    actionLabel = "重试",
                    onAction = viewModel::retry,
                )
            }
        }

        else -> {
            state.song?.let { song ->
                SongDetailContent(
                    song = song,
                    tags = state.tags,
                    onOpenArtist = onOpenArtist,
                    onOpenTag = onOpenTag,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun SongDetailContent(
    song: SongDto,
    tags: List<TagItemDto>,
    onOpenArtist: (ArtistKind, Int) -> Unit,
    onOpenTag: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val producers = song.producers
        .map { it.producer }
        .filter { it.id > 0 }
        .distinctBy { it.id }
        .map { ArtistChip(it.id, it.name, ArtistKind.Producer) }
    val vocalists = song.vocalists
        .map { it.vocalist }
        .filter { it.id > 0 }
        .distinctBy { it.id }
        .map { ArtistChip(it.id, it.name, ArtistKind.Vocalist) }
    val synthesizers = song.vocalists
        .mapNotNull { it.synthesizer }
        .filter { it.id > 0 }
        .distinctBy { it.id }
        .map { ArtistChip(it.id, it.name, ArtistKind.Synthesizer) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SongHero(song)
        }

        if (producers.isNotEmpty()) {
            item {
                ArtistChipRow(
                    title = "P主",
                    chips = producers,
                    onOpenArtist = onOpenArtist,
                )
            }
        }

        if (vocalists.isNotEmpty()) {
            item {
                ArtistChipRow(
                    title = "歌手",
                    chips = vocalists,
                    onOpenArtist = onOpenArtist,
                )
            }
        }

        if (synthesizers.isNotEmpty()) {
            item {
                ArtistChipRow(
                    title = "合成器",
                    chips = synthesizers,
                    onOpenArtist = onOpenArtist,
                )
            }
        }

        if (tags.isNotEmpty()) {
            item {
                TagChipRow(
                    tags = tags,
                    onOpenTag = onOpenTag,
                )
            }
        }

        item {
            SectionHeader(
                title = "视频",
                subtitle = if (song.videos.isEmpty()) null else "共 ${song.videos.size} 个投稿",
            )
        }

        if (song.videos.isEmpty()) {
            item {
                EmptyHint("暂无视频")
            }
        } else {
            itemsIndexed(
                items = song.videos,
                key = { index, video -> "${video.bvid}-$index" },
            ) { _, video ->
                VideoCard(video)
            }
        }
    }
}

@Composable
private fun SongHero(song: SongDto) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NetworkImage(
            url = song.coverUrl(),
            contentDescription = songDisplayName(song),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(24.dp)),
        )

        Text(
            text = songDisplayName(song),
            style = MaterialTheme.typography.headlineSmall,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(song.type?.takeIf { it.isNotBlank() } ?: "歌曲")
            if (song.isForeign) {
                StatusPill("外语")
            }
            if (!song.collected) {
                StatusPill("未收录")
            }
        }
    }
}

private data class ArtistChip(
    val id: Int,
    val name: String,
    val kind: ArtistKind,
)

@Composable
private fun ArtistChipRow(
    title: String,
    chips: List<ArtistChip>,
    onOpenArtist: (ArtistKind, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chips, key = { "${it.kind.apiType}-${it.id}" }) { chip ->
                AssistChip(
                    onClick = { onOpenArtist(chip.kind, chip.id) },
                    label = {
                        Text(
                            text = chip.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TagChipRow(
    tags: List<TagItemDto>,
    onOpenTag: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "标签",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tags, key = { it.id }) { tag ->
                AssistChip(
                    onClick = { onOpenTag(tag.id) },
                    label = {
                        Text(
                            text = tag.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun VideoCard(video: VideoDto) {
    val context = LocalContext.current
    val metrics = video.latestSnapshot
        ?.let { "${formatNumber(it.view)} 播放" }
        ?.takeIf { it.isNotBlank() }
    val date = formatShortDate(video.pubdate).takeIf { it.isNotBlank() }
    val meta = listOfNotNull(metrics, date).joinToString(" · ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openBilibiliVideo(context, video.bvid) }
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NetworkImage(
                url = video.thumbnail,
                contentDescription = video.title,
                modifier = Modifier
                    .width(124.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp)),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = video.title?.takeIf { it.isNotBlank() } ?: "未命名视频",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = video.uploader?.name?.takeIf { it.isNotBlank() } ?: "未知UP主",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(
                        enabled = !video.uploader?.uid.isNullOrBlank(),
                        onClick = {
                            openBilibiliUser(context, video.uploader?.uid)
                        },
                    ),
                )

                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (video.disabled) {
                    Text(
                        text = "视频已失效",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.size(2.dp))
        }
    }
}
