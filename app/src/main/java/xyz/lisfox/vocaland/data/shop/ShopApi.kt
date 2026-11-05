package xyz.lisfox.vocaland.data.shop

import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ShopApi {
    @GET("shop/items")
    suspend fun items(@Query("category") category: String? = null): ShopItemListDto

    @POST("shop/orders")
    suspend fun purchase(@Body request: ShopPurchaseRequest): ShopPurchaseResultDto

    @GET("user/me/wallet")
    suspend fun wallet(): WalletDto

    @GET("user/me/level")
    suspend fun level(): LevelInfoDto

    @POST("user/me/checkin")
    suspend fun checkin(@Body body: JsonObject): CheckinResultDto

    @GET("user/me/checkin/backfill")
    suspend fun backfillInfo(): BackfillInfoDto

    @POST("user/me/checkin/backfill")
    suspend fun backfill(@Body request: BackfillRequest): BackfillResultDto
}
