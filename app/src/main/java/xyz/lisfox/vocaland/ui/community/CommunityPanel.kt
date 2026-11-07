package xyz.lisfox.vocaland.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.community.CommunityRepository
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.ui.components.SectionHeader

enum class CommunityTab(val label: String) {
    Ratings("评分"),
    Comments("评论"),
}

@Composable
fun CommunityPanel(
    repository: CommunityRepository,
    targetKey: String,
    session: AuthSession?,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenUser: ((Int) -> Unit)? = null,
    initialTab: CommunityTab = CommunityTab.Ratings,
    showRatings: Boolean = true,
    showComments: Boolean = true,
    title: String = "社区",
) {
    val tabs = buildList {
        if (showRatings) add(CommunityTab.Ratings)
        if (showComments) add(CommunityTab.Comments)
    }
    if (tabs.isEmpty()) return

    val firstTab = if (initialTab in tabs) initialTab else tabs.first()
    var selectedTab by remember(targetKey, showRatings, showComments) {
        mutableStateOf(firstTab)
    }

    val viewModel: CommunityViewModel = viewModel(
        key = "community-$targetKey-$showRatings-$showComments",
        factory = viewModelFactory {
            initializer {
                CommunityViewModel(
                    repository = repository,
                    session = session,
                    targetKey = targetKey,
                    includeRatings = showRatings,
                    includeComments = showComments,
                )
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(
            title = title,
            subtitle = when (selectedTab) {
                CommunityTab.Ratings -> state.ratingsTotal
                    .takeIf { it > 0 }
                    ?.let { "$it 条评分" }

                CommunityTab.Comments -> state.commentsTotal
                    .takeIf { it > 0 }
                    ?.let { "$it 条评论" }
            },
        )

        if (tabs.size > 1) {
            CommunityTabBar(
                tabs = tabs,
                selected = selectedTab,
                onSelect = { selectedTab = it },
            )
        }

        when (selectedTab) {
            CommunityTab.Ratings -> CommunityRatingSection(
                state = state,
                viewModel = viewModel,
                onLogin = onLogin,
                onOpenUser = onOpenUser,
            )

            CommunityTab.Comments -> CommunityCommentSection(
                state = state,
                viewModel = viewModel,
                onLogin = onLogin,
                onOpenUser = onOpenUser,
            )
        }
    }
}

@Composable
private fun CommunityTabBar(
    tabs: List<CommunityTab>,
    selected: CommunityTab,
    onSelect: (CommunityTab) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tabs.forEach { tab ->
                val active = selected == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) scheme.secondaryContainer else Color.Transparent)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
internal fun CommunityLoginCard(
    message: String,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onLogin) {
                Text("去登录")
            }
        }
    }
}

@Composable
internal fun CommunityInlineMessage(
    text: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isError) scheme.errorContainer else scheme.secondaryContainer,
        contentColor = if (isError) scheme.onErrorContainer else scheme.onSecondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
internal fun CommunityPager(
    page: Int,
    totalPages: Int,
    loading: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (totalPages <= 1) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onPrevious,
            enabled = page > 1 && !loading,
        ) {
            Text("上一页")
        }
        Text(
            text = "$page / $totalPages",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = onNext,
            enabled = page < totalPages && !loading,
        ) {
            Text("下一页")
        }
    }
}
