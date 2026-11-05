package xyz.lisfox.vocaland.data.shop

import kotlinx.serialization.json.JsonObject

class ShopRepository(
    private val api: ShopApi,
) {
    suspend fun loadItems(category: String? = null): Result<List<ShopItemDto>> = runCatching {
        api.items(category).data
    }

    suspend fun purchase(itemId: Int, quantity: Int): Result<ShopPurchaseResultDto> = runCatching {
        require(quantity in 1..MAX_QUANTITY) { "购买数量需在 1 到 $MAX_QUANTITY 之间" }
        api.purchase(ShopPurchaseRequest(id = itemId, quantity = quantity))
    }

    suspend fun loadWallet(): Result<WalletDto> = runCatching {
        api.wallet()
    }

    suspend fun loadLevel(): Result<LevelInfoDto> = runCatching {
        api.level()
    }

    suspend fun checkin(): Result<CheckinResultDto> = runCatching {
        api.checkin(JsonObject(emptyMap()))
    }

    suspend fun loadBackfillInfo(): Result<BackfillInfoDto> = runCatching {
        api.backfillInfo()
    }

    suspend fun backfill(date: String): Result<BackfillResultDto> = runCatching {
        require(date.isNotBlank()) { "请选择补签日期" }
        api.backfill(BackfillRequest(date = date))
    }

    companion object {
        const val MAX_QUANTITY = 100
    }
}
