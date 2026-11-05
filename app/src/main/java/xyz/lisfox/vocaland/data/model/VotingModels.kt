package xyz.lisfox.vocaland.data.model

import kotlinx.serialization.Serializable

enum class VotingKind(val routeValue: String, val label: String) {
    Ed("ed", "ED 投票"),
    Trending("trending", "人气投票");

    companion object {
        fun fromRoute(value: String?): VotingKind? =
            entries.firstOrNull { it.routeValue == value }
    }
}

@Serializable
data class EdVotingCandidatesResponse(
    val data: List<EdVotingCandidateDto> = emptyList(),
    val maxVotes: Int = 0,
    val voterCount: Int = 0,
)

@Serializable
data class EdVotingCandidateDto(
    val candidateId: Int = 0,
    val bvid: String = "",
    val songId: Int? = null,
    val isDaily: Boolean = false,
    val isWeekly: Boolean = false,
    val isMonthly: Boolean = false,
    val song: EdSongSummaryDto? = null,
    val video: EdVideoSummaryDto? = null,
)

@Serializable
data class EdSongSummaryDto(
    val id: Int = 0,
    val name: String = "",
    val displayName: String? = null,
    val vocalists: List<EdArtistBriefDto> = emptyList(),
    val producers: List<EdArtistBriefDto> = emptyList(),
)

@Serializable
data class EdArtistBriefDto(
    val id: Int = 0,
    val name: String = "",
)

@Serializable
data class EdVideoSummaryDto(
    val bvid: String = "",
    val title: String = "",
    val thumbnail: String? = null,
    val duration: Int? = null,
    val uploaderName: String? = null,
)

@Serializable
data class EdVoteDto(
    val id: Int = 0,
    val campaignId: Int = 0,
    val candidateId: Int = 0,
    val isNegative: Boolean = false,
    val isBonus: Boolean = false,
    val bonusCount: Int = 0,
    val createdAt: String = "",
)

@Serializable
data class EdBonusVoteDto(
    val candidateId: Int,
    val count: Int,
    val isNegative: Boolean,
)

@Serializable
data class EdCastVotesRequest(
    val campaignId: Int,
    val positiveCandidateIds: List<Int>,
    val negativeCandidateIds: List<Int>,
    val bonusVotes: List<EdBonusVoteDto> = emptyList(),
)

@Serializable
data class TrendingCandidateListResponse(
    val data: List<TrendingCandidateDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class TrendingCandidateDto(
    val id: Int = 0,
    val campaignId: Int = 0,
    val status: String = "",
    val entityId: Int = 0,
    val entityKind: String = "",
    val nominator: VotingUserDto? = null,
    val createdAt: String = "",
    val song: SongDto? = null,
    val producer: TrendingArtistDto? = null,
    val vocalist: TrendingArtistDto? = null,
)

@Serializable
data class TrendingArtistDto(
    val id: Int = 0,
    val name: String = "",
    val picture: String? = null,
)

@Serializable
data class VotingUserDto(
    val id: Int = 0,
    val nickname: String = "",
    val avatar: String? = null,
)

@Serializable
data class TrendingTallyDto(
    val data: List<TrendingTallyEntryDto> = emptyList(),
    val totalVotes: Int = 0,
    val voterCount: Int = 0,
    val maxVotes: Int? = null,
)

@Serializable
data class TrendingTallyEntryDto(
    val candidateId: Int = 0,
    val voteCount: Int = 0,
)

@Serializable
data class TrendingMyVotesDto(
    val data: List<Int> = emptyList(),
)

@Serializable
data class TrendingCastVotesRequest(
    val candidateIds: List<Int>,
)
