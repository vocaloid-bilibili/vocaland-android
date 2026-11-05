package xyz.lisfox.vocaland.ui.voting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.EdVotingCandidateDto
import xyz.lisfox.vocaland.data.model.TrendingCandidateDto
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.util.categoryLabels
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.displayArtistLine
import xyz.lisfox.vocaland.util.displaySubtitle
import xyz.lisfox.vocaland.util.displayTitle

@Composable
fun VotingCampaignHeader(
    title: String,
    status: String,
    deadline: String?,
    meta: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.width(10.dp))
                StatusPill(text = status)
            }
            if (!deadline.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = deadline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!meta.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun VotingLoginCard(
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenMessage(
        title = "登录后投票",
        message = "登录后才能查看候选并提交选票。",
        modifier = modifier,
        actionLabel = "去登录",
        onAction = onLogin,
    )
}

@Composable
fun VotingStatusCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    ScreenMessage(
        title = title,
        message = message,
        modifier = modifier,
    )
}

@Composable
fun VotingInlineMessage(
    text: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isError) scheme.errorContainer else scheme.secondaryContainer,
        contentColor = if (isError) scheme.onErrorContainer else scheme.onSecondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun VotingSubmitBar(
    text: String,
    actionLabel: String,
    enabled: Boolean,
    submitting: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onSubmit,
                enabled = enabled && !submitting,
            ) {
                Text(if (submitting) "保存中…" else actionLabel)
            }
        }
    }
}

@Composable
fun EdCandidateRow(
    candidate: EdVotingCandidateDto,
    selectedPositive: Boolean,
    selectedNegative: Boolean,
    enabled: Boolean,
    onPositive: () -> Unit,
    onNegative: () -> Unit,
    onOpenSong: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val songId = candidate.songId?.takeIf { it > 0 }
        ?: candidate.song?.id?.takeIf { it > 0 }
    val categories = candidate.categoryLabels()
    val artistLine = candidate.displayArtistLine()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = songId != null) {
                songId?.let(onOpenSong)
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            NetworkImage(
                url = candidate.video?.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .width(92.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.displayTitle(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (artistLine.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = artistLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (categories.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach { label ->
                            StatusPill(text = label)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedPositive,
                        onClick = onPositive,
                        enabled = enabled,
                        label = { Text("赞成") },
                    )
                    FilterChip(
                        selected = selectedNegative,
                        onClick = onNegative,
                        enabled = enabled,
                        label = { Text("反对") },
                    )
                }
            }
        }
    }
}

@Composable
fun TrendingCandidateRow(
    candidate: TrendingCandidateDto,
    rank: Int,
    voteCount: Int,
    selected: Boolean,
    canVote: Boolean,
    onToggle: () -> Unit,
    onOpenSong: (Int) -> Unit,
    onOpenArtist: (ArtistKind, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val song = candidate.song
    val producer = candidate.producer
    val vocalist = candidate.vocalist
    val songId = song?.id?.takeIf { it > 0 }
    val producerId = producer?.id?.takeIf { it > 0 }
    val vocalistId = vocalist?.id?.takeIf { it > 0 }
    val hasTarget = songId != null || producerId != null || vocalistId != null
    val imageUrl = song?.coverUrl() ?: producer?.picture ?: vocalist?.picture
    val isArtist = song == null && (producer != null || vocalist != null)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = hasTarget) {
                when {
                    songId != null -> onOpenSong(songId)
                    producerId != null -> onOpenArtist(ArtistKind.Producer, producerId)
                    vocalistId != null -> onOpenArtist(ArtistKind.Vocalist, vocalistId)
                    else -> Unit
                }
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = rank.toString(),
                modifier = Modifier.width(24.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NetworkImage(
                url = imageUrl,
                contentDescription = null,
                modifier = if (isArtist) {
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                } else {
                    Modifier
                        .width(84.dp)
                        .height(62.dp)
                        .clip(RoundedCornerShape(12.dp))
                },
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.displayTitle(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = candidate.displaySubtitle()
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = voteCount.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "票",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (canVote) {
                    Spacer(Modifier.height(2.dp))
                    FilterChip(
                        selected = selected,
                        onClick = onToggle,
                        label = { Text(if (selected) "已投" else "投票") },
                    )
                }
            }
        }
    }
}
