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

@Serializable
data class ProfileLinkDto(
    val platform: String = "",
    val url: String = "",
    val label: String? = null,
)

@Serializable
data class PublicProfileDataDto(
    val bio: String? = null,
    val content: String? = null,
    val links: List<ProfileLinkDto> = emptyList(),
    val bannerUrl: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class PublicProfileUserDto(
    val id: Int = 0,
    val username: String = "",
    val nickname: String = "",
    val avatar: String? = null,
    val isBanned: Boolean = false,
    val level: Int = 0,
    val sponsorActive: Boolean = false,
    val role: String? = null,
    val createdAt: String? = null,
    val totalExp: Int? = null,
    val streakDays: Int? = null,
)

@Serializable
data class PublicProfileResponseDto(
    val user: PublicProfileUserDto = PublicProfileUserDto(),
    val profile: PublicProfileDataDto = PublicProfileDataDto(),
)

@Serializable
data class LevelInfoDto(
    val level: Int = 0,
    val totalExp: Int = 0,
    val currentLevelExp: Int = 0,
    val nextLevelExp: Int = 0,
    val progress: Double = 0.0,
    val streakDays: Int = 0,
    val streakBonus: Double = 0.0,
    val todayExp: Int = 0,
    val checkedInToday: Boolean = false,
    val loginToday: Boolean = false,
    val multiplier: Double = 1.0,
    val nextTicketDays: Int? = null,
)

@Serializable
data class CheckinResultDto(
    val already: Boolean = false,
    val expGained: Int = 0,
    val checkinExp: Int = 0,
    val streakBonus: Int = 0,
    val streakDays: Int = 0,
    val level: Int = 0,
    val totalExp: Int = 0,
    val streakBroken: Boolean = false,
    val ticketGranted: Boolean = false,
)
