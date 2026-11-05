package xyz.lisfox.vocaland.ui.voting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.SearchEntityDto
import xyz.lisfox.vocaland.data.model.TRENDING_NOMINATION_LIMIT
import xyz.lisfox.vocaland.data.model.TrendingCampaignDto
import xyz.lisfox.vocaland.data.model.TrendingCandidateDto
import xyz.lisfox.vocaland.data.repository.VotingRepository
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.util.deadlineText
import xyz.lisfox.vocaland.util.trendingEntityKindLabel
import xyz.lisfox.vocaland.util.votingStatusLabel

@Composable
fun TrendingVotingScreen(
    repository: VotingRepository,
    session: AuthSession,
    campaignId: Int,
    onLogin: () -> Unit,
    onOpenSong: (Int) -> Unit,
    onOpenArtist: (ArtistKind, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TrendingVotingViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TrendingVotingViewModel(repository, session, campaignId) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.loading && state.campaign == null -> {
            LoadingPane(modifier = modifier, text = "正在加载人气投票…")
        }

        state.campaign == null -> {
            Box(modifier = modifier.padding(16.dp)) {
                ScreenMessage(
                    title = "人气投票加载失败",
                    message = state.error ?: "请稍后重试",
                    actionLabel = "重新加载",
                    onAction = viewModel::refresh,
                )
            }
        }

        else -> {
            TrendingVotingContent(
                campaign = state.campaign!!,
                state = state,
                onLogin = onLogin,
                onOpenSong = onOpenSong,
                onOpenArtist = onOpenArtist,
                onToggle = viewModel::toggle,
                onSubmit = viewModel::submit,
                onEntityQueryChange = viewModel::updateEntityQuery,
                onNominate = viewModel::submitNomination,
                onWithdrawNomination = viewModel::withdrawNomination,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun TrendingVotingContent(
    campaign: TrendingCampaignDto,
    state: TrendingVotingUiState,
    onLogin: () -> Unit,
    onOpenSong: (Int) -> Unit,
    onOpenArtist: (ArtistKind, Int) -> Unit,
    onToggle: (Int) -> Unit,
    onSubmit: () -> Unit,
    onEntityQueryChange: (String) -> Unit,
    onNominate: (SearchEntityDto) -> Unit,
    onWithdrawNomination: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val deadline = deadlineText(
        if (campaign.status == "voting") campaign.voteEnd else campaign.submitEnd,
    )
    val meta = buildList {
        (campaign.candidateCount ?: state.candidates.size)
            .takeIf { it > 0 }?.let { add("$it 个候选") }
        state.voterCount.takeIf { it > 0 }?.let { add("$it 人投票") }
        state.totalVotes.takeIf { it > 0 }?.let { add("$it 票") }
    }.joinToString(" · ")
    val canVote = state.isLoggedIn && state.votingOpen

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = if (canVote) 96.dp else 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                VotingCampaignHeader(
                    title = "第 ${campaign.issue} 期 · ${trendingEntityKindLabel(campaign.entityKind)}",
                    status = votingStatusLabel(campaign.status),
                    deadline = deadline,
                    meta = meta,
                )
            }

            when (campaign.status) {
                "nomination" -> {
                    nominationContent(
                        state = state,
                        onLogin = onLogin,
                        onOpenSong = onOpenSong,
                        onOpenArtist = onOpenArtist,
                        onEntityQueryChange = onEntityQueryChange,
                        onNominate = onNominate,
                        onWithdrawNomination = onWithdrawNomination,
                    )
                }

                "voting" -> {
                    votingContent(
                        state = state,
                        onLogin = onLogin,
                        onOpenSong = onOpenSong,
                        onOpenArtist = onOpenArtist,
                        onToggle = onToggle,
                    )
                }

                "finalized" -> {
                    resultsContent(state = state, onOpenSong = onOpenSong, onOpenArtist = onOpenArtist)
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

        if (canVote) {
            VotingSubmitBar(
                text = if (state.maxVotes > 0) {
                    "已选 ${state.selected.size} / ${state.maxVotes}"
                } else {
                    "已选 ${state.selected.size}"
                },
                actionLabel = "保存投票",
                enabled = state.dirty && !(state.maxVotes > 0 && state.selected.size > state.maxVotes),
                submitting = state.submitting,
                onSubmit = onSubmit,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.nominationContent(
    state: TrendingVotingUiState,
    onLogin: () -> Unit,
    onOpenSong: (Int) -> Unit,
    onOpenArtist: (ArtistKind, Int) -> Unit,
    onEntityQueryChange: (String) -> Unit,
    onNominate: (SearchEntityDto) -> Unit,
    onWithdrawNomination: (Int) -> Unit,
) {
    if (!state.isLoggedIn) {
        item { VotingLoginCard(onLogin = onLogin) }
    } else {
        item {
            Text(
                text = "我的提名 · ${state.myNominations.size}/$TRENDING_NOMINATION_LIMIT",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (state.nominationsLoading) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        } else if (state.myNominations.isEmpty()) {
            item { EmptyHint(text = "还没有提名") }
        } else {
            items(state.myNominations, key = { "mine-${it.id}" }) { candidate ->
                TrendingNominationRow(
                    candidate = candidate,
                    withdrawing = state.withdrawingId == candidate.id,
                    showWithdraw = state.nominationOpen,
                    onWithdraw = { onWithdrawNomination(candidate.id) },
                    onOpenSong = onOpenSong,
                    onOpenArtist = onOpenArtist,
                )
            }
        }
        state.nominationsError?.takeIf { it.isNotBlank() }?.let { error ->
            item { VotingInlineMessage(text = error, isError = true) }
        }

        if (state.nominationOpen) {
            if (state.nominationAtLimit) {
                item {
                    VotingInlineMessage(
                        text = "已达到 $TRENDING_NOMINATION_LIMIT 个提名上限，撤回后可继续提名。",
                        isError = false,
                    )
                }
            } else {
                item {
                    Text(
                        text = "搜索并提名",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.entityQuery,
                        onValueChange = onEntityQueryChange,
                        label = { Text("搜索${trendingEntityKindLabel(state.campaign?.entityKind.orEmpty())}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.nominating,
                    )
                }
                if (state.entitySearchLoading) {
                    item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                }
                state.entitySearchError?.takeIf { it.isNotBlank() }?.let { error ->
                    item { VotingInlineMessage(text = error, isError = true) }
                }
                if (
                    state.entityQuery.isNotBlank() &&
                    state.entityResults.isEmpty() &&
                    !state.entitySearchLoading &&
                    state.entitySearchError.isNullOrBlank()
                ) {
                    item { EmptyHint(text = "没有找到匹配的条目") }
                }
                items(state.entityResults, key = { "search-${it.id}" }) { entity ->
                    TrendingEntitySearchRow(
                        entity = entity,
                        nominating = state.nominating,
                        onNominate = { onNominate(entity) },
                    )
                }
            }
        } else {
            item {
                VotingStatusCard(
                    title = "提名已结束",
                    message = "本期提名阶段已结束，等待进入投票。",
                )
            }
        }

        state.nominationNotice?.takeIf { it.isNotBlank() }?.let { notice ->
            item { VotingInlineMessage(text = notice, isError = false) }
        }
        state.nominationError?.takeIf { it.isNotBlank() }?.let { error ->
            item { VotingInlineMessage(text = error, isError = true) }
        }
    }

    val mineIds = state.myNominations.mapTo(mutableSetOf()) { it.id }
    val others = state.candidates.filterNot { it.id in mineIds }
    item {
        Text(
            text = "全部提名" + if (state.candidates.isNotEmpty()) {
                " · ${state.candidates.size}"
            } else {
                ""
            },
            style = MaterialTheme.typography.titleMedium,
        )
    }
    if (state.nominationsLoading) {
        item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
    } else if (others.isEmpty()) {
        item { EmptyHint(text = "暂无提名候选") }
    } else {
        items(others, key = { "all-${it.id}" }) { candidate ->
            TrendingNominationRow(
                candidate = candidate,
                withdrawing = false,
                showWithdraw = false,
                onOpenSong = onOpenSong,
                onOpenArtist = onOpenArtist,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.votingContent(
    state: TrendingVotingUiState,
    onLogin: () -> Unit,
    onOpenSong: (Int) -> Unit,
    onOpenArtist: (ArtistKind, Int) -> Unit,
    onToggle: (Int) -> Unit,
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

    val overLimit = state.maxVotes > 0 && state.selected.size > state.maxVotes
    item {
        Text(
            text = if (state.maxVotes > 0) {
                "已选 ${state.selected.size} / ${state.maxVotes}"
            } else {
                "已选 ${state.selected.size}"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
    } else {
        val candidates = state.candidates.sortedWith(
            compareByDescending<TrendingCandidateDto> { state.tally[it.id] ?: 0 }
                .thenBy { it.id },
        )
        if (candidates.isEmpty() && state.candidateError.isNullOrBlank()) {
            item { EmptyHint(text = "暂无候选") }
        } else {
            itemsIndexed(
                items = candidates,
                key = { _, candidate -> "vote-${candidate.id}" },
            ) { index, candidate ->
                TrendingCandidateRow(
                    candidate = candidate,
                    rank = index + 1,
                    voteCount = state.tally[candidate.id] ?: 0,
                    selected = candidate.id in state.selected,
                    canVote = true,
                    onToggle = { onToggle(candidate.id) },
                    onOpenSong = onOpenSong,
                    onOpenArtist = onOpenArtist,
                )
            }
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
    state: TrendingVotingUiState,
    onOpenSong: (Int) -> Unit,
    onOpenArtist: (ArtistKind, Int) -> Unit,
) {
    state.resultsError?.takeIf { it.isNotBlank() }?.let { error ->
        item { VotingInlineMessage(text = error, isError = true) }
    }
    if (state.resultsLoading) {
        item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
    }
    if (state.results.isEmpty() && !state.resultsLoading) {
        item { EmptyHint(text = "暂无结果") }
    } else {
        items(state.results, key = { "result-${it.rank}-${it.candidate.id}" }) { result ->
            TrendingResultItem(
                result = result,
                onOpenSong = onOpenSong,
                onOpenArtist = onOpenArtist,
            )
        }
    }
}
