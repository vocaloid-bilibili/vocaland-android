package xyz.lisfox.vocaland.data.repository

import xyz.lisfox.vocaland.data.model.CaptchaDto
import xyz.lisfox.vocaland.data.model.LoginRequest
import xyz.lisfox.vocaland.data.remote.AuthApi
import xyz.lisfox.vocaland.data.session.AuthSession

class AuthRepository(
    private val authApi: AuthApi,
    private val session: AuthSession,
) {
    suspend fun getCaptcha(): Result<CaptchaDto> = runCatching {
        authApi.captcha()
    }

    suspend fun login(
        username: String,
        password: String,
        codeId: Int,
        codeAnswer: String,
    ): Result<Unit> = runCatching {
        val response = authApi.login(
            LoginRequest(
                username = username.trim(),
                password = password,
                codeId = codeId.toString(),
                codeAnswer = codeAnswer.trim(),
            ),
        )
        require(response.accessToken.isNotBlank()) { "登录响应缺少令牌" }
        session.saveLogin(response)
    }

    suspend fun logout() {
        session.logout()
    }
}
