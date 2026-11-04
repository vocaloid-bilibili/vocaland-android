package xyz.lisfox.vocaland.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DataEnvelope<T>(
    val data: T,
)

@Serializable
data class ListEnvelope<T>(
    val data: List<T> = emptyList(),
    val total: Int = 0,
)

// ── 主视觉 ──────────────────────────────────────────────

@Serializable
data class HeroSlideDto(
    val id: Int = 0,
    val caption: String = "",
    val imageUrl: String = "",
    val linkUrl: String = "",
)

// ── 本站统计 ────────────────────────────────────────────

@Serializable
data class SiteStatsDto(
    val songCount: Long = 0,
    val snapshotCount: Long = 0,
    val userCount: Long = 0,
)

// ── 歌曲 / 艺人 ─────────────────────────────────────────

@Serializable
data class ArtistBriefDto(
    val id: Int = 0,
    val name: String = "",
    val picture: String? = null,
)

@Serializable
data class SongProducerDto(
    val producer: ArtistBriefDto = ArtistBriefDto(),
    val role: String? = null,
)

@Serializable
data class SynthesizerDto(
    val id: Int = 0,
    val name: String = "",
    val picture: String? = null,
    val abbreviation: String? = null,
)

@Serializable
data class VocalistDto(
    val id: Int = 0,
    val name: String = "",
    val picture: String? = null,
    val isVs: Boolean = false,
)

@Serializable
data class SongVocalistDto(
    val vocalist: VocalistDto = VocalistDto(),
    val synthesizer: SynthesizerDto? = null,
    val isSupport: Boolean = false,
)

@Serializable
data class VideoDto(
    val bvid: String = "",
    val title: String? = null,
    val thumbnail: String? = null,
    val pubdate: String? = null,
    val duration: Int? = null,
    val copyright: Int? = null,
    val disabled: Boolean = false,
)

@Serializable
data class SongDto(
    val id: Int = 0,
    val name: String = "",
    val displayName: String? = null,
    val type: String? = null,
    val collected: Boolean = true,
    val isForeign: Boolean = false,
    val thumbnail: String? = null,
    val producers: List<SongProducerDto> = emptyList(),
    val vocalists: List<SongVocalistDto> = emptyList(),
    val videos: List<VideoDto> = emptyList(),
)

@Serializable
data class SnapshotDto(
    val bvid: String = "",
    val date: String? = null,
    val view: Long = 0,
    val favorite: Long = 0,
    val coin: Long = 0,
    val like: Long = 0,
    val danmaku: Long = 0,
    val reply: Long = 0,
    val share: Long = 0,
)

@Serializable
data class SongCountDto(
    val daily: Int = 0,
    val weekly: Int = 0,
)

@Serializable
data class SongListItemDto(
    val song: SongDto = SongDto(),
    val video: VideoDto? = null,
    val snapshot: SnapshotDto? = null,
    val count: SongCountDto? = null,
)

@Serializable
data class SongListResponse(
    val data: List<SongListItemDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class SongBriefDto(
    val id: Int = 0,
    val name: String = "",
    val displayName: String? = null,
    val type: String? = null,
    val thumbnail: String? = null,
    val bvid: String? = null,
    val producers: List<SongProducerDto> = emptyList(),
    val vocalists: List<SongVocalistDto> = emptyList(),
)

// ── 里程碑 ─────────────────────────────────────────────

@Serializable
data class MilestoneDto(
    val id: Int = 0,
    val songId: Int = 0,
    val bvid: String = "",
    val milestone: Long = 0,
    val achievedDate: String = "",
    val viewCount: Long = 0,
    val song: SongDto = SongDto(),
    val video: VideoDto? = null,
)

// ── 评分 ───────────────────────────────────────────────

@Serializable
data class RatingUserDto(
    val id: Int = 0,
    val username: String? = null,
    val nickname: String = "",
    val avatar: String? = null,
    val role: String = "guest",
    val level: Int = 0,
)

@Serializable
data class RatingDto(
    val id: String = "",
    val targetKey: String = "",
    val targetName: String? = null,
    val score: Int = 0,
    val comment: String? = null,
    val isAnonymous: Boolean = false,
    val canPeek: Boolean = false,
    val isRevealed: Boolean = false,
    val user: RatingUserDto = RatingUserDto(),
    val createdAt: String = "",
    val updatedAt: String = "",
)

// ── 评论 ───────────────────────────────────────────────

@Serializable
data class CommentUserDto(
    val id: Int = 0,
    val username: String = "",
    val nickname: String = "",
    val avatar: String? = null,
    val role: String = "guest",
    val level: Int = 0,
)

@Serializable
data class CommentDto(
    val id: Int = 0,
    val content: String = "",
    val targetKey: String = "",
    val user: CommentUserDto = CommentUserDto(),
    val likeCount: Int = 0,
    val createdAt: String = "",
)

// ── 投票活动 ───────────────────────────────────────────

@Serializable
data class EdCampaignDto(
    val id: Int = 0,
    val board: String = "",
    val issue: Int = 0,
    val status: String = "nomination",
    val submitStart: String? = null,
    val submitEnd: String? = null,
    val voteStart: String? = null,
    val voteEnd: String? = null,
    val finalizedAt: String? = null,
    val maxVotes: Int? = null,
    val acceptedCount: Int? = null,
    val voterCount: Int? = null,
    val createdAt: String = "",
)

@Serializable
data class TrendingCampaignDto(
    val id: Int = 0,
    val issue: Int = 0,
    val entityKind: String = "song",
    val status: String = "nomination",
    val submitStart: String? = null,
    val submitEnd: String? = null,
    val voteStart: String? = null,
    val voteEnd: String? = null,
    val finalizedAt: String? = null,
    val maxVotes: Int? = null,
    val candidateCount: Int? = null,
    val voterCount: Int? = null,
    val createdAt: String = "",
)

// ── 认证 ───────────────────────────────────────────────

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val codeId: String,
    val codeAnswer: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class LogoutRequest(
    val refreshToken: String,
)

@Serializable
data class CachedUserDto(
    val id: Int = 0,
    val username: String = "",
    val nickname: String = "",
    val role: String = "guest",
    val avatar: String? = null,
    val level: Int = 0,
    val totalExp: Int = 0,
    val isBanned: Boolean = false,
)

@Serializable
data class LoginResponse(
    val user: CachedUserDto? = null,
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresIn: Long = 0,
)

@Serializable
data class CaptchaDto(
    val codeId: Int = 0,
    val image: String = "",
)
