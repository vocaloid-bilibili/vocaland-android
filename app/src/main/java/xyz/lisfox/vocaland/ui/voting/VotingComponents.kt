package xyz.lisfox.vocaland.ui.voting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlinx.coroutines.delay
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.EdCandidateDto
import xyz.lisfox.vocaland.data.model.EdHistoryDto
import xyz.lisfox.vocaland.data.model.EdMechanismConfigDto
import xyz.lisfox.vocaland.data.model.EdResultDto
import xyz.lisfox.vocaland.data.model.EdVotingCandidateDto
import xyz.lisfox.vocaland.data.model.SearchEntityDto
import xyz.lisfox.vocaland.data.model.TrendingCandidateDto
import xyz.lisfox.vocaland.data.model.TrendingResultRowDto
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.util.boardLabel
import xyz.lisfox.vocaland.util.categoryLabels
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.formatDecimal
import xyz.lisfox.vocaland.util.deadlineText
import xyz.lisfox.vocaland.util.displayArtistLine
import xyz.lisfox.vocaland.util.displaySubtitle
import xyz.lisfox.vocaland.util.displayTitle
import xyz.lisfox.vocaland.util.parseIsoMillis
import xyz.lisfox.vocaland.util.statusLabel
import xyz.lisfox.vocaland.util.votingStatusLabel

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
                val statusColors = votingStatusPillColors(status)
                StatusPill(
                    text = status,
                    containerColor = statusColors.first,
                    contentColor = statusColors.second,
                )
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

/** 投票倒计时：秒级刷新，到期回调一次（用于禁用投票按钮）。 */
@Composable
fun VoteCountdown(
    endTime: String?,
    modifier: Modifier = Modifier,
    onExpire: () -> Unit = {},
) {
    val currentOnExpire by rememberUpdatedState(onExpire)
    var remaining by remember(endTime) { mutableStateOf<Long?>(null) }
    var expired by remember(endTime) { mutableStateOf(false) }

    LaunchedEffect(endTime) {
        val end = parseIsoMillis(endTime)
        if (end == null) {
            remaining = null
            return@LaunchedEffect
        }
        while (true) {
            val left = ((end - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            remaining = left
            if (left <= 0) {
                if (!expired) {
                    expired = true
                    currentOnExpire()
                }
                break
            }
            delay(1000)
        }
    }

    val text = when {
        remaining == null -> deadlineText(endTime) ?: "截止时间待定"
        remaining == 0L -> "已截止"
        else -> {
            val total = remaining ?: 0L
            val hours = total / 3600
            val minutes = (total % 3600) / 60
            val seconds = total % 60
            "投票剩余 ${hours}时${minutes}分${seconds}秒"
        }
    }
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun EdRulesButton(
    config: EdMechanismConfigDto,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(" 规则")
    }
    if (open) {
        EdRulesDialog(config = config, onDismiss = { open = false })
    }
}

@Composable
private fun EdRulesDialog(
    config: EdMechanismConfigDto,
    onDismiss: () -> Unit,
) {
    val sections = edRuleSections(config)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("投票规则") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sections.forEach { (title, items) ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    items.forEachIndexed { index, item ->
                        Text(
                            text = "${index + 1}. $item",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        },
    )
}

private fun edRuleSections(config: EdMechanismConfigDto): List<Pair<String, List<String>>> {
    val roleBonus = formatMultiplier(config.roleBonusMultiplier)
    val negative = formatPct(config.negativeWeight * 100)
    val weeklyPenalty = formatPct(config.winPenaltyWeeklyMonthlyPct)
    val dailyPenalty = formatPct(config.winPenaltyDailyPct)
    return listOf(
        "提名规则" to listOf(
            "投稿时间需早于当前时间 60 天以上。",
            "歌曲时长需符合对应榜单范围：日刊 1:00~4:40，周刊 3:00~4:20，月刊 4:40~7:00。",
            "周刊活动可提名符合日刊或周刊时长规则的歌曲。",
            "月刊活动仅可提名符合月刊时长规则的歌曲。",
            "不可提名已经成为过 ED 的歌曲。",
            "最近 2 期周刊（主榜前 20、新曲榜前 10）或最近 1 期月刊（主榜前 20、新曲榜前 20）上过榜的歌曲暂不可提名。",
            "最近 10 期内累计 3 人次提名但均未入选的歌曲，暂不可继续提名。",
        ),
        "投票与结算规则" to listOf(
            "每期可投票数由本期可提名数量决定。",
            "投票权按站内用户等级计算。",
            "worker 及以上用户投票权 ×${roleBonus}。",
            "反对票按当前票权的 ${negative}% 计算。",
            "附加票是商城购买的消耗品；每首歌投入的张数按指数 ${formatMultiplier(config.bonusExponent)} 的次线性函数折算为附加分。",
            "获得周刊第一或月刊第一后，之后的投票权和自己提名歌曲得分各减少 ${weeklyPenalty}%（日刊 ${dailyPenalty}%），惩罚窗口为最近 ${config.winPenaltyWindowMonths} 个月。该惩罚可叠加。",
            "周刊第一成为当期周刊 ED。",
            "周刊中符合日刊规则的剩余前 7 名，成为当周每天的日刊 ED。",
            "月刊第一成为当期月刊 ED。",
            "同分时依次比较：${config.tieBreakers.joinToString(" > ").ifBlank { "最终得分 > 赞成票 > 附加分 > 反对票 > 候选编号" }}。",
        ),
    )
}

private fun formatPct(value: Double): String =
    if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.CHINA, "%.2f", value)
    }

private fun formatMultiplier(value: Double): String =
    if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(Locale.CHINA, "%.2f", value).trimEnd('0').trimEnd('.')
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
    isWinner: Boolean = false,
    detailsOpen: Boolean = false,
    negativeWeight: Double = 0.5,
) {
    val candidate = result.candidate
    val songId = candidate.songId?.takeIf { it > 0 }
        ?: candidate.song?.id?.takeIf { it > 0 }
    val scoreText = formatDecimal(result.score, 2)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = songId != null) {
                songId?.let(onOpenSong)
            },
        shape = RoundedCornerShape(18.dp),
        color = if (isWinner) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isWinner) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "第一名",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = result.rank.takeIf { it > 0 }?.toString() ?: "-",
                        modifier = Modifier.width(30.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (isWinner) Spacer(Modifier.width(8.dp))
                NetworkImage(
                    url = candidate.video?.thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .width(if (isWinner) 108.dp else 84.dp)
                        .height(if (isWinner) 76.dp else 62.dp)
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
                        text = "得分 $scoreText · ${result.voteSnapshots.size} 票",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (result.songPenaltyPct > 0) {
                        Text(
                            text = "提名者折分 × ${formatDecimal(100 - result.songPenaltyPct, 1)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            if (detailsOpen && result.voteSnapshots.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                EdResultDetails(
                    result = result,
                    negativeWeight = negativeWeight,
                )
            }
        }
    }
}

@Composable
private fun EdResultDetails(
    result: EdResultDto,
    negativeWeight: Double,
) {
    val snapshots = result.voteSnapshots
    val positive = snapshots.count { !it.isNegative && !it.isBonus }
    val negative = snapshots.count { it.isNegative && !it.isBonus }
    val bonusEntries = snapshots.filter { it.isBonus }
    val bonusCount = bonusEntries.sumOf { it.bonusCount }
    val rawTotal = snapshots.sumOf { it.actualWeight }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "赞成 $positive · 反对 $negative · 附加 ${bonusEntries.size} 人 / $bonusCount 张 · 实际票权合计 ${formatDecimal(rawTotal, 2)} · 最终 ${formatDecimal(result.score, 2)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (result.songPenaltyPct > 0) {
                Text(
                    text = "提名者折分：× ${formatDecimal(100 - result.songPenaltyPct, 1)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            snapshots.forEach { snapshot ->
                val kind = when {
                    snapshot.isBonus -> "附加×${snapshot.bonusCount}"
                    snapshot.isNegative -> "反对"
                    else -> "赞成"
                }
                val user = snapshot.user?.nickname?.takeIf { it.isNotBlank() }
                    ?: "用户 ${snapshot.userId}"
                val role = snapshot.user?.role?.takeIf { it.isNotBlank() }
                val bonusPct = snapshot.roleBonusPct
                val penaltyPct = snapshot.votePenaltyPct
                val detail = buildString {
                    append("$user · $kind")
                    role?.let { append(" · $it") }
                    if (bonusPct > 100) append(" · 角色加成 ${formatDecimal(bonusPct, 0)}%")
                    if (penaltyPct > 0) append(" · 投票惩罚 ${formatDecimal(penaltyPct, 1)}%")
                    append(" · 权重 ${formatDecimal(snapshot.rawWeight, 2)} → ${formatDecimal(snapshot.actualWeight, 2)}")
                }
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (negative > 0) {
                Text(
                    text = "反对票权重系数：${formatDecimal(negativeWeight * 100, 0)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
fun EdHistoryRow(
    history: EdHistoryDto,
    onOpenSong: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val song = history.song
    val songId = history.songId?.takeIf { it > 0 } ?: song?.id?.takeIf { it > 0 }
    val title = song?.displayName?.trim()?.takeIf { it.isNotEmpty() }
        ?: song?.name?.trim()?.takeIf { it.isNotEmpty() }
        ?: history.video?.title?.trim()?.takeIf { it.isNotEmpty() }
        ?: history.bvid
    val artist = listOf(
        song?.producers.orEmpty().mapNotNull { it.name?.trim()?.takeIf { name -> name.isNotEmpty() } }
            .distinct().joinToString(" / "),
        song?.vocalists.orEmpty().mapNotNull { it.name?.trim()?.takeIf { name -> name.isNotEmpty() } }
            .distinct().joinToString(" / "),
    ).filter { it.isNotEmpty() }.joinToString(" · ")
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = songId != null) { songId?.let(onOpenSong) },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NetworkImage(
                url = history.video?.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .width(84.dp)
                    .height(62.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${boardLabel(history.board)} · 第 ${history.issue} 期",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (artist.isNotBlank()) {
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = buildString {
                        append("得分 ${formatDecimal(history.score, 2)}")
                        history.nominator?.nickname?.takeIf { it.isNotBlank() }?.let {
                            append(" · 提名 $it")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
    entityKind: String,
    submitting: Boolean,
    enabled: Boolean,
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
                modifier = if (entityKind == "song") {
                    Modifier
                        .width(64.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                } else {
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                },
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
            TextButton(onClick = onNominate, enabled = enabled) {
                Text(if (submitting) "提交中…" else "提名")
            }
        }
    }
}

@Composable
internal fun votingStatusPillColors(label: String): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (label) {
        votingStatusLabel("voting") -> scheme.primaryContainer to scheme.onPrimaryContainer
        votingStatusLabel("nomination") -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
    }
}