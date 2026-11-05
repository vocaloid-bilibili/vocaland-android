package xyz.lisfox.vocaland.data.shop

import kotlinx.serialization.Serializable

@Serializable
data class ShopItemDto(
    val id: Int = 0,
    val name: String = "",
    val itemId: Int? = null,
    val category: String = "other",
    val price: Int = 0,
    val durationDays: Int? = null,
    val ticketCount: Int? = null,
    val icon: String? = null,
    val description: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
)

@Serializable
data class ShopItemListDto(
    val data: List<ShopItemDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class ShopPurchaseRequest(
    val id: Int,
    val quantity: Int = 1,
)

@Serializable
data class ShopOrderDto(
    val id: Int = 0,
    val userId: Int = 0,
    val itemId: Int = 0,
    val itemName: String = "",
    val price: Int = 0,
    val quantity: Int = 0,
    val totalCoins: Int = 0,
    val status: String = "",
    val createdAt: String? = null,
)

@Serializable
data class ShopPurchaseResultDto(
    val status: String = "",
    val order: ShopOrderDto = ShopOrderDto(),
    val vCoinsRemaining: Int = 0,
)

@Serializable
data class WalletItemDto(
    val itemId: Int = 0,
    val name: String = "",
    val category: String = "",
    val quantity: Int = 0,
    val description: String? = null,
)

@Serializable
data class WalletDto(
    val vCoins: Int = 0,
    val totalVCoinsEarned: Int = 0,
    val items: List<WalletItemDto> = emptyList(),
)

@Serializable
data class LevelInfoDto(
    val level: Int = 0,
    val totalExp: Int = 0,
    val currentLevelExp: Int = 0,
    val nextLevelExp: Int = 0,
    val progress: Double = 0.0,
    val streakDays: Int = 0,
    val streakBonus: Int = 0,
    val todayExp: Int = 0,
    val checkedInToday: Boolean = false,
    val loginToday: Boolean = false,
    val multiplier: Double = 1.0,
    val nextTicketDays: Int? = null,
)

@Serializable
data class CheckinResultDto(
    val already: Boolean = false,
    val expGained: Int = 0,
    val checkinExp: Int = 0,
    val streakBonus: Int = 0,
    val streakDays: Int = 0,
    val level: Int = 0,
    val totalExp: Int = 0,
    val streakBroken: Boolean = false,
    val ticketGranted: Boolean = false,
)

@Serializable
data class BackfillInfoDto(
    val tickets: Int = 0,
    val nextTicketDays: Int? = null,
    val missedDates: List<String> = emptyList(),
)

@Serializable
data class BackfillRequest(
    val date: String,
)

@Serializable
data class BackfillResultDto(
    val success: Boolean = false,
    val date: String? = null,
    val expGained: Int = 0,
    val streakDays: Int = 0,
    val level: Int = 0,
    val totalExp: Int = 0,
    val ticketsRemaining: Int = 0,
    val error: String? = null,
)

const val VOTE_TICKET_ID = 1
const val BACKFILL_TICKET_ID = 3
const val PEEK_TICKET_ID = 4

fun WalletDto.quantityOf(itemId: Int): Int =
    items.firstOrNull { it.itemId == itemId }?.quantity ?: 0
