package xyz.lisfox.vocaland.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SongWithVideosListResponse(
    val data: List<SongDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class ArtistDetailDto(
    val id: Int = 0,
    val name: String = "",
    val picture: String? = null,
    val vocadbId: Int? = null,
    val vocawikiEntry: String? = null,
    val vcpediaEntry: String? = null,
    val isVs: Boolean = false,
    val abbreviation: String? = null,
    val uid: String? = null,
)

@Serializable
data class ArtistStatsSummaryDto(
    val view: Long = 0,
    val favorite: Long = 0,
    val coin: Long = 0,
    val like: Long = 0,
    val danmaku: Long = 0,
    val reply: Long = 0,
    val share: Long = 0,
    val songCount: Int = 0,
)

@Serializable
data class TagItemDto(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val scope: List<String> = emptyList(),
    val associationCount: Int = 0,
    val picture: String? = null,
)

@Serializable
data class RatingSummaryDto(
    val targetKey: String = "",
    val total: Int = 0,
    val avgScore: Double? = null,
    val scoreCounts: List<Int> = emptyList(),
)

@Serializable
data class RatingListResponse(
    val data: List<RatingDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class CommentListResponse(
    val data: List<CommentDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class SongRankingStatsDto(
    val totalCount: Int = 0,
    val top20Count: Int = 0,
    val top1Count: Int = 0,
    val bestRank: Int = 0,
    val bestRankIssue: Int? = null,
    val avgRank: Double? = null,
    val recentCount: Int = 0,
    val bestPoint: Long = 0,
    val bestPointIssue: Int? = null,
    val firstIssue: Int = 0,
    val firstIssueRank: Int? = null,
    val latestRank: Int? = null,
    val latestIssue: Int? = null,
    val prevRank: Int? = null,
)

data class SongPage(
    val songs: List<SongDto> = emptyList(),
    val total: Int = 0,
)

enum class ArtistKind(
    val apiType: String,
    val label: String,
) {
    Vocalist("vocalist", "歌手"),
    Producer("producer", "P主"),
    Synthesizer("synthesizer", "合成器"),
    ;

    companion object {
        fun fromApiType(type: String): ArtistKind? =
            entries.firstOrNull { it.apiType == type }
    }
}
