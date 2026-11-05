package xyz.lisfox.vocaland.ui.voting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.model.EdCampaignDto
import xyz.lisfox.vocaland.data.model.TrendingCampaignDto
import xyz.lisfox.vocaland.data.model.VotingKind
import xyz.lisfox.vocaland.data.repository.VotingHubData
import xyz.lisfox.vocaland.data.repository.VotingRepository
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.util.boardLabel
import xyz.lisfox.vocaland.util.deadlineText
import xyz.lisfox.vocaland.util.trendingEntityKindLabel
import xyz.lisfox.vocaland.util.votingStatusLabel

@Composable
fun VotingHubScreen(
    repository: VotingRepository,
    onOpenVoting: (VotingKind, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: VotingHubViewModel = viewModel(
        factory = viewModelFactory {
            initializer { VotingHubViewModel(repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.loading && state.data == null -> {
            LoadingPane(modifier = modifier, text = "正在加载投票活动…")
        }

        state.data == null -> {
            Box(modifier = modifier.padding(16.dp)) {
                ScreenMessage(
                    title = "投票活动加载失败",
                    message = state.error ?: "请稍后重试",
                    actionLabel = "重新加载",
                    onAction = viewModel::refresh,
                )
            }
        }

        else -> {
            VotingHubContent(
                data = state.data!!,
                onOpenVoting = onOpenVoting,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun VotingHubContent(
    data: VotingHubData,
    onOpenVoting: (VotingKind, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ed = data.ed
        .filter { it.status == "voting" || it.status == "nomination" || it.status == "finalized" }
        .sortedWith(
            compareBy<EdCampaignDto> { statusOrder(it.status) }.thenByDescending { it.id },
        )
    val trending = data.trending
        .filter { it.status == "voting" || it.status == "nomination" || it.status == "finalized" }
        .sortedWith(
            compareBy<TrendingCampaignDto> { statusOrder(it.status) }
                .thenByDescending { it.id },
        )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "ED 投票",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (ed.isEmpty()) {
            item { EmptyHint(text = "暂无 ED 投票活动") }
        } else {
            items(ed, key = { "ed-${it.id}" }) { campaign ->
                EdCampaignRow(
                    campaign = campaign,
                    onClick = { onOpenVoting(VotingKind.Ed, campaign.id) },
                )
            }
        }

        item {
            Spacer(Modifier.height(14.dp))
            Text(
                text = "人气投票",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (trending.isEmpty()) {
            item { EmptyHint(text = "暂无人气投票活动") }
        } else {
            items(trending, key = { "trending-${it.id}" }) { campaign ->
                TrendingCampaignRow(
                    campaign = campaign,
                    onClick = { onOpenVoting(VotingKind.Trending, campaign.id) },
                )
            }
        }
    }
}

@Composable
private fun EdCampaignRow(
    campaign: EdCampaignDto,
    onClick: () -> Unit,
) {
    val deadline = deadlineText(
        when (campaign.status) {
            "voting" -> campaign.voteEnd
            "finalized" -> campaign.finalizedAt
            else -> campaign.submitEnd
        },
    )
    CampaignRow(
        title = "${boardLabel(campaign.board)} · 第 ${campaign.issue} 期",
        status = votingStatusLabel(campaign.status),
        deadline = deadline,
        meta = buildList {
            campaign.acceptedCount?.let { add("$it 个候选") }
            campaign.voterCount?.let { add("$it 人投票") }
        }.joinToString(" · "),
        onClick = onClick,
    )
}

@Composable
private fun TrendingCampaignRow(
    campaign: TrendingCampaignDto,
    onClick: () -> Unit,
) {
    val deadline = deadlineText(
        when (campaign.status) {
            "voting" -> campaign.voteEnd
            "finalized" -> campaign.finalizedAt
            else -> campaign.submitEnd
        },
    )
    CampaignRow(
        title = "第 ${campaign.issue} 期 · ${trendingEntityKindLabel(campaign.entityKind)}",
        status = votingStatusLabel(campaign.status),
        deadline = deadline,
        meta = buildList {
            campaign.candidateCount?.let { add("$it 个候选") }
            campaign.voterCount?.let { add("$it 人投票") }
        }.joinToString(" · "),
        onClick = onClick,
    )
}

@Composable
private fun CampaignRow(
    title: String,
    status: String,
    deadline: String?,
    meta: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(text = status)
                    if (!deadline.isNullOrBlank()) {
                        Text(
                            text = deadline,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (meta.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun statusOrder(status: String): Int = when (status) {
    "voting" -> 0
    "nomination" -> 1
    "finalized" -> 2
    else -> 3
}
