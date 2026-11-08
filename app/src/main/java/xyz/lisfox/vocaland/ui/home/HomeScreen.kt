package xyz.lisfox.vocaland.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.model.HomeContent
import xyz.lisfox.vocaland.data.model.SongListItemDto
import xyz.lisfox.vocaland.data.model.VotingKind
import xyz.lisfox.vocaland.data.repository.HomeRepository
import xyz.lisfox.vocaland.ui.components.InlineLoading
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.navigation.Routes
import xyz.lisfox.vocaland.ui.playlist.PlaylistPreviewSection
import xyz.lisfox.vocaland.util.SiteTarget
import xyz.lisfox.vocaland.util.parseSitePath
import xyz.lisfox.vocaland.util.parseTargetKey

@Composable
fun HomeScreen(
    repository: HomeRepository,
    onOpenFeature: (String) -> Unit,
    onOpenTarget: (SiteTarget) -> Unit,
    onOpenSong: (Int) -> Unit,
    onOpenUser: (Int) -> Unit,
    onOpenPlaylist: (Int) -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenVotings: () -> Unit,
    onOpenVoting: (VotingKind, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HomeViewModel(repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = modifier) {
        when {
            state.loading && state.content == null -> {
                LoadingPane(text = "正在整理今天的术力口数据…")
            }

            state.content == null -> {
                Box(modifier = Modifier.padding(16.dp)) {
                    ScreenMessage(
                        title = "首页加载失败",
                        message = state.error ?: "请检查网络后重试",
                        actionLabel = "重新加载",
                        onAction = viewModel::refresh,
                    )
                }
            }

            else -> {
                HomeContentList(
                    content = state.content!!,
                    refreshing = state.refreshing,
                    error = state.error,
                    selectedStat = state.selectedStat,
                    topSongs = state.topSongs,
                    topSongsLoading = state.topSongsLoading,
                    topSongsError = state.topSongsError,
                    onRefresh = viewModel::refresh,
                    onSelectStat = viewModel::selectStat,
                    onOpenFeature = onOpenFeature,
                    onOpenTarget = onOpenTarget,
                    onOpenSong = onOpenSong,
                    onOpenUser = onOpenUser,
                    onOpenPlaylist = onOpenPlaylist,
                    onOpenPlaylists = onOpenPlaylists,
                    onOpenVotings = onOpenVotings,
                    onOpenVoting = onOpenVoting,
                )
            }
        }
    }
}

@Composable
private fun HomeContentList(
    content: HomeContent,
    refreshing: Boolean,
    error: String?,
    selectedStat: String,
    topSongs: List<SongListItemDto>,
    topSongsLoading: Boolean,
    topSongsError: String?,
    onRefresh: () -> Unit,
    onSelectStat: (String) -> Unit,
    onOpenFeature: (String) -> Unit,
    onOpenTarget: (SiteTarget) -> Unit,
    onOpenSong: (Int) -> Unit,
    onOpenUser: (Int) -> Unit,
    onOpenPlaylist: (Int) -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenVotings: () -> Unit,
    onOpenVoting: (VotingKind, Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (refreshing) {
            item(contentType = "inline-loading") {
                InlineLoading()
            }
        }

        if (!error.isNullOrBlank()) {
            item(contentType = "error") {
                ScreenMessage(
                    title = "刷新失败",
                    message = error,
                    actionLabel = "重试",
                    onAction = onRefresh,
                )
            }
        }

        if (content.failedSections.isNotEmpty()) {
            item(contentType = "failed-notice") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "部分内容加载失败",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                text = content.failedSections.joinToString("、"),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        TextButton(onClick = onRefresh) {
                            Text("重试")
                        }
                    }
                }
            }
        }

        item(contentType = "hero") {
            HeroCarousel(
                slides = content.heroSlides,
                onClick = { slide ->
                    val target = parseSitePath(slide.linkUrl)
                    if (target != null) {
                        onOpenTarget(target)
                    } else {
                        onOpenFeature(Routes.FeatureKey.HERO_SLIDE)
                    }
                },
            )
        }

        item(contentType = "stats") {
            StatsCard(stats = content.stats)
        }

        item(contentType = "playlists") {
            PlaylistPreviewSection(
                playlists = content.playlists,
                onOpenPlaylist = onOpenPlaylist,
                onOpenAll = onOpenPlaylists,
            )
        }

        item(contentType = "voting") {
            VotingCard(
                entries = content.votingEntries,
                summary = content.votingSummary,
                onOpenAll = onOpenVotings,
                onOpenEntry = { onOpenVoting(it.kind, it.campaignId) },
            )
        }

        item(contentType = "milestones") {
            MilestonesSection(
                milestones = content.milestones,
                onOpen = {
                    val songId = it.song.id.takeIf { id -> id > 0 } ?: it.songId
                    if (songId > 0) onOpenSong(songId)
                },
                onOpenAll = { onOpenFeature(Routes.FeatureKey.MILESTONES) },
            )
        }

        item(contentType = "top-songs") {
            TopSongsSection(
                selectedStat = selectedStat,
                items = topSongs,
                loading = topSongsLoading,
                error = topSongsError,
                onSelectStat = onSelectStat,
                onOpenSong = onOpenSong,
            )
        }

        item(contentType = "on-this-day") {
            OnThisDaySection(
                month = content.todayMonth,
                day = content.todayDay,
                items = content.onThisDay,
                onOpenSong = onOpenSong,
            )
        }

        item(contentType = "recent-activity") {
            RecentActivitySection(
                ratings = content.ratings,
                comments = content.comments,
                onOpenRating = { rating ->
                    val target = parseTargetKey(rating.targetKey)
                    if (target != null) {
                        onOpenTarget(target)
                    } else {
                        onOpenFeature(Routes.FeatureKey.RATING_DETAIL)
                    }
                },
                onOpenComment = { comment ->
                    val target = parseTargetKey(comment.targetKey)
                    if (target != null) {
                        onOpenTarget(target)
                    } else {
                        onOpenFeature(Routes.FeatureKey.COMMENT_DETAIL)
                    }
                },
                onOpenUser = onOpenUser,
            )
        }

        item(contentType = "latest-songs") {
            LatestSongsSection(
                songs = content.latestSongs,
                onOpenSong = onOpenSong,
            )
        }

        item(contentType = "footer") {
            Text(
                text = "VocaLand · 术力口数据库",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
