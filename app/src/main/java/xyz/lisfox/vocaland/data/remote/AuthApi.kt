package xyz.lisfox.vocaland.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import xyz.lisfox.vocaland.data.model.CaptchaDto
import xyz.lisfox.vocaland.data.model.LoginRequest
import xyz.lisfox.vocaland.data.model.LoginResponse
import xyz.lisfox.vocaland.data.model.LogoutRequest
import xyz.lisfox.vocaland.data.model.RefreshRequest

interface AuthApi {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("captcha")
    suspend fun captcha(): CaptchaDto

    @POST("refresh")
    suspend fun refresh(@Body request: RefreshRequest): LoginResponse

    @POST("logout")
    suspend fun logout(@Body request: LogoutRequest)
}
