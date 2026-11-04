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
