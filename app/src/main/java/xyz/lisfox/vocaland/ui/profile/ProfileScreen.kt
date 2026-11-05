package xyz.lisfox.vocaland.ui.profile

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.BuildConfig
import xyz.lisfox.vocaland.data.model.CachedUserDto
import xyz.lisfox.vocaland.data.preferences.ThemePreferences
import xyz.lisfox.vocaland.data.repository.AuthRepository
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.ui.account.roleLabel
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.ui.theme.AppPalette
import xyz.lisfox.vocaland.ui.theme.ThemeMode
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
    onOpenCheckIn: (() -> Unit)? = null,
    onCheckUpdate: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val sessionState by session.state.collectAsStateWithLifecycle()
    val themeSettings by themePreferences.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val openNotifications = onOpenNotifications ?: { onOpenFeature("通知中心") }
    val openSettings = onOpenSettings ?: { onOpenFeature("账号设置") }
    val openWallet = onOpenWallet ?: { onOpenFeature("我的钱包") }
    val openShop = onOpenShop ?: { onOpenFeature("商城") }
    val openCheckIn = onOpenCheckIn ?: { onOpenFeature("每日签到") }
    val checkUpdate = onCheckUpdate ?: { onOpenFeature("检查更新") }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Crossfade(
                targetState = sessionState.user,
                animationSpec = tween(260),
                label = "profileHeader",
            ) { user ->
                if (user == null) {
                    GuestHeader(onLogin = onLogin)
                } else {
                    UserHeader(user = user)
                }
            }
        }

        item {
            Column {
                SectionHeader(title = "外观模式")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeSettings.mode == mode,
                            onClick = { themePreferences.setMode(mode) },
                            label = { Text(mode.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (mode) {
                                        ThemeMode.System -> Icons.Filled.BrightnessAuto
                                        ThemeMode.Light -> Icons.Filled.LightMode
                                        ThemeMode.Dark -> Icons.Filled.DarkMode
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                )
                            },
                        )
                    }
                }
            }
        }

        item {
            Column {
                SectionHeader(title = "配色方案", subtitle = "选择一套种子色，明暗主题自动生成")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppPalette.entries.forEach { palette ->
                        FilterChip(
                            selected = themeSettings.palette == palette,
                            onClick = { themePreferences.setPalette(palette) },
                            label = { Text(palette.label) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(palette.previewColor),
                                )
                            },
                        )
                    }
                }
            }
        }

        item {
            Column {
                SectionHeader(title = "账号与资产")
                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column {
                        ProfileMenuItem(
                            icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                            title = "通知中心",
                            onClick = openNotifications,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        ProfileMenuItem(
                            icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
                            title = "我的钱包",
                            onClick = openWallet,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        ProfileMenuItem(
                            icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                            title = "每日签到",
                            onClick = openCheckIn,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        ProfileMenuItem(
                            icon = { Icon(Icons.Filled.ShoppingBag, contentDescription = null) },
                            title = "商城",
                            onClick = openShop,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        ProfileMenuItem(
                            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            title = "账号设置",
                            onClick = openSettings,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        ProfileMenuItem(
                            icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                            title = "检查更新",
                            onClick = checkUpdate,
                        )
                    }
                }
            }
        }

        item {
            Column {
                SectionHeader(title = "探索工具")
                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column {
                        ProfileMenuItem(
                            icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            title = "标签图鉴",
                            onClick = { onOpenFeature("标签图鉴") },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        ProfileMenuItem(
                            icon = { Icon(Icons.Filled.Leaderboard, contentDescription = null) },
                            title = "数据统计",
                            onClick = { onOpenFeature("数据统计") },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        ProfileMenuItem(
                            icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                            title = "自定义排行榜",
                            onClick = { onOpenFeature("自定义排行榜") },
                        )
                    }
                }
            }
        }

        item {
            Column {
                SectionHeader(title = "我的内容")
                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column {
                        ProfileMenuItem(
                            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                            title = "我的收藏",
                            onClick = { onOpenFeature("我的收藏") },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        ProfileMenuItem(
                            icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                            title = "我的评分",
                            onClick = { onOpenFeature("我的评分") },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        ProfileMenuItem(
                            icon = { Icon(Icons.Filled.PlaylistPlay, contentDescription = null) },
                            title = "我的歌单",
                            onClick = { onOpenFeature("我的歌单") },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        ProfileMenuItem(
                            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            title = "个人资料",
                            onClick = { onOpenFeature("个人资料") },
                        )
                    }
                }
            }
        }

        if (sessionState.user != null) {
            item {
                OutlinedButton(
                    onClick = {
                        scope.launch { authRepository.logout() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("退出登录")
                }
            }
        }

        item {
            Text(
                text = "VocaLand · 术力口数据库 Android · v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun GuestHeader(onLogin: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
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
                    text = "登录 VocaLand",
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onPrimaryContainer,
                )
                Text(
                    text = "同步收藏、评分与歌单，参与投票和社区讨论。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.82f),
                )
                Button(onClick = onLogin) {
                    Icon(Icons.Filled.Login, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("登录 / 注册")
                }
            }
        }
    }
}

@Composable
private fun UserHeader(user: CachedUserDto) {
    val scheme = MaterialTheme.colorScheme
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
                UserStat(value = formatNumber(user.totalExp.toLong()), label = "经验值")
                UserStat(value = user.level.toString(), label = "等级")
                UserStat(
                    value = if (user.isBanned) "受限" else "正常",
                    label = "账号状态",
                )
            }
        }
    }
}

@Composable
private fun UserStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
