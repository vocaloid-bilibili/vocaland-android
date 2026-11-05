package xyz.lisfox.vocaland.data.account

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.lisfox.vocaland.data.model.CachedUserDto
import xyz.lisfox.vocaland.data.remote.AppJson
import xyz.lisfox.vocaland.data.session.AuthSession

class AccountRepository(
    private val api: AccountApi,
    private val session: AuthSession,
) {
    suspend fun loadMe(): Result<UserPrivateDto> = runCatching {
        val me = api.me()
        session.updateUser(me.toCachedUser())
        me
    }

    suspend fun updateNickname(nickname: String): Result<UpdateNicknameResponse> = runCatching {
        val trimmed = nickname.trim()
        require(trimmed.isNotEmpty()) { "昵称不能为空" }
        val response = api.updateNickname(UpdateNicknameRequest(nickname = trimmed))
        val current = session.state.value.user
        if (current != null) {
            session.updateUser(current.copy(nickname = response.nickname.ifBlank { trimmed }))
        }
        response
    }

    suspend fun uploadAvatar(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): Result<AvatarResponse> = runCatching {
        require(bytes.isNotEmpty()) { "头像文件为空" }
        require(bytes.size <= MAX_AVATAR_BYTES) { "头像不能超过 10MB" }
        val body = bytes.toRequestBody(mimeType.toMediaType())
        val part = MultipartBody.Part.createFormData("file", fileName, body)
        val response = api.uploadAvatar(part)
        val current = session.state.value.user
        if (current != null && response.avatar.isNotBlank()) {
            session.updateUser(current.copy(avatar = response.avatar))
        }
        response
    }

    suspend fun deleteAvatar(): Result<StatusResponse> = runCatching {
        val response = api.deleteAvatar()
        val current = session.state.value.user
        if (current != null) {
            session.updateUser(current.copy(avatar = null))
        }
        response
    }

    suspend fun claimSponsorOrder(orderId: String): Result<SponsorClaimResult> = runCatching {
        val trimmed = orderId.trim()
        require(trimmed.isNotEmpty()) { "请填写爱发电订单号" }
        parseSponsorClaim(api.claimSponsor(SponsorClaimRequest(sourceOrderId = trimmed)))
    }

    private fun parseSponsorClaim(element: JsonElement): SponsorClaimResult {
        val body = element as? JsonObject ?: error("兑换响应格式异常")
        val payload = body["data"] as? JsonObject ?: body
        return AppJson.decodeFromJsonElement(SponsorClaimResult.serializer(), payload)
    }

    private fun UserPrivateDto.toCachedUser(): CachedUserDto = CachedUserDto(
        id = id,
        username = username,
        nickname = nickname,
        role = role,
        avatar = avatar,
        level = level,
        totalExp = totalExp,
        isBanned = isBanned,
    )

    private companion object {
        const val MAX_AVATAR_BYTES = 10 * 1024 * 1024
    }
}
