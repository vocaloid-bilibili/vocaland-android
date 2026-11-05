package xyz.lisfox.vocaland.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.account.AccountRepository
import xyz.lisfox.vocaland.data.shop.BACKFILL_TICKET_ID
import xyz.lisfox.vocaland.data.shop.BackfillInfoDto
import xyz.lisfox.vocaland.data.shop.PEEK_TICKET_ID
import xyz.lisfox.vocaland.data.shop.ShopRepository
import xyz.lisfox.vocaland.data.shop.VOTE_TICKET_ID
import xyz.lisfox.vocaland.data.shop.quantityOf
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.util.formatNumber

@Composable
fun WalletScreen(
    shopRepository: ShopRepository,
    accountRepository: AccountRepository,
    isLoggedIn: Boolean,
    onLogin: (() -> Unit)? = null,
    onOpenShop: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel: WalletViewModel = viewModel(
        factory = viewModelFactory {
            initializer { WalletViewModel(shopRepository, accountRepository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var backfillOpen by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) viewModel.load()
    }

    if (!isLoggedIn) {
        ScreenMessage(
            title = "登录后查看钱包",
            message = "V币余额、投票卡、补签卡与签到记录都在这里。",
            modifier = modifier,
            actionLabel = if (onLogin != null) "去登录" else null,
            onAction = onLogin,
        )
        return
    }

    when {
        state.loading && state.wallet == null -> {
            LoadingPane(modifier = modifier, text = "正在读取钱包")
        }

        state.wallet == null -> {
            ScreenMessage(
                title = "钱包读取失败",
                message = state.error ?: "请稍后重试",
                modifier = modifier,
                actionLabel = "重试",
                onAction = viewModel::load,
            )
        }

        else -> {
            val wallet = state.wallet!!
            val level = state.level
            val backfillTickets = wallet.quantityOf(BACKFILL_TICKET_ID)

            LazyColumn(
                modifier = modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Wallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.size(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "我的钱包",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "累计获得 ${formatNumber(wallet.totalVCoinsEarned.toLong())} V币",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = viewModel::load) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                                }
                            }

                            Row(verticalAlignment = Alignment.Bottom) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(26.dp),
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    text = formatNumber(wallet.vCoins.toLong()),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    text = "V币",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (onOpenShop != null) {
                                OutlinedButton(
                                    onClick = onOpenShop,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ShoppingBag,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Text("去商城")
                                }
                            }
                        }
                    }
                }

                if (!state.notice.isNullOrBlank()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            Text(
                                text = state.notice!!,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                item {
                    SectionHeader(title = "持有道具", subtitle = "投票券、补签卡与偷窥卡")
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (wallet.items.isEmpty()) {
                                Text(
                                    text = "暂时没有持有道具",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                wallet.items.forEachIndexed { index, item ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                            val note = when (item.itemId) {
                                                VOTE_TICKET_ID -> "ED 附加票"
                                                BACKFILL_TICKET_ID -> "用于补签漏签日期"
                                                PEEK_TICKET_ID -> "用于查看匿名评分"
                                                else -> item.description
                                            }
                                            if (!note.isNullOrBlank()) {
                                                Text(
                                                    text = note,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                        Text(
                                            text = "×${item.quantity}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SectionHeader(title = "每日签到", subtitle = "连续签到可获得额外经验")
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (level == null) {
                                Text(
                                    text = "签到状态读取中",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.EventAvailable,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Text(
                                        text = if (level.checkedInToday) {
                                            "今日已签到"
                                        } else {
                                            "今日未签到"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    if (level.streakDays > 0) {
                                        Text(
                                            text = "连续 ${level.streakDays} 天",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                val progress = level.progress.toFloat().coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                )
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Lv.${level.level}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = "${level.currentLevelExp} / ${level.nextLevelExp}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = "今日经验 +${level.todayExp}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                if (!level.checkedInToday) {
                                    Button(
                                        onClick = viewModel::checkin,
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !state.checkingIn,
                                    ) {
                                        if (state.checkingIn) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.CalendarMonth,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(Modifier.size(8.dp))
                                            Text("每日签到")
                                        }
                                    }
                                }

                                if (backfillTickets > 0) {
                                    OutlinedButton(
                                        onClick = {
                                            backfillOpen = true
                                            viewModel.loadBackfillInfo()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("补签（$backfillTickets 张）")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SectionHeader(title = "赞助", subtitle = "赞助状态与订单兑换")
                    Spacer(Modifier.height(8.dp))
                    SponsorClaimCard(
                        claiming = state.sponsorClaiming,
                        message = state.sponsorMessage,
                        error = state.sponsorError,
                        onSubmit = viewModel::claimSponsor,
                    )
                }
            }

            if (backfillOpen) {
                BackfillDialog(
                    info = state.backfillInfo,
                    loading = state.backfillLoading,
                    busy = state.backfillBusy,
                    onDismiss = { backfillOpen = false },
                    onSelect = viewModel::backfill,
                )
            }
        }
    }
}

@Composable
private fun SponsorClaimCard(
    claiming: Boolean,
    message: String?,
    error: String?,
    onSubmit: (String) -> Unit,
) {
    var orderId by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) orderId = ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "赞助权益可通过爱发电订单号兑换",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = orderId,
                    onValueChange = { orderId = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("爱发电订单号") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                Button(
                    onClick = { onSubmit(orderId) },
                    enabled = !claiming && orderId.isNotBlank(),
                ) {
                    if (claiming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("兑换")
                    }
                }
            }
            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (!error.isNullOrBlank()) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun BackfillDialog(
    info: BackfillInfoDto?,
    loading: Boolean,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("补签漏签的日期") },
        text = {
            when {
                loading && info == null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }

                info == null -> {
                    Text(
                        text = "补签信息加载失败",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                info.missedDates.isEmpty() -> {
                    Text(
                        text = "最近没有漏签的日期",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "可用补签卡 ${info.tickets} 张，只能补过去的日期",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        info.missedDates.forEach { date ->
                            OutlinedButton(
                                onClick = { onSelect(date) },
                                enabled = !busy && info.tickets > 0,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(date)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}
