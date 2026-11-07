package xyz.lisfox.vocaland.ui.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.lisfox.vocaland.R
import xyz.lisfox.vocaland.data.community.CommunityCommentDto
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.util.timeAgo

private data class ReplyTarget(
    val rootId: Int,
    val replyToId: Int,
)

@Composable
internal fun CommunityCommentSection(
    state: CommunityUiState,
    viewModel: CommunityViewModel,
    onLogin: () -> Unit,
    onOpenUser: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (!state.isLoggedIn) {
        CommunityLoginCard(
            message = "登录后即可查看与参与评论。",
            onLogin = onLogin,
            modifier = modifier,
        )
        return
    }

    var draft by remember(state.targetKey) { mutableStateOf("") }
    var replyTarget by remember(state.targetKey) { mutableStateOf<ReplyTarget?>(null) }

    LaunchedEffect(state.commentResetKey) {
        if (state.commentResetKey > 0) {
            draft = ""
            replyTarget = null
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CommentComposer(
            value = draft,
            onValueChange = { draft = it.take(COMMUNITY_COMMENT_MAX) },
            placeholder = "写下你的看法…",
            posting = state.commentPosting,
            onSend = { viewModel.postComment(draft) },
        )

        state.commentActionError?.let { error ->
            CommunityInlineMessage(text = error, isError = true)
        }

        when {
            state.commentsLoading && state.comments.isEmpty() -> {
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
                        text = "评论加载中",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            state.commentsError != null && state.comments.isEmpty() -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CommunityInlineMessage(
                        text = state.commentsError ?: "评论加载失败",
                        isError = true,
                    )
                    TextButton(onClick = viewModel::retryComments) {
                        Text("重试")
                    }
                }
            }

            state.comments.isEmpty() -> {
                EmptyHint("还没有评论，来抢沙发")
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.comments.forEach { comment ->
                        CommentThread(
                            comment = comment,
                            rootId = comment.id,
                            replyTarget = replyTarget,
                            state = state,
                            onOpenUser = onOpenUser,
                            onReply = { rootId, replyToId ->
                                replyTarget = ReplyTarget(rootId, replyToId)
                            },
                            onCancelReply = { replyTarget = null },
                            onPostReply = { content, target ->
                                viewModel.postComment(
                                    content = content,
                                    parentId = target.rootId,
                                    replyToId = target.replyToId,
                                )
                            },
                            onLike = viewModel::toggleCommentLike,
                            onDelete = viewModel::deleteComment,
                        )
                    }
                }
            }
        }

        CommunityPager(
            page = state.commentsPage,
            totalPages = state.commentsTotalPages,
            loading = state.commentsLoading,
            onPrevious = viewModel::previousCommentsPage,
            onNext = viewModel::nextCommentsPage,
        )
    }
}

@Composable
private fun CommentThread(
    comment: CommunityCommentDto,
    rootId: Int,
    replyTarget: ReplyTarget?,
    state: CommunityUiState,
    onOpenUser: ((Int) -> Unit)?,
    onReply: (Int, Int) -> Unit,
    onCancelReply: () -> Unit,
    onPostReply: (String, ReplyTarget) -> Unit,
    onLike: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            CommentItem(
                comment = comment,
                rootId = rootId,
                isNested = false,
                replyTarget = replyTarget,
                state = state,
                onOpenUser = onOpenUser,
                onReply = onReply,
                onCancelReply = onCancelReply,
                onPostReply = onPostReply,
                onLike = onLike,
                onDelete = onDelete,
            )

            if (!comment.isDeleted && comment.replies.isNotEmpty()) {
                val guideColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                Column(
                    modifier = Modifier
                        .drawBehind {
                            val lineWidth = 2.dp.toPx()
                            val inset = 4.dp.toPx()
                            drawRoundRect(
                                color = guideColor,
                                topLeft = Offset(inset, inset),
                                size = Size(lineWidth, size.height - inset * 2),
                                cornerRadius = CornerRadius(lineWidth / 2),
                            )
                        }
                        .padding(start = 26.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    comment.replies.forEach { reply ->
                        CommentItem(
                            comment = reply,
                            rootId = rootId,
                            isNested = true,
                            replyTarget = replyTarget,
                            state = state,
                            onOpenUser = onOpenUser,
                            onReply = onReply,
                            onCancelReply = onCancelReply,
                            onPostReply = onPostReply,
                            onLike = onLike,
                            onDelete = onDelete,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: CommunityCommentDto,
    rootId: Int,
    isNested: Boolean,
    replyTarget: ReplyTarget?,
    state: CommunityUiState,
    onOpenUser: ((Int) -> Unit)?,
    onReply: (Int, Int) -> Unit,
    onCancelReply: () -> Unit,
    onPostReply: (String, ReplyTarget) -> Unit,
    onLike: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val avatarSize = if (isNested) 26.dp else 34.dp
    val canOpenUser = comment.user.id > 0 && onOpenUser != null
    var confirmingDelete by remember(comment.id) { mutableStateOf(false) }
    val likePending = comment.id in state.likePendingIds
    val deleting = comment.id in state.deletingCommentIds
    val replyOpen = replyTarget?.replyToId == comment.id

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (isNested) 6.dp else 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NetworkImage(
            url = comment.user.avatar,
                    fallbackIcon = Icons.Filled.Person,
            contentDescription = comment.user.nickname,
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .clickable(enabled = canOpenUser) {
                    onOpenUser?.invoke(comment.user.id)
                },
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = comment.user.nickname.ifBlank { "用户 #${comment.user.id}" },
                    // 昵称必须参与权重分配：否则超长昵称会先占满整行，
                    // 后面的等级/时间被挤成 0 宽，文字竖排错位
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = canOpenUser) {
                            onOpenUser?.invoke(comment.user.id)
                        },
                    style = if (isNested) {
                        MaterialTheme.typography.bodyMedium
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusPill(text = "Lv.${comment.user.level}")
                timeAgo(comment.createdAt).takeIf { it.isNotBlank() }?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            comment.replyTo?.let { replyTo ->
                Text(
                    text = "回复 @${replyTo.nickname}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            if (comment.isDeleted) {
                Text(
                    text = "该评论已删除",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TextButton(
                        onClick = { onLike(comment.id) },
                        enabled = !likePending,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = if (comment.isLiked) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Filled.FavoriteBorder
                            },
                            contentDescription = stringResource(
                                if (comment.isLiked) {
                                    R.string.community_comment_unlike
                                } else {
                                    R.string.community_comment_like
                                },
                            ),
                            modifier = Modifier.size(18.dp),
                            tint = if (comment.isLiked) scheme.primary else scheme.onSurfaceVariant,
                        )
                        if (comment.likeCount > 0) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = comment.likeCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            onReply(rootId, comment.id)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("回复", style = MaterialTheme.typography.labelMedium)
                    }

                    if (comment.isAuthor) {
                        if (confirmingDelete) {
                            TextButton(
                                onClick = {
                                    confirmingDelete = false
                                    onDelete(comment.id)
                                },
                                enabled = !deleting,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = if (deleting) "删除中…" else "确认删除",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = scheme.error,
                                )
                            }
                        } else {
                            TextButton(
                                onClick = { confirmingDelete = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text("删除", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            if (replyOpen && !comment.isDeleted) {
                ReplyComposer(
                    nickname = comment.user.nickname,
                    posting = state.commentPosting,
                    onCancel = onCancelReply,
                    onSend = { content ->
                        replyTarget?.let { target ->
                            onPostReply(content, target)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CommentComposer(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    posting: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder) },
                minLines = 2,
                maxLines = 5,
                enabled = !posting,
                colors = commentFieldColors(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${value.length} / $COMMUNITY_COMMENT_MAX",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onSend,
                    enabled = value.isNotBlank() && !posting,
                    colors = commentSendButtonColors(),
                ) {
                    Text(if (posting) "发送中…" else "发送")
                }
            }
        }
    }
}

@Composable
private fun ReplyComposer(
    nickname: String,
    posting: Boolean,
    onCancel: () -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(COMMUNITY_COMMENT_MAX) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("回复 @$nickname") },
            minLines = 2,
            maxLines = 4,
            enabled = !posting,
            colors = commentFieldColors(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel, enabled = !posting) {
                Text("取消")
            }
            Button(
                onClick = {
                    if (text.isNotBlank()) onSend(text)
                },
                enabled = text.isNotBlank() && !posting,
                colors = commentSendButtonColors(),
            ) {
                Text(if (posting) "发送中…" else "回复")
            }
        }
    }
}

@Composable
private fun commentFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
)

@Composable
private fun commentSendButtonColors() = ButtonDefaults.buttonColors(
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)