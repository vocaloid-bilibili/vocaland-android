package xyz.lisfox.vocaland.ui.shop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.shop.ShopItemDto
import xyz.lisfox.vocaland.data.shop.ShopRepository
import xyz.lisfox.vocaland.ui.components.EmptyHint
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.util.formatNumber

private val CATEGORY_ORDER = listOf("ticket", "sponsor", "other")

private fun categoryLabel(category: String): String = when (category) {
    "ticket" -> "道具"
    "sponsor" -> "会员"
    else -> "其他"
}

private fun categoryIcon(category: String) = when (category) {
    "ticket" -> Icons.Filled.ShoppingBag
    "sponsor" -> Icons.Filled.EmojiEvents
    else -> Icons.Filled.Storefront
}

private fun itemSubtitle(item: ShopItemDto): String? = when {
    item.category == "sponsor" && (item.durationDays ?: 0) > 0 ->
        "${item.durationDays} 天会员时长"

    item.category == "ticket" && (item.ticketCount ?: 0) > 0 ->
        "每份 ${item.ticketCount} 张"

    else -> null
}

@Composable
fun ShopScreen(
    repository: ShopRepository,
    isLoggedIn: Boolean,
    onLogin: (() -> Unit)? = null,
    onOpenWallet: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel: ShopViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ShopViewModel(repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<ShopItemDto?>(null) }

    LaunchedEffect(isLoggedIn) {
        viewModel.load(includeWallet = isLoggedIn)
    }

    LaunchedEffect(state.notice) {
        if (!state.notice.isNullOrBlank()) {
            selected = null
        }
    }

    val heldMap = remember(state.wallet) {
        state.wallet?.items?.associate { it.itemId to it.quantity }.orEmpty()
    }

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
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "商城",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "用 V币兑换道具与会员时长",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { viewModel.load(includeWallet = isLoggedIn) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = "V币余额",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        when {
                            !isLoggedIn -> {
                                TextButton(onClick = { onLogin?.invoke() }) {
                                    Text("登录后查看")
                                }
                            }

                            state.walletLoading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            }

                            state.walletError != null -> {
                                Text(
                                    text = "获取失败",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }

                            else -> {
                                Text(
                                    text = formatNumber((state.wallet?.vCoins ?: 0).toLong()),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    if (onOpenWallet != null) {
                        OutlinedButton(
                            onClick = onOpenWallet,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Wallet,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("查看钱包与签到")
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

        when {
            state.loading && state.items.isEmpty() -> {
                item {
                    LoadingPane(
                        modifier = Modifier.height(240.dp),
                        text = "正在加载商品",
                    )
                }
            }

            state.error != null && state.items.isEmpty() -> {
                item {
                    ScreenMessage(
                        title = "商品加载失败",
                        message = state.error!!,
                        actionLabel = "重试",
                        onAction = { viewModel.load(includeWallet = isLoggedIn) },
                    )
                }
            }

            state.items.none { it.isActive } -> {
                item {
                    EmptyHint(text = "商城暂时没有上架商品")
                }
            }

            else -> {
                val activeItems = state.items.filter { it.isActive }
                CATEGORY_ORDER.forEach { category ->
                    val group = activeItems
                        .filter { it.category.normalizedCategory() == category }
                        .sortedWith(compareBy({ it.sortOrder }, { it.id }))
                    if (group.isNotEmpty()) {
                        item(key = "header-$category") {
                            SectionHeader(
                                title = categoryLabel(category),
                                subtitle = "${group.size} 件商品",
                            )
                        }
                        group.chunked(2).forEach { rowItems ->
                            item(key = "${category}-${rowItems.first().id}") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    rowItems.forEach { item ->
                                        ShopItemCard(
                                            item = item,
                                            held = item.itemId?.let { heldMap[it] } ?: 0,
                                            onClick = {
                                                viewModel.clearNotice()
                                                selected = item
                                            },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { item ->
        PurchaseDialog(
            item = item,
            held = item.itemId?.let { heldMap[it] } ?: 0,
            balance = state.wallet?.vCoins ?: 0,
            walletLoading = state.walletLoading,
            walletError = state.walletError,
            isLoggedIn = isLoggedIn,
            purchasing = state.purchasing,
            purchaseError = state.purchaseError,
            onLogin = onLogin,
            onDismiss = {
                viewModel.clearNotice()
                selected = null
            },
            onConfirm = { quantity -> viewModel.purchase(item, quantity) },
        )
    }
}

@Composable
private fun ShopItemCard(
    item: ShopItemDto,
    held: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val subtitle = itemSubtitle(item)
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = categoryIcon(item.category.normalizedCategory()),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (held > 0) {
                    Text(
                        text = "持有 $held",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            if (!item.description.isNullOrBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(36.dp),
                )
            } else {
                Spacer(Modifier.height(36.dp))
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = scheme.tertiary,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.size(3.dp))
                Text(
                    text = item.price.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchaseDialog(
    item: ShopItemDto,
    held: Int,
    balance: Int,
    walletLoading: Boolean,
    walletError: String?,
    isLoggedIn: Boolean,
    purchasing: Boolean,
    purchaseError: String?,
    onLogin: (() -> Unit)?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var quantity by remember(item.id) { mutableIntStateOf(1) }
    val total = item.price * quantity
    val affordable = balance >= total
    val subtitle = itemSubtitle(item)
    val delivered = if (item.category == "ticket") {
        (item.ticketCount ?: 0) * quantity
    } else {
        0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("购买「${item.name}」") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!item.description.isNullOrBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (!isLoggedIn) {
                    Text(
                        text = "购买需要登录。登录后可查看 V币余额并下单。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = { if (quantity > 1) quantity -= 1 },
                            enabled = !purchasing && quantity > 1,
                        ) {
                            Text("-")
                        }
                        Text(
                            text = quantity.toString(),
                            modifier = Modifier.padding(horizontal = 20.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        OutlinedButton(
                            onClick = {
                                if (quantity < ShopRepository.MAX_QUANTITY) quantity += 1
                            },
                            enabled = !purchasing && quantity < ShopRepository.MAX_QUANTITY,
                        ) {
                            Text("+")
                        }
                    }

                    Text(
                        text = "单价 ${item.price} V币 · 合计 $total V币",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (delivered > 0) {
                        Text(
                            text = "本次到账 $delivered 张（当前持有 $held 张）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = when {
                            walletLoading -> "正在读取余额"
                            walletError != null -> "余额读取失败，请稍后重试"
                            else -> "当前余额 ${formatNumber(balance.toLong())} V币"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!walletLoading && walletError == null && !affordable) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    if (!purchaseError.isNullOrBlank()) {
                        Text(
                            text = purchaseError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isLoggedIn) {
                Button(
                    onClick = { onConfirm(quantity) },
                    enabled = !purchasing &&
                        !walletLoading &&
                        walletError == null &&
                        affordable,
                ) {
                    if (purchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("确认购买")
                    }
                }
            } else if (onLogin != null) {
                Button(
                    onClick = {
                        onDismiss()
                        onLogin()
                    },
                ) {
                    Text("去登录")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

private fun String.normalizedCategory(): String =
    if (this in CATEGORY_ORDER) this else "other"
