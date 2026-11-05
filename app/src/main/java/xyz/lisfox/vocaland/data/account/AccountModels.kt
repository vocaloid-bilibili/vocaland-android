package xyz.lisfox.vocaland.data.account

import kotlinx.serialization.Serializable

@Serializable
data class UserPrivateDto(
    val id: Int = 0,
    val username: String = "",
    val nickname: String = "",
    val avatar: String? = null,
    val email: String? = null,
    val emailVerified: Boolean = false,
    val role: String = "guest",
    val level: Int = 0,
    val totalExp: Int = 0,
    val isBanned: Boolean = false,
    val sponsorActive: Boolean = false,
    val createdAt: String? = null,
    val modifiedAt: String? = null,
    val lastLoginAt: String? = null,
    val sponsorExpiresAt: String? = null,
    val sponsorRemainingDays: Int = 0,
)

@Serializable
data class UpdateNicknameRequest(
    val nickname: String,
)

@Serializable
data class UpdateNicknameResponse(
    val status: String = "",
    val nickname: String = "",
)

@Serializable
data class AvatarResponse(
    val status: String = "",
    val avatar: String = "",
)

@Serializable
data class StatusResponse(
    val status: String = "",
)

@Serializable
data class SponsorClaimRequest(
    val sourceOrderId: String,
)

@Serializable
data class SponsorClaimResult(
    val status: String = "",
    val orderId: Int = 0,
    val userId: Int = 0,
    val durationDays: Int = 0,
    val sponsorExpiresAt: String? = null,
)

@Serializable
data class RegisterRequest(
    val username: String,
    val nickname: String,
    val email: String,
    val password: String,
    val emailCode: String,
)

@Serializable
data class RegisterResult(
    val status: String = "",
    val userId: Int = 0,
    val username: String = "",
)

@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String,
)

@Serializable
data class SendEmailCodeRequest(
    val email: String,
    val purpose: String,
    val codeId: Int,
    val codeAnswer: String,
)

@Serializable
data class SimpleMessageResult(
    val status: String = "",
    val message: String = "",
)

@Serializable
data class VerifyEmailRequest(
    val email: String,
    val code: String,
)

@Serializable
data class VerifyEmailResult(
    val status: String = "",
    val message: String = "",
    val emailVerified: Boolean = false,
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String,
)
