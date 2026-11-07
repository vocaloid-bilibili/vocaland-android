package xyz.lisfox.vocaland.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.lisfox.vocaland.data.model.CommentDto
import xyz.lisfox.vocaland.data.model.MilestoneDto
import xyz.lisfox.vocaland.data.model.RatingDto
import xyz.lisfox.vocaland.data.model.SiteStatsDto
import xyz.lisfox.vocaland.data.model.VotingEntryUi
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.util.compactParts
import xyz.lisfox.vocaland.util.formatNumber
import xyz.lisfox.vocaland.util.formatShortDate
import xyz.lisfox.vocaland.util.songDisplayName
import xyz.lisfox.vocaland.util.targetLabel
import xyz.lisfox.vocaland.util.timeAgo

@Composable
fun StatsCard(
    stats: SiteStatsDto?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        if (stats == null) {
            Text(
                text = "暂无统计数据",
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem(value = stats.songCount, label = "首收录歌曲")
                StatItem(value = stats.snapshotCount, label = "条数据快照")
                StatItem(value = stats.userCount, label = "位注册用户")
            }
        }
    }
}

@Composable
private fun StatItem(value: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatNumber(value),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun VotingCard(
    entries: List<VotingEntryUi>,
    summary: String,
    onOpenAll: () -> Unit,
    onOpenEntry: (VotingEntryUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HowToVote,
                            contentDescription = null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "投票",
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onPrimaryContainer,
                        )
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onPrimaryContainer.copy(alpha = 0.72f),
                        )
                    }
                }
                TextButton(onClick = onOpenAll) {
                    Text("全部")
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    entries.forEach { entry ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenEntry(entry) },
                            shape = RoundedCornerShape(14.dp),
                            color = scheme.surfaceContainerLow,
                            contentColor = scheme.onSurface,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (entry.isVoting) {
                                        Icons.Filled.HowToVote
                                    } else {
                                        Icons.Filled.EmojiEvents
                                    },
                                    contentDescription = null,
                                    tint = scheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.typeLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = scheme.primary,
                                    )
                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (!entry.deadlineText.isNullOrBlank()) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = entry.deadlineText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MilestonesSection(
    milestones: List<MilestoneDto>,
    onOpen: (MilestoneDto) -> Unit,
    onOpenAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "最近里程碑",
            action = {
                TextButton(onClick = onOpenAll) {
                    Text("查看全部")
                }
            },
        )
        Spacer(Modifier.height(12.dp))

        if (milestones.isEmpty()) {
            EmptyHint("暂无里程碑数据")
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(milestones, key = { it.id }) { item ->
                    MilestoneCard(item = item, onClick = { onOpen(item) })
                }
            }
        }
    }
}

@Composable
private fun MilestoneCard(
    item: MilestoneDto,
    onClick: () -> Unit,
) {
    val (value, unit) = compactParts(item.milestone)
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            ) {
                NetworkImage(
                    url = item.video?.thumbnail,
                    contentDescription = songDisplayName(item.song),
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Text(
                        text = "$value$unit",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = songDisplayName(item.song),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = formatShortDate(item.achievedDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun RatingsSection(
    ratings: List<RatingDto>,
    onOpen: (RatingDto) -> Unit,
    onOpenUser: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "最近评分")
        Spacer(Modifier.height(12.dp))

        if (ratings.isEmpty()) {
            EmptyHint("暂时还没有评分")
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(ratings, key = { it.id }) { item ->
                    RatingCard(
                        item = item,
                        onClick = { onOpen(item) },
                        onOpenUser = onOpenUser,
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingCard(
    item: RatingDto,
    onClick: () -> Unit,
    onOpenUser: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val masked = item.isAnonymous && !item.isRevealed
    val label = targetLabel(item.targetKey, item.targetName)

    Card(
        modifier = Modifier
            .width(236.dp)
            .heightIn(min = 136.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = item.score.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "/ 10",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.comment?.replace(Regex("\\s+"), " ")?.takeIf { it.isNotBlank() }
                    ?: "只打了分，没留言",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (masked) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(scheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                } else {
                    NetworkImage(
                        url = item.user.avatar,
                    fallbackIcon = Icons.Filled.Person,
                        contentDescription = item.user.nickname,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable(enabled = item.user.id > 0) {
                                onOpenUser(item.user.id)
                            },
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = item.user.nickname,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = timeAgo(item.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
fun CommentsSection(
    comments: List<CommentDto>,
    onOpen: (CommentDto) -> Unit,
    onOpenUser: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "最近评论")
        Spacer(Modifier.height(8.dp))

        if (comments.isEmpty()) {
            EmptyHint("暂时还没有评论")
        } else {
            Column {
                comments.forEach { comment ->
                    CommentRow(
                        comment = comment,
                        onClick = { onOpen(comment) },
                        onOpenUser = onOpenUser,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: CommentDto,
    onClick: () -> Unit,
    onOpenUser: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        NetworkImage(
            url = comment.user.avatar,
                    fallbackIcon = Icons.Filled.Person,
            contentDescription = comment.user.nickname,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(enabled = comment.user.id > 0) {
                    onOpenUser(comment.user.id)
                },
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.user.nickname,
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = timeAgo(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = targetLabel(comment.targetKey),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (comment.likeCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "赞 ${formatNumber(comment.likeCount.toLong())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = comment.content.replace(Regex("\\s+"), " "),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


