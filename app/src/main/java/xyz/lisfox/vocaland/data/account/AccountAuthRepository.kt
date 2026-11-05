package xyz.lisfox.vocaland.data.account

import xyz.lisfox.vocaland.data.model.CaptchaDto

class AccountAuthRepository(
    private val api: AccountAuthApi,
) {
    suspend fun getCaptcha(): Result<CaptchaDto> = runCatching {
        api.captcha()
    }

    suspend fun register(
        username: String,
        nickname: String,
        email: String,
        password: String,
        emailCode: String,
    ): Result<RegisterResult> = runCatching {
        api.register(
            RegisterRequest(
                username = username.trim(),
                nickname = nickname.trim(),
                email = email.trim(),
                password = password,
                emailCode = emailCode.trim(),
            ),
        )
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<StatusResponse> =
        runCatching {
            require(oldPassword.isNotEmpty()) { "请输入原密码" }
            require(newPassword.length >= 6) { "新密码至少 6 位" }
            api.changePassword(
                ChangePasswordRequest(
                    oldPassword = oldPassword,
                    newPassword = newPassword,
                ),
            )
        }

    suspend fun sendEmailCode(
        email: String,
        purpose: String,
        codeId: Int,
        codeAnswer: String,
    ): Result<SimpleMessageResult> = runCatching {
        require(email.contains("@")) { "请输入有效的邮箱地址" }
        require(codeAnswer.isNotBlank()) { "请输入图形验证码" }
        api.sendEmailCode(
            SendEmailCodeRequest(
                email = email.trim(),
                purpose = purpose,
                codeId = codeId,
                codeAnswer = codeAnswer.trim(),
            ),
        )
    }

    suspend fun verifyEmail(email: String, code: String): Result<VerifyEmailResult> = runCatching {
        api.verifyEmail(
            VerifyEmailRequest(
                email = email.trim(),
                code = code.trim(),
            ),
        )
    }

    suspend fun changeEmail(email: String, code: String): Result<VerifyEmailResult> = runCatching {
        api.changeEmail(
            VerifyEmailRequest(
                email = email.trim(),
                code = code.trim(),
            ),
        )
    }

    suspend fun resetPasswordByEmail(
        email: String,
        code: String,
        newPassword: String,
    ): Result<SimpleMessageResult> = runCatching {
        require(newPassword.length >= 6) { "新密码至少 6 位" }
        api.resetPasswordByEmail(
            ResetPasswordRequest(
                email = email.trim(),
                code = code.trim(),
                newPassword = newPassword,
            ),
        )
    }
}
