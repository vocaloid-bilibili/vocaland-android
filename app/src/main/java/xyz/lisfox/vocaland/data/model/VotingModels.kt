package xyz.lisfox.vocaland.data.model

import kotlinx.serialization.SerialName
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
    val mechanism: EdMechanismDto? = null,
)

@Serializable
data class EdMechanismDto(
    val key: String = "",
    val config: EdMechanismConfigDto = EdMechanismConfigDto(),
)

@Serializable
data class EdMechanismConfigDto(
    @SerialName("allowNegativeVote") val allowNegativeVote: Boolean = true,
    @SerialName("allowBonusVote") val allowBonusVote: Boolean = true,
    @SerialName("roleBonusMultiplier") val roleBonusMultiplier: Double = 1.2,
    @SerialName("negativeWeight") val negativeWeight: Double = 0.5,
    @SerialName("bonusExponent") val bonusExponent: Double = 0.6,
    @SerialName("winPenaltyDailyPct") val winPenaltyDailyPct: Double = 5.0,
    @SerialName("winPenaltyWeeklyMonthlyPct") val winPenaltyWeeklyMonthlyPct: Double = 10.0,
    @SerialName("winPenaltyWindowMonths") val winPenaltyWindowMonths: Int = 12,
    @SerialName("tieBreakers") val tieBreakers: List<String> = emptyList(),
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
    val name: String? = null,
    val displayName: String? = null,
    val vocalists: List<EdArtistBriefDto> = emptyList(),
    val producers: List<EdArtistBriefDto> = emptyList(),
)

@Serializable
data class EdArtistBriefDto(
    val id: Int = 0,
    val name: String? = null,
)

@Serializable
data class EdVideoSummaryDto(
    val bvid: String = "",
    val title: String? = null,
    val thumbnail: String? = null,
    val duration: Int? = null,
    val uploaderName: String? = null,
)

@Serializable
data class EdCandidateDto(
    val id: Int = 0,
    val campaignId: Int = 0,
    val songId: Int? = null,
    val bvid: String = "",
    val status: String = "",
    val rejectReasonCode: String? = null,
    val rejectReason: String? = null,
    val isDaily: Boolean = false,
    val isWeekly: Boolean = false,
    val isMonthly: Boolean = false,
    val createdAt: String = "",
    val song: EdSongSummaryDto? = null,
    val video: EdVideoSummaryDto? = null,
    val nominator: VotingUserDto? = null,
)

@Serializable
data class EdCandidateListResponse(
    val data: List<EdCandidateDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class EdEligibilityDto(
    val canSubmit: Boolean = false,
    val isDaily: Boolean = false,
    val isWeekly: Boolean = false,
    val isMonthly: Boolean = false,
    val reasons: List<String> = emptyList(),
    val songId: Int? = null,
)

@Serializable
data class EdSubmitNominationRequest(
    val campaignId: Int,
    val bvid: String,
    val songId: Int? = null,
    val name: String? = null,
    val vocalist: String? = null,
    val producer: String? = null,
)

@Serializable
data class EdSubmitNominationResponse(
    val id: Int = 0,
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
data class EdVoteUserDto(
    val id: Int = 0,
    val nickname: String = "",
    val avatar: String? = null,
    val role: String = "user",
    val level: Int = 0,
)

@Serializable
data class EdVoteSnapshotDto(
    val userId: Int = 0,
    val user: EdVoteUserDto? = null,
    val isNegative: Boolean = false,
    val isBonus: Boolean = false,
    val bonusCount: Int = 0,
    val rawWeight: Double = 0.0,
    val roleBonusPct: Double = 100.0,
    val votePenaltyPct: Double = 0.0,
    val actualWeight: Double = 0.0,
    val createdAt: String = "",
)

@Serializable
data class EdResultDto(
    val candidate: EdCandidateDto = EdCandidateDto(),
    val category: String = "",
    val rank: Int = 0,
    val score: Double = 0.0,
    val songPenaltyPct: Double = 0.0,
    val voteSnapshots: List<EdVoteSnapshotDto> = emptyList(),
)

@Serializable
data class EdResultListResponse(
    val data: List<EdResultDto> = emptyList(),
)

@Serializable
data class EdHistoryDto(
    val id: Int = 0,
    val board: String = "",
    val issue: Int = 0,
    val songId: Int? = null,
    val bvid: String = "",
    val score: Double = 0.0,
    val userId: Int? = null,
    val createdAt: String = "",
    val song: EdSongSummaryDto? = null,
    val video: EdVideoSummaryDto? = null,
    val nominator: VotingUserDto? = null,
)

@Serializable
data class EdHistoryListResponse(
    val data: List<EdHistoryDto> = emptyList(),
    val total: Int = 0,
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

@Serializable
data class TrendingNominateRequest(
    val entityKind: String,
    val entityId: Int,
)

@Serializable
data class TrendingResultRowDto(
    val rank: Int = 0,
    val candidate: TrendingCandidateDto = TrendingCandidateDto(),
    val voteCount: Int = 0,
)

@Serializable
data class TrendingResultListResponse(
    val data: List<TrendingResultRowDto> = emptyList(),
)

@Serializable
data class TrendingHistoryListResponse(
    val data: List<TrendingResultRowDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class WalletItemDto(
    val itemId: Int = 0,
    val name: String = "",
    val category: String = "",
    val quantity: Int = 0,
    val description: String? = null,
)

@Serializable
data class WalletDto(
    val vCoins: Int = 0,
    val totalVCoinsEarned: Int = 0,
    val items: List<WalletItemDto> = emptyList(),
) {
    val voteTickets: Int get() = items.firstOrNull { it.itemId == VOTE_TICKET_ITEM_ID }?.quantity ?: 0

    private companion object {
        const val VOTE_TICKET_ITEM_ID = 1
    }
}

const val TRENDING_NOMINATION_LIMIT = 3
