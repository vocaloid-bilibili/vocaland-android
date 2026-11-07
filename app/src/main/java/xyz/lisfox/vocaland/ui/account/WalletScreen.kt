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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.R
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
            title = stringResource(R.string.wallet_login_required_title),
            message = stringResource(R.string.wallet_login_required_message),
            modifier = modifier,
            actionLabel = if (onLogin != null) stringResource(R.string.action_login) else null,
            onAction = onLogin,
        )
        return
    }

    when {
        state.loading && state.wallet == null -> {
            LoadingPane(modifier = modifier, text = stringResource(R.string.wallet_loading))
        }

        state.wallet == null -> {
            ScreenMessage(
                title = stringResource(R.string.wallet_load_failed_title),
                message = state.error ?: stringResource(R.string.common_retry_later),
                modifier = modifier,
                actionLabel = stringResource(R.string.action_retry),
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
                                        text = "V币余额",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.wallet_total_earned_format,
                                            formatNumber(wallet.totalVCoinsEarned.toLong()),
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = viewModel::load) {
                                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
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
                                    text = stringResource(R.string.wallet_coin_unit),
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
                                    Text(stringResource(R.string.wallet_action_open_shop))
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
                    SectionHeader(
                        title = stringResource(R.string.wallet_items_title),
                        subtitle = stringResource(R.string.wallet_items_subtitle),
                    )
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
                                    text = stringResource(R.string.wallet_items_empty),
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
                                                VOTE_TICKET_ID -> stringResource(R.string.wallet_item_note_vote)
                                                BACKFILL_TICKET_ID -> stringResource(R.string.wallet_item_note_backfill)
                                                PEEK_TICKET_ID -> stringResource(R.string.wallet_item_note_peek)
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
                                            text = stringResource(R.string.wallet_item_quantity_format, item.quantity),
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
                    SectionHeader(
                        title = stringResource(R.string.profile_check_in),
                        subtitle = stringResource(R.string.wallet_checkin_subtitle),
                    )
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
                                    text = stringResource(R.string.wallet_checkin_loading),
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
                                            stringResource(R.string.wallet_checkin_done)
                                        } else {
                                            stringResource(R.string.wallet_checkin_pending)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    if (level.streakDays > 0) {
                                        Text(
                                            text = stringResource(R.string.wallet_checkin_streak_format, level.streakDays),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                val progress = if (level.nextLevelExp > 0) {
                                    (level.currentLevelExp.toFloat() / level.nextLevelExp.toFloat())
                                        .coerceIn(0f, 1f)
                                } else {
                                    level.progress.toFloat().coerceIn(0f, 1f)
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    drawStopIndicator = {},
                                )
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Lv.${level.level}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = "${formatNumber(level.currentLevelExp.toLong())} / ${formatNumber(level.nextLevelExp.toLong())}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.wallet_checkin_today_exp_format, level.todayExp),
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
                                            Text(stringResource(R.string.profile_check_in))
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
                                        Text(stringResource(R.string.wallet_backfill_button_format, backfillTickets))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = stringResource(R.string.wallet_sponsor_title),
                        subtitle = stringResource(R.string.wallet_sponsor_subtitle),
                    )
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
    var orderId by rememberSaveable { mutableStateOf("") }

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
                    text = stringResource(R.string.wallet_sponsor_hint),
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
                    label = { Text(stringResource(R.string.wallet_sponsor_order_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (orderId.isNotBlank() && !claiming) onSubmit(orderId)
                        },
                    ),
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
                        Text(stringResource(R.string.wallet_sponsor_claim))
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
        title = { Text(stringResource(R.string.wallet_backfill_dialog_title)) },
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
                        text = stringResource(R.string.wallet_backfill_load_failed),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                info.missedDates.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.wallet_backfill_empty),
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
                            text = stringResource(R.string.wallet_backfill_hint_format, info.tickets),
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
                Text(stringResource(R.string.action_close))
            }
        },
    )
}
