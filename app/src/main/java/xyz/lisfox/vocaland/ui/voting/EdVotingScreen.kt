package xyz.lisfox.vocaland.ui.voting

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.model.EdCampaignDto
import xyz.lisfox.vocaland.data.repository.VotingRepository
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.ui.components.InlineLoading
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.util.boardLabel
import xyz.lisfox.vocaland.util.deadlineText
import xyz.lisfox.vocaland.util.votingStatusLabel

@Composable
fun EdVotingScreen(
    repository: VotingRepository,
    session: AuthSession,
    campaignId: Int,
    onLogin: () -> Unit,
    onOpenSong: (Int) -> Unit,
    onOpenShop: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel: EdVotingViewModel = viewModel(
        factory = viewModelFactory {
            initializer { EdVotingViewModel(repository, session, campaignId) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.loading && state.campaign == null -> {
            LoadingPane(modifier = modifier, text = "正在加载 ED 投票…")
        }

        state.campaign == null -> {
            Box(modifier = modifier.padding(16.dp)) {
                ScreenMessage(
                    title = "ED 投票加载失败",
                    message = state.error ?: "请稍后重试",
                    actionLabel = "重新加载",
                    onAction = viewModel::refresh,
                )
            }
        }

        else -> {
            EdVotingContent(
                campaign = state.campaign!!,
                state = state,
                onLogin = onLogin,
                onOpenSong = onOpenSong,
                onOpenShop = onOpenShop,
                onVoteExpired = viewModel::markVoteExpired,
                onBvidChange = viewModel::updateBvid,
                onNameChange = viewModel::updateName,
                onVocalistChange = viewModel::updateVocalist,
                onProducerChange = viewModel::updateProducer,
                onSubmitNomination = viewModel::submitNomination,
                onWithdrawNomination = viewModel::withdrawNomination,
                onTogglePositive = viewModel::togglePositive,
                onToggleNegative = viewModel::toggleNegative,
                onAddBonus = viewModel::addBonusVote,
                onRemoveBonus = viewModel::removeBonusVote,
                onBonusNegativeChange = viewModel::setBonusNegative,
                onSubmit = viewModel::submit,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun EdVotingContent(
    campaign: EdCampaignDto,
    state: EdVotingUiState,
    onLogin: () -> Unit,
    onOpenSong: (Int) -> Unit,
    onOpenShop: (() -> Unit)?,
    onVoteExpired: () -> Unit,
    onBvidChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onVocalistChange: (String) -> Unit,
    onProducerChange: (String) -> Unit,
    onSubmitNomination: () -> Unit,
    onWithdrawNomination: () -> Unit,
    onTogglePositive: (Int) -> Unit,
    onToggleNegative: (Int) -> Unit,
    onAddBonus: (Int, Int) -> Unit,
    onRemoveBonus: (Int) -> Unit,
    onBonusNegativeChange: (Int, Boolean) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deadline = deadlineText(
        if (campaign.status == "voting") campaign.voteEnd else campaign.submitEnd,
    )
    val meta = buildList {
        campaign.acceptedCount?.let { add("$it 个候选") }
        state.voterCount.takeIf { it > 0 }?.let { add("$it 人投票") }
    }.joinToString(" · ")
    val overLimit = state.maxVotes > 0 && state.selectedCount > state.maxVotes
    var detailsOpen by remember { mutableStateOf(false) }
    var losersOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = if (state.isLoggedIn && state.votingOpen) 96.dp else 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column {
                    VotingCampaignHeader(
                        title = "${boardLabel(campaign.board)} · 第 ${campaign.issue} 期",
                        status = votingStatusLabel(campaign.status),
                        deadline = deadline,
                        meta = meta,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (campaign.status == "voting") {
                            VoteCountdown(
                                endTime = campaign.voteEnd,
                                modifier = Modifier.weight(1f),
                                onExpire = onVoteExpired,
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        EdRulesButton(config = state.mechanismConfig)
                        if (campaign.status == "finalized") {
                            Spacer(Modifier.width(6.dp))
                            TextButton(onClick = { detailsOpen = !detailsOpen }) {
                                Text(if (detailsOpen) "收起明细" else "查看明细")
                            }
                        }
                    }
                }
            }

            when {
                campaign.status == "nomination" -> {
                    nominationContent(
                        state = state,
                        onLogin = onLogin,
                        onOpenSong = onOpenSong,
                        onBvidChange = onBvidChange,
                        onNameChange = onNameChange,
                        onVocalistChange = onVocalistChange,
                        onProducerChange = onProducerChange,
                        onSubmitNomination = onSubmitNomination,
                        onWithdrawNomination = onWithdrawNomination,
                    )
                }

                campaign.status == "voting" -> {
                    votingContent(
                        state = state,
                        onLogin = onLogin,
                        onOpenSong = onOpenSong,
                        onTogglePositive = onTogglePositive,
                        onToggleNegative = onToggleNegative,
                        onAddBonus = onAddBonus,
                        onRemoveBonus = onRemoveBonus,
                        onBonusNegativeChange = onBonusNegativeChange,
                        overLimit = overLimit,
                        onOpenShop = onOpenShop,
                    )
                }

                campaign.status == "finalized" -> {
                    resultsContent(
                        state = state,
                        onOpenSong = onOpenSong,
                        detailsOpen = detailsOpen,
                        losersOpen = losersOpen,
                        onToggleLosers = { losersOpen = !losersOpen },
                    )
                }

                else -> {
                    item {
                        VotingStatusCard(
                            title = "当前不可投票",
                            message = "请稍后再试。",
                        )
                    }
                }
            }
        }

        if (state.isLoggedIn && state.votingOpen) {
            VotingSubmitBar(
                text = buildString {
                    if (state.maxVotes > 0) {
                        append("已选 ${state.selectedCount} / ${state.maxVotes}")
                    } else {
                        append("已选 ${state.selectedCount}")
                    }
                    if (state.allowBonusVote) {
                        append(" · 附加票剩余 ${state.bonusRemaining}")
                    }
                },
                actionLabel = "保存投票",
                enabled = state.dirty && !overLimit,
                submitting = state.submitting,
                onSubmit = onSubmit,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.nominationContent(
    state: EdVotingUiState,
    onLogin: () -> Unit,
    onOpenSong: (Int) -> Unit,
    onBvidChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onVocalistChange: (String) -> Unit,
    onProducerChange: (String) -> Unit,
    onSubmitNomination: () -> Unit,
    onWithdrawNomination: () -> Unit,
) {
    if (!state.isLoggedIn) {
        item { VotingLoginCard(onLogin = onLogin) }
        item {
            VotingStatusCard(
                title = phaseTitle(state),
                message = "登录后可以提交提名并查看候选池。",
            )
        }
        return
    }

    val myNomination = state.myNomination
    if (myNomination != null) {
        item {
            Text(
                text = "我的提名",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        item {
            EdNominationCandidateRow(
                candidate = myNomination,
                onOpenSong = onOpenSong,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onWithdrawNomination,
                    enabled = !state.withdrawing && state.nominationOpen,
                ) {
                    Text(if (state.withdrawing) "撤回中…" else "撤回提名")
                }
            }
        }
        state.nominationNotice?.takeIf { it.isNotBlank() }?.let { notice ->
            item { VotingInlineMessage(text = notice, isError = false) }
        }
        state.nominationError?.takeIf { it.isNotBlank() }?.let { error ->
            item { VotingInlineMessage(text = error, isError = true) }
        }
    } else if (state.nominationOpen) {
        item {
            EdNominationForm(
                state = state,
                onBvidChange = onBvidChange,
                onNameChange = onNameChange,
                onVocalistChange = onVocalistChange,
                onProducerChange = onProducerChange,
            )
        }
        item {
            Button(
                onClick = onSubmitNomination,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSubmitNomination &&
                    !state.nominationSubmitting &&
                    !state.eligibilityLoading,
            ) {
                Text(if (state.nominationSubmitting) "提交中…" else "提交提名")
            }
        }
        state.nominationNotice?.takeIf { it.isNotBlank() }?.let { notice ->
            item { VotingInlineMessage(text = notice, isError = false) }
        }
        state.nominationError?.takeIf { it.isNotBlank() }?.let { error ->
            item { VotingInlineMessage(text = error, isError = true) }
        }
    } else {
        item {
            VotingStatusCard(
                title = "提名已结束",
                message = "本期提名阶段已结束，等待进入投票。",
            )
        }
    }

    if (state.nominationCandidatesError?.isNotBlank() == true) {
        item {
            VotingInlineMessage(
                text = state.nominationCandidatesError,
                isError = true,
            )
        }
    }
    item {
        Text(
            text = "候选池" + if (state.nominationCandidates.isNotEmpty()) {
                " · ${state.nominationCandidates.size}"
            } else {
                ""
            },
            style = MaterialTheme.typography.titleMedium,
        )
    }
    if (state.nominationCandidatesLoading) {
        item { InlineLoading() }
    } else if (
        state.nominationCandidates.isEmpty() &&
        state.nominationCandidatesError.isNullOrBlank()
    ) {
        item { EmptyHint(text = "暂无提名候选") }
    } else {
        items(state.nominationCandidates, key = { it.id }) { candidate ->
            EdNominationCandidateRow(
                candidate = candidate,
                onOpenSong = onOpenSong,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.votingContent(
    state: EdVotingUiState,
    onLogin: () -> Unit,
    onOpenSong: (Int) -> Unit,
    onTogglePositive: (Int) -> Unit,
    onToggleNegative: (Int) -> Unit,
    onAddBonus: (Int, Int) -> Unit,
    onRemoveBonus: (Int) -> Unit,
    onBonusNegativeChange: (Int, Boolean) -> Unit,
    overLimit: Boolean,
    onOpenShop: (() -> Unit)?,
) {
    if (!state.votingOpen) {
        item {
            VotingStatusCard(
                title = "投票已结束",
                message = "本期投票时间已结束，结果结算后会展示在这里。",
            )
        }
        return
    }
    if (!state.isLoggedIn) {
        item { VotingLoginCard(onLogin = onLogin) }
        return
    }

    item {
        Text(
            text = if (state.maxVotes > 0) {
                "已选 ${state.selectedCount} / ${state.maxVotes}"
            } else {
                "已选 ${state.selectedCount}"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (state.allowBonusVote) {
        item {
            Column {
                Text(
                    text = if (state.walletLoading) {
                        "附加票加载中…"
                    } else {
                        "附加票预算 ${state.bonusBudget}，剩余 ${state.bonusRemaining}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!state.walletLoading && state.bonusRemaining <= 0 && onOpenShop != null) {
                    TextButton(onClick = onOpenShop) {
                        Text("附加票不足，去商城购买")
                    }
                }
            }
        }
    }
    if (overLimit) {
        item {
            VotingInlineMessage(
                text = "已超过最多 ${state.maxVotes} 项的投票上限，请取消部分选择。",
                isError = true,
            )
        }
    }
    state.candidateError?.takeIf { it.isNotBlank() }?.let { error ->
        item { VotingInlineMessage(text = error, isError = true) }
    }
    if (state.candidateLoading) {
        item { InlineLoading() }
    } else if (state.candidates.isEmpty() && state.candidateError.isNullOrBlank()) {
        item { EmptyHint(text = "暂无候选") }
    } else {
        items(state.candidates, key = { it.candidateId }) { candidate ->
            val bonus = state.bonusVotes[candidate.candidateId]
            EdCandidateRow(
                candidate = candidate,
                selectedPositive = candidate.candidateId in state.positive,
                selectedNegative = candidate.candidateId in state.negative,
                enabled = !state.submitting,
                allowNegative = state.allowNegativeVote,
                onPositive = { onTogglePositive(candidate.candidateId) },
                onNegative = { onToggleNegative(candidate.candidateId) },
                onOpenSong = onOpenSong,
                bonusCount = bonus?.count ?: 0,
                bonusIsNegative = bonus?.isNegative == true,
                bonusRemaining = state.bonusRemaining,
                showBonus = state.allowBonusVote,
                onAddBonus = { onAddBonus(candidate.candidateId, 1) },
                onRemoveBonus = { onRemoveBonus(candidate.candidateId) },
                onBonusNegativeChange = { negative ->
                    onBonusNegativeChange(candidate.candidateId, negative)
                },
            )
        }
    }
    state.notice?.takeIf { it.isNotBlank() }?.let { notice ->
        item { VotingInlineMessage(text = notice, isError = false) }
    }
    state.submitError?.takeIf { it.isNotBlank() }?.let { error ->
        item { VotingInlineMessage(text = error, isError = true) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.resultsContent(
    state: EdVotingUiState,
    onOpenSong: (Int) -> Unit,
    detailsOpen: Boolean,
    losersOpen: Boolean,
    onToggleLosers: () -> Unit,
) {
    state.resultsError?.takeIf { it.isNotBlank() }?.let { error ->
        item { VotingInlineMessage(text = error, isError = true) }
    }
    if (state.resultsLoading) {
        item { InlineLoading() }
    }

    val categoryOrder = listOf("weekly", "monthly", "daily")
    val grouped = categoryOrder.mapNotNull { category ->
        state.results
            .filter { it.category == category }
            .takeIf { it.isNotEmpty() }
            ?.let { category to it }
    }
    val hasSelected = grouped.isNotEmpty()

    grouped.forEach { (category, categoryItems) ->
        item(key = "result-header-$category") {
            Text(
                text = categoryResultLabel(category),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        itemsIndexed(
            categoryItems.sortedBy { it.rank },
            key = { _, result -> "$category-${result.rank}-${result.candidate.id}" },
        ) { index, result ->
            EdResultItem(
                result = result,
                onOpenSong = onOpenSong,
                isWinner = index == 0,
                detailsOpen = detailsOpen,
                negativeWeight = state.mechanismConfig.negativeWeight,
            )
        }
    }

    val selectedIds = state.results.mapTo(mutableSetOf()) { it.candidate.id }
    val losers = state.ranking
        .filterNot { it.candidate.id in selectedIds }
        .sortedBy { it.rank }
    if (losers.isNotEmpty()) {
        item(key = "losers-header") {
            TextButton(
                onClick = onToggleLosers,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (losersOpen) {
                        "收起落选（${losers.size}）"
                    } else {
                        "展开落选（${losers.size}）"
                    },
                )
            }
        }
        if (losersOpen) {
            items(losers, key = { "loser-${it.rank}-${it.candidate.id}" }) { result ->
                EdResultItem(
                    result = result,
                    onOpenSong = onOpenSong,
                    detailsOpen = detailsOpen,
                    negativeWeight = state.mechanismConfig.negativeWeight,
                )
            }
        }
    }

    if (!hasSelected && losers.isEmpty() && !state.resultsLoading) {
        item { EmptyHint(text = "暂无结果") }
    }
}

private fun categoryResultLabel(category: String): String = when (category) {
    "weekly" -> "周刊结果"
    "monthly" -> "月刊结果"
    "daily" -> "日刊结果"
    "loser" -> "落选"
    else -> category.ifBlank { "投票结果" }
}

@Composable
private fun EdNominationForm(
    state: EdVotingUiState,
    onBvidChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onVocalistChange: (String) -> Unit,
    onProducerChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "提交提名",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "填写 BV 号后会先检查是否符合本期提名条件。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.bvid,
            onValueChange = onBvidChange,
            label = { Text("BV 号") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.nominationSubmitting,
        )
        if (state.eligibilityLoading) {
            InlineLoading()
        }
        state.eligibilityError?.takeIf { it.isNotBlank() }?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        val eligibility = state.eligibility
        if (eligibility != null) {
            if (!eligibility.canSubmit) {
                Text(
                    text = eligibility.reasons.joinToString("；")
                        .ifBlank { "该视频不符合本期提名条件" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (eligibility.songId == null) {
                Text(
                    text = "未匹配到歌曲库，请补充歌曲信息。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("歌曲名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.nominationSubmitting,
                )
                OutlinedTextField(
                    value = state.vocalist,
                    onValueChange = onVocalistChange,
                    label = { Text("歌手") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.nominationSubmitting,
                )
                OutlinedTextField(
                    value = state.producer,
                    onValueChange = onProducerChange,
                    label = { Text("P主") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.nominationSubmitting,
                )
            } else {
                Text(
                    text = "已匹配到歌曲库。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun phaseTitle(state: EdVotingUiState): String = when {
    state.nominationOpen -> "提名进行中"
    state.campaign?.status == "finalized" -> "投票已结束"
    state.campaign?.status == "voting" && !state.votingOpen -> "投票已结束"
    else -> "当前不可投票"
}
