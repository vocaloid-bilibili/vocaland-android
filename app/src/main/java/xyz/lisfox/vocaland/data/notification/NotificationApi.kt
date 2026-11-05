package xyz.lisfox.vocaland.data.notification

import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface NotificationApi {
    @GET("notifications")
    suspend fun page(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("unread_only") unreadOnly: Boolean = false,
    ): NotificationPageDto

    @POST("notifications/read")
    suspend fun markRead(@Body request: MarkReadRequest): MarkReadResult

    @POST("notifications/read-all")
    suspend fun markAllRead(@Body body: JsonObject): MarkReadResult

    @GET("user/me/notification-prefs")
    suspend fun preferences(): NotificationPreferencesDto

    @PUT("user/me/notification-prefs")
    suspend fun updatePreferences(
        @Body request: NotificationPreferencesPatch,
    ): NotificationPreferencesDto
}
