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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.R
import xyz.lisfox.vocaland.data.notification.NotificationEntryDto
import xyz.lisfox.vocaland.data.notification.NotificationKind
import xyz.lisfox.vocaland.data.notification.NotificationRepository
import xyz.lisfox.vocaland.data.notification.notificationPreviewText
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.MotionDurations
import xyz.lisfox.vocaland.ui.components.PagerRow
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.ui.components.SegmentedTabs
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
            title = stringResource(R.string.notifications_login_title),
            message = stringResource(R.string.notifications_login_message),
            modifier = modifier,
            actionLabel = if (onLogin != null) stringResource(R.string.action_login) else null,
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
                title = "最新动态",
                subtitle = if (state.unread > 0) {
                    stringResource(R.string.notifications_unread_count_format, state.unread)
                } else {
                    stringResource(R.string.notifications_unread_none_subtitle)
                },
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = viewModel::refresh) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.action_refresh),
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
                                Text(stringResource(R.string.notifications_mark_all_read))
                            }
                        }
                    }
                },
            )
        }

        item {
            val unreadLabel = if (state.unread > 0) {
                stringResource(R.string.notifications_tab_unread_format, state.unread)
            } else {
                stringResource(R.string.notifications_tab_unread)
            }
            val allLabel = stringResource(R.string.notifications_tab_all)
            SegmentedTabs(
                options = NotificationTab.entries,
                selected = state.tab,
                label = { tab ->
                    when (tab) {
                        NotificationTab.Unread -> unreadLabel
                        NotificationTab.All -> allLabel
                    }
                },
                onSelect = viewModel::setTab,
            )
        }

        when {
            state.loading && state.entries.isEmpty() -> {
                item {
                    LoadingPane(
                        modifier = Modifier.height(240.dp),
                        text = stringResource(R.string.notifications_loading),
                    )
                }
            }

            state.error != null && state.entries.isEmpty() -> {
                item {
                    ScreenMessage(
                        title = stringResource(R.string.notifications_load_failed_title),
                        message = state.error!!,
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = viewModel::refresh,
                    )
                }
            }

            state.entries.isEmpty() -> {
                item {
                    EmptyHint(
                        text = if (state.tab == NotificationTab.Unread) {
                            stringResource(R.string.notifications_empty_unread)
                        } else {
                            stringResource(R.string.notifications_empty_all)
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
                PagerRow(
                    page = state.page,
                    totalPages = state.pages,
                    loading = state.loading,
                    onPrevious = { viewModel.setPage(state.page - 1) },
                    onNext = { viewModel.setPage(state.page + 1) },
                    total = state.total,
                    unit = "条",
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
    val systemSender = stringResource(R.string.notifications_system_sender)
    val actorName = entry.actor?.nickname?.ifBlank { systemSender } ?: systemSender
    val preview = notificationPreviewText(entry.contentPreview)
    val containerColor by animateColorAsState(
        targetValue = if (entry.isRead) {
            scheme.surfaceContainerLow
        } else {
            scheme.primaryContainer.copy(alpha = 0.45f)
        },
        animationSpec = tween(MotionDurations.FAST),
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
                    fallbackIcon = Icons.Filled.Person,
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

