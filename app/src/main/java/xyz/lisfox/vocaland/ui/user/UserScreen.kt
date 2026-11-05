package xyz.lisfox.vocaland.ui.user

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.community.CommunityRepository
import xyz.lisfox.vocaland.data.community.CommunityTargets
import xyz.lisfox.vocaland.data.model.PublicUserDto
import xyz.lisfox.vocaland.data.model.RatingDto
import xyz.lisfox.vocaland.data.model.UserLitStatsDto
import xyz.lisfox.vocaland.data.repository.UserRepository
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.ui.community.CommunityPanel
import xyz.lisfox.vocaland.ui.community.CommunityTab
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.MetricGrid
import xyz.lisfox.vocaland.ui.components.MetricItem
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.util.targetLabel
import xyz.lisfox.vocaland.util.timeAgo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserScreen(
    repository: UserRepository,
    userId: Int,
    onOpenSong: (Int) -> Unit,
    modifier: Modifier = Modifier,
    session: AuthSession? = null,
    communityRepository: CommunityRepository? = null,
    onLogin: () -> Unit = {},
    onOpenUser: (Int) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenWallet: (() -> Unit)? = null,
) {
    val viewModel: UserViewModel = viewModel(
        key = "user-$userId",
        factory = viewModelFactory {
            initializer { UserViewModel(repository, userId, session) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val community = repository.community ?: communityRepository

    when {
        state.loading && state.user == null -> {
            LoadingPane(modifier = modifier)
        }

        state.user == null -> {
            Box(modifier = modifier.padding(16.dp)) {
                ScreenMessage(
                    title = "用户加载失败",
                    message = state.error ?: "用户不存在",
                    actionLabel = "重试",
                    onAction = viewModel::load,
                )
            }
        }

        else -> {
            val user = state.user ?: return
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    UserHero(user)
                }

                item {
                    UserProfileCard(
                        profile = state.profile,
                        isLoggedIn = state.isLoggedIn,
                        nickname = user.nickname.ifBlank { user.username },
                        onLogin = onLogin,
                    )
                }

                state.litStats?.let { stats ->
                    item {
                        UserLitStatsCard(stats)
                    }
                }

                if (state.isOwner && community != null) {
                    item {
                        UserOwnerSection(
                            wallet = state.wallet,
                            level = state.level,
                            loading = state.ownerLoading,
                            error = state.ownerError,
                            notice = state.ownerNotice,
                            checkinBusy = state.checkinBusy,
                            onCheckin = viewModel::checkin,
                            onOpenSettings = onOpenSettings,
                            onOpenWallet = onOpenWallet,
                        )
                    }
                }

                item {
                    SectionHeader(
                        title = "评分",
                        subtitle = state.ratingsTotal.takeIf { it > 0 }?.let { "共 $it 条" },
                    )
                }

                if (state.ratings.isEmpty()) {
                    item {
                        EmptyHint("还没有评分")
                    }
                } else {
                    items(
                        count = state.ratings.size,
                        key = { index -> state.ratings[index].id.ifBlank { "rating-$index" } },
                        contentType = { "rating" },
                    ) { index ->
                        val rating = state.ratings[index]
                        Column(modifier = Modifier.animateItem()) {
                            UserRatingRow(
                                rating = rating,
                                onOpenSong = onOpenSong,
                            )
                            if (index != state.ratings.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                }

                item {
                    when {
                        state.ratingsLoadingMore -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            }
                        }

                        state.ratingsPage < state.ratingsTotalPages -> {
                            Button(
                                onClick = viewModel::loadMoreRatings,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("加载更多评分")
                            }
                        }
                    }
                    state.ratingsLoadMoreError?.let { error ->
                        TextButton(onClick = viewModel::loadMoreRatings) {
                            Text("加载失败，点击重试：$error")
                        }
                    }
                }

                if (community != null) {
                    item {
                        CommunityPanel(
                            repository = community,
                            targetKey = CommunityTargets.user(user.id),
                            session = session,
                            onLogin = onLogin,
                            onOpenUser = onOpenUser,
                            initialTab = CommunityTab.Comments,
                            showRatings = false,
                            title = "评论",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserHero(user: PublicUserDto) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            scheme.primaryContainer.copy(alpha = 0.85f),
                            scheme.tertiaryContainer.copy(alpha = 0.55f),
                        ),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                NetworkImage(
                    url = user.avatar,
                    contentDescription = user.nickname,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.nickname.ifBlank { user.username },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onPrimaryContainer,
                    )
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    text = "Lv.${user.level}",
                    containerColor = scheme.surface.copy(alpha = 0.7f),
                    contentColor = scheme.onSurface,
                )
                if (user.sponsorActive) {
                    StatusPill(
                        text = "赞助者",
                        containerColor = scheme.surface.copy(alpha = 0.7f),
                        contentColor = scheme.onSurface,
                    )
                }
                if (user.isBanned) {
                    StatusPill(
                        text = "账号受限",
                        containerColor = scheme.errorContainer,
                        contentColor = scheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserLitStatsCard(stats: UserLitStatsDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        MetricGrid(
            items = listOf(
                MetricItem("听过歌曲", stats.songCount.toString()),
                MetricItem("点亮P主", stats.producerCount.toString()),
                MetricItem("点亮歌手", stats.vocalistCount.toString()),
                MetricItem("想听", stats.wantCount.toString()),
            ),
            modifier = Modifier.padding(vertical = 14.dp),
            columns = 4,
        )
    }
}

@Composable
private fun UserRatingRow(
    rating: RatingDto,
    onOpenSong: (Int) -> Unit,
) {
    val songId = rating.targetKey
        .takeIf { it.startsWith("song:") }
        ?.removePrefix("song:")
        ?.toIntOrNull()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = songId != null) {
                songId?.let(onOpenSong)
            }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = rating.score.toString(),
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = rating.targetName?.takeIf { it.isNotBlank() }
                    ?: targetLabel(rating.targetKey),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            rating.comment
                ?.takeIf { it.isNotBlank() }
                ?.let { comment ->
                    Text(
                        text = comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
        }
        Text(
            text = timeAgo(rating.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
