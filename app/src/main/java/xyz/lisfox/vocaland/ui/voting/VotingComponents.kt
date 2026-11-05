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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.EdCandidateDto
import xyz.lisfox.vocaland.data.model.EdResultDto
import xyz.lisfox.vocaland.data.model.EdVotingCandidateDto
import xyz.lisfox.vocaland.data.model.SearchEntityDto
import xyz.lisfox.vocaland.data.model.TrendingCandidateDto
import xyz.lisfox.vocaland.data.model.TrendingResultRowDto
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.util.categoryLabels
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.displayArtistLine
import xyz.lisfox.vocaland.util.displaySubtitle
import xyz.lisfox.vocaland.util.displayTitle
import xyz.lisfox.vocaland.util.statusLabel

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
    allowNegative: Boolean = true,
    onPositive: () -> Unit,
    onNegative: () -> Unit,
    onOpenSong: (Int) -> Unit,
    bonusCount: Int = 0,
    bonusIsNegative: Boolean = false,
    bonusRemaining: Int = 0,
    showBonus: Boolean = false,
    onAddBonus: () -> Unit = {},
    onRemoveBonus: () -> Unit = {},
    onBonusNegativeChange: (Boolean) -> Unit = {},
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
                    if (allowNegative) {
                        FilterChip(
                            selected = selectedNegative,
                            onClick = onNegative,
                            enabled = enabled,
                            label = { Text("反对") },
                        )
                    }
                }
                if (showBonus) {
                    Spacer(Modifier.height(4.dp))
                    EdBonusVoteControl(
                        count = bonusCount,
                        isNegative = bonusIsNegative,
                        remaining = bonusRemaining,
                        enabled = enabled,
                        onAdd = onAddBonus,
                        onRemove = onRemoveBonus,
                        onNegativeChange = onBonusNegativeChange,
                    )
                }
            }
        }
    }
}

@Composable
fun EdBonusVoteControl(
    count: Int,
    isNegative: Boolean,
    remaining: Int,
    enabled: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onNegativeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "附加票",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRemove, enabled = enabled && count > 0) {
                Text("-")
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
            )
            TextButton(onClick = onAdd, enabled = enabled && remaining > 0) {
                Text("+")
            }
            Text(
                text = "剩 $remaining",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (count > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = !isNegative,
                    onClick = { onNegativeChange(false) },
                    enabled = enabled,
                    label = { Text("附加赞成") },
                )
                FilterChip(
                    selected = isNegative,
                    onClick = { onNegativeChange(true) },
                    enabled = enabled,
                    label = { Text("附加反对") },
                )
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

@Composable
fun EdNominationCandidateRow(
    candidate: EdCandidateDto,
    onOpenSong: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val songId = candidate.songId?.takeIf { it > 0 }
        ?: candidate.song?.id?.takeIf { it > 0 }
    val categories = candidate.categoryLabels()
    val artistLine = candidate.displayArtistLine()
    val nominator = candidate.nominator?.nickname?.trim().orEmpty()
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
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPill(text = candidate.statusLabel())
                    categories.forEach { label -> StatusPill(text = label) }
                }
                if (nominator.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "提名者：$nominator",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val rejectReason = candidate.rejectReason?.trim().orEmpty()
                if (candidate.status == "rejected" && rejectReason.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "驳回原因：$rejectReason",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
fun EdResultItem(
    result: EdResultDto,
    onOpenSong: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val candidate = result.candidate
    val songId = candidate.songId?.takeIf { it > 0 }
        ?: candidate.song?.id?.takeIf { it > 0 }
    val scoreText = "%.2f".format(result.score)
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = result.rank.takeIf { it > 0 }?.toString() ?: "-",
                modifier = Modifier.width(30.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            NetworkImage(
                url = candidate.video?.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .width(84.dp)
                    .height(62.dp)
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
                val artistLine = candidate.displayArtistLine()
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
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "得分 $scoreText",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (result.songPenaltyPct > 0) {
                    Text(
                        text = "分票惩罚 ${"%.1f".format(result.songPenaltyPct)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
fun TrendingNominationRow(
    candidate: TrendingCandidateDto,
    withdrawing: Boolean,
    showWithdraw: Boolean = true,
    onWithdraw: () -> Unit = {},
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
            NetworkImage(
                url = imageUrl,
                contentDescription = null,
                modifier = if (isArtist) {
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                } else {
                    Modifier
                        .width(76.dp)
                        .height(56.dp)
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
                val nominator = candidate.nominator?.nickname?.trim().orEmpty()
                if (nominator.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "提名者：$nominator",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (showWithdraw) {
                TextButton(onClick = onWithdraw, enabled = !withdrawing) {
                    Text(if (withdrawing) "撤回中…" else "撤回")
                }
            }
        }
    }
}

@Composable
fun TrendingResultItem(
    result: TrendingResultRowDto,
    onOpenSong: (Int) -> Unit,
    onOpenArtist: (ArtistKind, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val candidate = result.candidate
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
                text = result.rank.takeIf { it > 0 }?.toString() ?: "-",
                modifier = Modifier.width(30.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            NetworkImage(
                url = imageUrl,
                contentDescription = null,
                modifier = if (isArtist) {
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                } else {
                    Modifier
                        .width(76.dp)
                        .height(56.dp)
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
            Text(
                text = "${result.voteCount} 票",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun TrendingEntitySearchRow(
    entity: SearchEntityDto,
    nominating: Boolean,
    onNominate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = entity.displayName?.trim()?.takeIf { it.isNotEmpty() }
        ?: entity.name.trim().takeIf { it.isNotEmpty() }
        ?: entity.title?.trim()?.takeIf { it.isNotEmpty() }
        ?: entity.bvid
    val producers = entity.producers.map { it.producer.name.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .joinToString(" / ")
    val vocalists = entity.vocalists.map { it.vocalist.name.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .joinToString(" / ")
    val subtitle = listOf(producers, vocalists)
        .filter { it.isNotEmpty() }
        .joinToString(" · ")
    val imageUrl = entity.thumbnail ?: entity.picture ?: entity.song?.coverUrl()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NetworkImage(
                url = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .width(64.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
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
            TextButton(onClick = onNominate, enabled = !nominating) {
                Text(if (nominating) "提交中…" else "提名")
            }
        }
    }
}
