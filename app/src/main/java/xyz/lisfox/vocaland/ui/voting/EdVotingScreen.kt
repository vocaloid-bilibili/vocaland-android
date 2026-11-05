package xyz.lisfox.vocaland.ui.voting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
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
import xyz.lisfox.vocaland.data.repository.VotingRepository
import xyz.lisfox.vocaland.data.session.AuthSession
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
                onTogglePositive = viewModel::togglePositive,
                onToggleNegative = viewModel::toggleNegative,
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
    onTogglePositive: (Int) -> Unit,
    onToggleNegative: (Int) -> Unit,
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
    val candidateError = state.candidateError
    val notice = state.notice
    val submitError = state.submitError

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
                VotingCampaignHeader(
                    title = "${boardLabel(campaign.board)} · 第 ${campaign.issue} 期",
                    status = votingStatusLabel(campaign.status),
                    deadline = deadline,
                    meta = meta,
                )
            }

            if (state.candidateLoading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            if (state.campaign?.status == "voting" && state.votingOpen) {
                if (!state.isLoggedIn) {
                    item { VotingLoginCard(onLogin = onLogin) }
                } else {
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
                    if (overLimit) {
                        item {
                            VotingInlineMessage(
                                text = "已超过最多 ${state.maxVotes} 项的投票上限，请取消部分选择。",
                                isError = true,
                            )
                        }
                    }
                    if (!candidateError.isNullOrBlank()) {
                        item {
                            VotingInlineMessage(
                                text = candidateError,
                                isError = true,
                            )
                        }
                    }
                    if (
                        state.candidates.isEmpty() &&
                        !state.candidateLoading &&
                        candidateError.isNullOrBlank()
                    ) {
                        item { EmptyHint(text = "暂无候选") }
                    } else {
                        items(state.candidates, key = { it.candidateId }) { candidate ->
                            EdCandidateRow(
                                candidate = candidate,
                                selectedPositive = candidate.candidateId in state.positive,
                                selectedNegative = candidate.candidateId in state.negative,
                                enabled = !state.submitting,
                                onPositive = { onTogglePositive(candidate.candidateId) },
                                onNegative = { onToggleNegative(candidate.candidateId) },
                                onOpenSong = onOpenSong,
                            )
                        }
                    }
                }
            } else {
                item {
                    VotingStatusCard(
                        title = phaseTitle(campaign, state),
                        message = phaseMessage(campaign, state),
                    )
                }
            }

            if (!notice.isNullOrBlank()) {
                item {
                    VotingInlineMessage(text = notice, isError = false)
                }
            }
            if (!submitError.isNullOrBlank()) {
                item {
                    VotingInlineMessage(text = submitError, isError = true)
                }
            }
        }

        if (state.isLoggedIn && state.votingOpen) {
            VotingSubmitBar(
                text = if (state.maxVotes > 0) {
                    "已选 ${state.selectedCount} / ${state.maxVotes}"
                } else {
                    "已选 ${state.selectedCount}"
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

private fun phaseTitle(campaign: EdCampaignDto, state: EdVotingUiState): String = when {
    campaign.status == "nomination" -> "提名进行中"
    campaign.status == "finalized" -> "投票已结束"
    campaign.status == "voting" && !state.votingOpen -> "投票已结束"
    else -> "当前不可投票"
}

private fun phaseMessage(campaign: EdCampaignDto, state: EdVotingUiState): String = when {
    campaign.status == "nomination" -> "投票开始后可在这里选择赞成或反对。"
    campaign.status == "finalized" -> "本期投票已结算。"
    campaign.status == "voting" && !state.votingOpen -> "本期投票时间已结束。"
    else -> "请稍后再试。"
}
