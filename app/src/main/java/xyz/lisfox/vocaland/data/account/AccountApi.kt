package xyz.lisfox.vocaland.data.account

import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part

interface AccountApi {
    @GET("user/me")
    suspend fun me(): UserPrivateDto

    @PATCH("user/me")
    suspend fun updateNickname(@Body request: UpdateNicknameRequest): UpdateNicknameResponse

    @Multipart
    @POST("user/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): AvatarResponse

    @DELETE("user/me/avatar")
    suspend fun deleteAvatar(): StatusResponse

    @POST("user/me/sponsor/claim")
    suspend fun claimSponsor(@Body request: SponsorClaimRequest): JsonElement
}
