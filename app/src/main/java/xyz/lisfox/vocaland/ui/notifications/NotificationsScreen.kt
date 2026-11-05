package xyz.lisfox.vocaland.ui.notifications

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.notification.NotificationEntryDto
import xyz.lisfox.vocaland.data.notification.NotificationKind
import xyz.lisfox.vocaland.data.notification.NotificationRepository
import xyz.lisfox.vocaland.data.notification.notificationPreviewText
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.MotionDurations
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.util.timeAgo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationsScreen(
    repository: NotificationRepository,
    isLoggedIn: Boolean,
    onOpenNotification: (targetKey: String?, commentId: Int?) -> Unit,
    onLogin: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel: NotificationsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { NotificationsViewModel(repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is NotificationEvent.Open) {
                onOpenNotification(event.targetKey, event.commentId)
            }
        }
    }

    if (!isLoggedIn) {
        ScreenMessage(
            title = "登录后查看通知",
            message = "有人回复、点赞或提到你时，通知会出现在这里。",
            modifier = modifier,
            actionLabel = if (onLogin != null) "去登录" else null,
            onAction = onLogin,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "消息中心",
                subtitle = if (state.unread > 0) {
                    "${state.unread} 条未读通知"
                } else {
                    "暂时没有未读通知"
                },
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = viewModel::refresh) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "刷新",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        if (state.unread > 0) {
                            TextButton(
                                onClick = viewModel::markAllRead,
                                enabled = !state.markingAll,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DoneAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(4.dp))
                                Text("全部已读")
                            }
                        }
                    }
                },
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.tab == NotificationTab.Unread,
                    onClick = { viewModel.setTab(NotificationTab.Unread) },
                    label = {
                        Text(
                            if (state.unread > 0) "未读 (${state.unread})" else "未读",
                        )
                    },
                )
                FilterChip(
                    selected = state.tab == NotificationTab.All,
                    onClick = { viewModel.setTab(NotificationTab.All) },
                    label = { Text("全部") },
                )
            }
        }

        when {
            state.loading && state.entries.isEmpty() -> {
                item {
                    LoadingPane(
                        modifier = Modifier.height(240.dp),
                        text = "正在加载通知",
                    )
                }
            }

            state.error != null && state.entries.isEmpty() -> {
                item {
                    ScreenMessage(
                        title = "通知加载失败",
                        message = state.error!!,
                        actionLabel = "重试",
                        onAction = viewModel::refresh,
                    )
                }
            }

            state.entries.isEmpty() -> {
                item {
                    EmptyHint(
                        text = if (state.tab == NotificationTab.Unread) {
                            "没有未读通知"
                        } else {
                            "暂时没有通知"
                        },
                    )
                }
            }

            else -> {
                items(state.entries, key = { it.id }, contentType = { "notification" }) { entry ->
                    NotificationCard(
                        entry = entry,
                        onClick = { viewModel.open(entry) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        if (state.total > 0) {
            item {
                PaginationRow(
                    page = state.page,
                    pages = state.pages,
                    onPrevious = { viewModel.setPage(state.page - 1) },
                    onNext = { viewModel.setPage(state.page + 1) },
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    entry: NotificationEntryDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val kind = NotificationKind.fromApi(entry.type) ?: NotificationKind.Reply
    val actorName = entry.actor?.nickname?.ifBlank { "系统消息" } ?: "系统消息"
    val preview = notificationPreviewText(entry.contentPreview)
    val containerColor by animateColorAsState(
        targetValue = if (entry.isRead) {
            scheme.surfaceContainerLow
        } else {
            scheme.primaryContainer.copy(alpha = 0.45f)
        },
        animationSpec = tween(MotionDurations.MEDIUM),
        label = "notificationContainer",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(modifier = Modifier.size(42.dp)) {
                NetworkImage(
                    url = entry.actor?.avatar,
                    contentDescription = actorName,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape),
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp),
                    shape = CircleShape,
                    color = scheme.secondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = notificationIcon(kind),
                            contentDescription = null,
                            tint = scheme.onSecondaryContainer,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.size(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$actorName ${kind.verb}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (preview.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val time = timeAgo(entry.createdAt)
                    if (time.isNotBlank()) {
                        Text(
                            text = time,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    if (!entry.targetName.isNullOrBlank()) {
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Text(
                            text = entry.targetName,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (!entry.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, start = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                )
            }
        }
    }
}

@Composable
private fun PaginationRow(
    page: Int,
    pages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onPrevious,
            enabled = page > 1,
        ) {
            Text("上一页")
        }
        Text(
            text = "$page / $pages",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onNext,
            enabled = page < pages,
        ) {
            Text("下一页")
        }
    }
}
