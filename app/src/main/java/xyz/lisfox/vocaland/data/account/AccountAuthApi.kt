package xyz.lisfox.vocaland.data.account

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import xyz.lisfox.vocaland.data.model.CaptchaDto

interface AccountAuthApi {
    @GET("captcha")
    suspend fun captcha(): CaptchaDto

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): RegisterResult

    @POST("change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): StatusResponse

    @POST("send-email-code")
    suspend fun sendEmailCode(@Body request: SendEmailCodeRequest): SimpleMessageResult

    @POST("verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): VerifyEmailResult

    @POST("change-email")
    suspend fun changeEmail(@Body request: VerifyEmailRequest): VerifyEmailResult

    @POST("reset-password-by-email")
    suspend fun resetPasswordByEmail(@Body request: ResetPasswordRequest): SimpleMessageResult
}
