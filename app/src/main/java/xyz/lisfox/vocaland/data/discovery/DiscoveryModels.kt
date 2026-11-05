package xyz.lisfox.vocaland.data.discovery

import kotlinx.serialization.Serializable
import xyz.lisfox.vocaland.data.model.ArtistBriefDto
import xyz.lisfox.vocaland.data.model.SongProducerDto
import xyz.lisfox.vocaland.data.model.SongVocalistDto
import xyz.lisfox.vocaland.data.model.TagItemDto

@Serializable
data class TagCategoryCountDto(
    val category: String = "",
    val count: Int = 0,
)

@Serializable
data class TagCategorySummaryDto(
    val categories: List<TagCategoryCountDto> = emptyList(),
    val totalTags: Int = 0,
)

@Serializable
data class TagGroupedCategoryDto(
    val category: String = "",
    val tags: List<TagItemDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class TagGroupedListResponseDto(
    val data: List<TagGroupedCategoryDto> = emptyList(),
)

@Serializable
data class TagFlatListResponseDto(
    val data: List<TagItemDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class ArtistRankingSongDto(
    val id: Int = 0,
    val name: String = "",
    val displayName: String? = null,
    val point: Double = 0.0,
    val rank: Int = 0,
)

@Serializable
data class ArtistRankingItemDto(
    val rank: Int = 0,
    val artist: ArtistBriefDto = ArtistBriefDto(),
    val totalPoint: Double = 0.0,
    val songs: List<ArtistRankingSongDto> = emptyList(),
    val songCount: Int = 0,
)

@Serializable
data class ArtistRankingResponseDto(
    val data: List<ArtistRankingItemDto> = emptyList(),
    val total: Int = 0,
    val issue: Int = 0,
)

@Serializable
data class ArtistRankingSongDetailDto(
    val id: Int = 0,
    val name: String = "",
    val displayName: String? = null,
    val producers: List<SongProducerDto> = emptyList(),
    val vocalists: List<SongVocalistDto> = emptyList(),
    val point: Double = 0.0,
    val rank: Int = 0,
)

@Serializable
data class ArtistRankingSongsResponseDto(
    val data: List<ArtistRankingSongDetailDto> = emptyList(),
    val total: Int = 0,
)
