package xyz.lisfox.vocaland.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.lisfox.vocaland.data.model.RatingDto
import xyz.lisfox.vocaland.data.model.RatingSummaryDto
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.util.timeAgo
import kotlin.math.max

@Composable
internal fun CommunityRatingSection(
    state: CommunityUiState,
    viewModel: CommunityViewModel,
    onLogin: () -> Unit,
    onOpenUser: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RatingSummaryCard(state.ratingSummary)

        if (state.isLoggedIn) {
            MyRatingEditor(
                state = state,
                onScore = viewModel::selectRatingScore,
                onComment = viewModel::updateRatingComment,
                onToggleAnonymous = viewModel::toggleRatingAnonymous,
                onSubmit = viewModel::submitRating,
                onWithdraw = viewModel::withdrawRating,
            )
        } else {
            CommunityLoginCard(
                message = "登录后可评分，评分与留言默认实名公开，也可以匿名提交。",
                onLogin = onLogin,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "全部评分",
                style = MaterialTheme.typography.titleMedium,
            )
            state.ratingsTotal.takeIf { it > 0 }?.let { total ->
                Text(
                    text = "共 $total 条",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when {
            state.ratingsLoading && state.ratings.isEmpty() -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "评分加载中",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            state.ratingsError != null && state.ratings.isEmpty() -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CommunityInlineMessage(
                        text = state.ratingsError ?: "评分加载失败",
                        isError = true,
                    )
                    TextButton(onClick = viewModel::retryRatings) {
                        Text("重试")
                    }
                }
            }

            state.ratings.isEmpty() -> {
                EmptyHint("还没有人评分，来抢第一个")
            }

            else -> {
                state.ratings.forEachIndexed { index, rating ->
                    RatingRow(
                        rating = rating,
                        onOpenUser = onOpenUser,
                    )
                    if (index != state.ratings.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        CommunityPager(
            page = state.ratingsPage,
            totalPages = state.ratingsTotalPages,
            loading = state.ratingsLoading,
            onPrevious = viewModel::previousRatingsPage,
            onNext = viewModel::nextRatingsPage,
        )
    }
}

@Composable
private fun RatingSummaryCard(
    summary: RatingSummaryDto?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val total = summary?.total ?: 0
    val average = summary?.avgScore
    val counts = remember(summary) {
        List(COMMUNITY_SCORE_RANGE.count()) { index -> summary?.scoreCounts?.getOrNull(index) ?: 0 }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = average?.let { String.format("%.1f", it) } ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ScoreStars(average)
                    Text(
                        text = if (total > 0) "$total 人评分" else "暂无评分",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            if (total > 0 && counts.any { it > 0 }) {
                ScoreDistribution(counts = counts)
            }
        }
    }
}

@Composable
private fun ScoreStars(score: Double?) {
    val value = score ?: 0.0
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(5) { index ->
            val lower = index * 2.0
            val upper = lower + 2.0
            val tint = when {
                value >= upper -> MaterialTheme.colorScheme.primary
                value > lower -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            }
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ScoreDistribution(
    counts: List<Int>,
    modifier: Modifier = Modifier,
) {
    val maxCount = max(counts.maxOrNull() ?: 0, 1)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        counts.forEachIndexed { score, count ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.height(48.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                if (count > 0) {
                                    max((count.toFloat() / maxCount) * 44f, 4f).dp
                                } else {
                                    2.dp
                                },
                            )
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = score.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MyRatingEditor(
    state: CommunityUiState,
    onScore: (Int) -> Unit,
    onComment: (String) -> Unit,
    onToggleAnonymous: () -> Unit,
    onSubmit: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val draft = state.ratingDraft
    val myRating = state.myRating
    val hasRating = myRating != null

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "我的评分",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = when {
                        state.myRatingLoading -> "读取中…"
                        myRating != null -> "已评 ${myRating.score} 分${if (myRating.isAnonymous) "（匿名）" else ""}"
                        else -> "还没打分"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxItemsInEachRow = 6,
            ) {
                COMMUNITY_SCORE_RANGE.forEach { score ->
                    FilterChip(
                        selected = draft.score == score,
                        onClick = { onScore(score) },
                        label = { Text(score.toString()) },
                    )
                }
            }

            OutlinedTextField(
                value = draft.comment,
                onValueChange = onComment,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("留言（可选）") },
                minLines = 2,
                maxLines = 4,
                supportingText = {
                    Text(
                        text = "${draft.comment.length} / 1000",
                        color = scheme.onSurfaceVariant,
                    )
                },
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Checkbox(
                    checked = draft.isAnonymous,
                    onCheckedChange = { onToggleAnonymous() },
                )
                Text(
                    text = "匿名提交：只在这里显示为「匿名用户」",
                    modifier = Modifier.clickable { onToggleAnonymous() },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onSubmit,
                    enabled = draft.score in COMMUNITY_SCORE_RANGE && !state.ratingSubmitting,
                ) {
                    Text(
                        when {
                            state.ratingSubmitting -> "保存中…"
                            hasRating -> "改分"
                            else -> "提交"
                        },
                    )
                }
                if (hasRating) {
                    TextButton(
                        onClick = onWithdraw,
                        enabled = !state.ratingSubmitting,
                    ) {
                        Text("撤回")
                    }
                }
            }

            state.ratingNotice?.let { notice ->
                Text(
                    text = notice,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.primary,
                )
            }
            state.ratingError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.error,
                )
            }
        }
    }
}

@Composable
private fun RatingRow(
    rating: RatingDto,
    onOpenUser: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val masked = rating.isAnonymous && !rating.isRevealed
    val displayName = if (masked) {
        "匿名用户"
    } else {
        rating.user.nickname.ifBlank { "用户 #${rating.user.id}" }
    }
    val canOpenUser = !masked && rating.user.id > 0 && onOpenUser != null
    val time = rating.updatedAt.takeIf { it.isNotBlank() } ?: rating.createdAt

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.width(42.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = rating.score.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "/ 10",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = displayName,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = canOpenUser) {
                            if (canOpenUser) onOpenUser?.invoke(rating.user.id)
                        },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!masked) {
                    StatusPill(text = "Lv.${rating.user.level}")
                }
                if (rating.isAnonymous) {
                    StatusPill(
                        text = "匿名",
                        containerColor = scheme.tertiaryContainer,
                        contentColor = scheme.onTertiaryContainer,
                    )
                }
            }

            rating.comment
                ?.takeIf { it.isNotBlank() }
                ?.let { comment ->
                    Text(
                        text = comment,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                ?: Text(
                    text = "只打了分，没留言",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
        }

        Text(
            text = timeAgo(time),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
    }
}
