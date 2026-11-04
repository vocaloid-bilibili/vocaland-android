package xyz.lisfox.vocaland.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PublicUserDto(
    val id: Int = 0,
    val username: String = "",
    val nickname: String = "",
    val avatar: String? = null,
    val isBanned: Boolean = false,
    val level: Int = 0,
    val sponsorActive: Boolean = false,
)

@Serializable
data class UserLitStatsDto(
    val songCount: Int = 0,
    val producerCount: Int = 0,
    val vocalistCount: Int = 0,
    val litTotal: Int = 0,
    val wantCount: Int = 0,
)
