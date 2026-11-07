package xyz.lisfox.vocaland.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.BuildConfig
import xyz.lisfox.vocaland.R
import xyz.lisfox.vocaland.data.model.CachedUserDto
import xyz.lisfox.vocaland.data.preferences.ThemePreferences
import xyz.lisfox.vocaland.data.repository.AuthRepository
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.ui.account.roleLabel
import xyz.lisfox.vocaland.ui.components.MotionDurations
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.ui.theme.AppPalette
import xyz.lisfox.vocaland.ui.theme.LocalAppColorScheme
import xyz.lisfox.vocaland.ui.theme.ThemeMode
import xyz.lisfox.vocaland.ui.theme.ThemeSettings
import xyz.lisfox.vocaland.util.formatNumber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    session: AuthSession,
    authRepository: AuthRepository,
    themePreferences: ThemePreferences,
    onLogin: () -> Unit,
    onOpenFeature: (String) -> Unit,
    onOpenNotifications: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenWallet: (() -> Unit)? = null,
    onOpenShop: (() -> Unit)? = null,
    onCheckUpdate: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val sessionState by session.state.collectAsStateWithLifecycle()
    val themeSettings by themePreferences.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val notificationsLabel = stringResource(R.string.profile_notifications)
    val settingsLabel = stringResource(R.string.profile_settings)
    val walletLabel = stringResource(R.string.profile_wallet)
    val shopLabel = stringResource(R.string.profile_shop)
    val updateLabel = stringResource(R.string.profile_check_update)
    val tagIndexLabel = stringResource(R.string.profile_tag_index)
    val statLabel = stringResource(R.string.profile_stat)
    val customRankingLabel = stringResource(R.string.profile_custom_ranking)
    val favoritesLabel = stringResource(R.string.profile_favorites)
    val playlistsLabel = stringResource(R.string.profile_playlists)
    val userDetailLabel = stringResource(R.string.profile_user_detail)
    val openNotifications = onOpenNotifications ?: { onOpenFeature(notificationsLabel) }
    val openSettings = onOpenSettings ?: { onOpenFeature(settingsLabel) }
    val openWallet = onOpenWallet ?: { onOpenFeature(walletLabel) }
    val openShop = onOpenShop ?: { onOpenFeature(shopLabel) }
    val checkUpdate = onCheckUpdate ?: { onOpenFeature(updateLabel) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Box {
                AnimatedVisibility(
                    visible = sessionState.user == null,
                    enter = fadeIn(tween(MotionDurations.MEDIUM)),
                    exit = fadeOut(tween(MotionDurations.MEDIUM)),
                ) {
                    GuestHeader(onLogin = onLogin)
                }
                AnimatedVisibility(
                    visible = sessionState.user != null,
                    enter = fadeIn(tween(MotionDurations.MEDIUM)),
                    exit = fadeOut(tween(MotionDurations.MEDIUM)),
                ) {
                    sessionState.user?.let { UserHeader(user = it) }
                }
            }
        }

        item {
            ProfileQuickActions(
                actions = listOf(
                    ProfileQuickAction(
                        icon = Icons.Filled.Favorite,
                        label = favoritesLabel,
                        onClick = { onOpenFeature(favoritesLabel) },
                    ),
                    ProfileQuickAction(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        label = playlistsLabel,
                        onClick = { onOpenFeature(playlistsLabel) },
                    ),
                    ProfileQuickAction(
                        icon = Icons.Filled.Person,
                        label = userDetailLabel,
                        onClick = { onOpenFeature(userDetailLabel) },
                    ),
                ),
            )
        }

        item {
            AppearanceSection(
                themeSettings = themeSettings,
                onMode = themePreferences::setMode,
                onPalette = themePreferences::setPalette,
            )
        }

        item {
            ProfileMenuGroup(
                title = stringResource(R.string.profile_account_section),
                entries = listOf(
                    ProfileMenuEntry(
                        icon = Icons.Filled.Notifications,
                        title = notificationsLabel,
                        onClick = openNotifications,
                    ),
                    ProfileMenuEntry(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = walletLabel,
                        onClick = openWallet,
                    ),
                    ProfileMenuEntry(
                        icon = Icons.Filled.ShoppingBag,
                        title = shopLabel,
                        onClick = openShop,
                    ),
                    ProfileMenuEntry(
                        icon = Icons.Filled.Settings,
                        title = settingsLabel,
                        onClick = openSettings,
                    ),
                    ProfileMenuEntry(
                        icon = Icons.Filled.Refresh,
                        title = updateLabel,
                        onClick = checkUpdate,
                    ),
                ),
            )
        }

        item {
            ProfileMenuGroup(
                title = stringResource(R.string.profile_tools_section),
                entries = listOf(
                    ProfileMenuEntry(
                        icon = Icons.Filled.Search,
                        title = tagIndexLabel,
                        onClick = { onOpenFeature(tagIndexLabel) },
                    ),
                    ProfileMenuEntry(
                        icon = Icons.Filled.Leaderboard,
                        title = statLabel,
                        onClick = { onOpenFeature(statLabel) },
                    ),
                    ProfileMenuEntry(
                        icon = Icons.Filled.Star,
                        title = customRankingLabel,
                        onClick = { onOpenFeature(customRankingLabel) },
                    ),
                ),
            )
        }

        if (sessionState.user != null) {
            item {
                OutlinedButton(
                    onClick = {
                        scope.launch { authRepository.logout() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.profile_logout))
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.profile_version_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

private data class ProfileQuickAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

private data class ProfileMenuEntry(
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit,
)

@Composable
private fun ProfileQuickActions(actions: List<ProfileQuickAction>) {
    val scheme = LocalAppColorScheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { action ->
            Card(
                onClick = action.onClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = scheme.secondaryContainer,
                        contentColor = scheme.onSecondaryContainer,
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSection(
    themeSettings: ThemeSettings,
    onMode: (ThemeMode) -> Unit,
    onPalette: (AppPalette) -> Unit,
) {
    val scheme = LocalAppColorScheme.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = stringResource(R.string.profile_appearance))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "主题模式",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeSettings.mode == mode,
                                onClick = { onMode(mode) },
                                label = { Text(stringResource(mode.labelRes)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (mode) {
                                            ThemeMode.System -> Icons.Filled.BrightnessAuto
                                            ThemeMode.Light -> Icons.Filled.LightMode
                                            ThemeMode.Dark -> Icons.Filled.DarkMode
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                            )
                        }
                    }
                }

                HorizontalDivider(color = scheme.outlineVariant)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.profile_palette),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.profile_palette_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppPalette.entries.forEach { palette ->
                            FilterChip(
                                selected = themeSettings.palette == palette,
                                onClick = { onPalette(palette) },
                                label = { Text(stringResource(palette.labelRes)) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(palette.previewColor),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuGroup(
    title: String,
    entries: List<ProfileMenuEntry>,
) {
    val scheme = LocalAppColorScheme.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = title)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        ) {
            Column {
                entries.forEachIndexed { index, entry ->
                    ProfileMenuItem(
                        icon = { Icon(entry.icon, contentDescription = null) },
                        title = entry.title,
                        onClick = entry.onClick,
                    )
                    if (index != entries.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = scheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestHeader(onLogin: () -> Unit) {
    val scheme = LocalAppColorScheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(scheme.primaryContainer, scheme.tertiaryContainer),
                    ),
                )
                .padding(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = scheme.onPrimaryContainer,
                    modifier = Modifier.size(34.dp),
                )
                Text(
                    text = stringResource(R.string.profile_login_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.profile_login_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.82f),
                )
                Button(
                    onClick = onLogin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.profile_login_register))
                }
            }
        }
    }
}

@Composable
private fun UserHeader(user: CachedUserDto) {
    val scheme = LocalAppColorScheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            scheme.primaryContainer.copy(alpha = 0.85f),
                            scheme.tertiaryContainer.copy(alpha = 0.55f),
                        ),
                    ),
                )
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NetworkImage(
                    url = user.avatar,
                    contentDescription = user.nickname,
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape),
                )
                Spacer(Modifier.size(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.nickname.ifBlank { user.username },
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(
                            text = roleLabel(user.role),
                            containerColor = scheme.surface.copy(alpha = 0.7f),
                            contentColor = scheme.onSurface,
                        )
                        StatusPill(
                            text = "Lv.${user.level}",
                            containerColor = scheme.surface.copy(alpha = 0.7f),
                            contentColor = scheme.onSurface,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surface.copy(alpha = 0.6f))
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                UserStat(value = formatNumber(user.totalExp.toLong()), label = stringResource(R.string.profile_exp))
                UserStat(value = user.level.toString(), label = stringResource(R.string.profile_level))
                UserStat(
                    value = if (user.isBanned) stringResource(R.string.profile_status_restricted) else stringResource(R.string.profile_status_normal),
                    label = stringResource(R.string.profile_account_status),
                )
            }
        }
    }
}

@Composable
private fun UserStat(value: String, label: String) {
    val scheme = LocalAppColorScheme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
) {
    val scheme = LocalAppColorScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = scheme.secondaryContainer,
            contentColor = scheme.onSecondaryContainer,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurface,
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            modifier = Modifier.clearAndSetSemantics {},
            tint = scheme.onSurfaceVariant,
        )
    }
}
