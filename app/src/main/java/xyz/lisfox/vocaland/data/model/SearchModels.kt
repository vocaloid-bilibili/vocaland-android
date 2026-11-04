package xyz.lisfox.vocaland.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchEntityDto(
    val id: Int = 0,
    val name: String = "",
    val displayName: String? = null,
    val type: String? = null,
    val thumbnail: String? = null,
    val picture: String? = null,
    val abbreviation: String? = null,
    val description: String? = null,
    val associationCount: Int = 0,
    val uid: String? = null,
    val score: Double = 0.0,
    val producers: List<SongProducerDto> = emptyList(),
    val vocalists: List<SongVocalistDto> = emptyList(),
    val videos: List<VideoDto> = emptyList(),
    val song: SongDto? = null,
    val songId: Int = 0,
    val bvid: String = "",
    val title: String? = null,
    val uploader: ArtistBriefDto? = null,
    val latestSnapshot: SnapshotDto? = null,
)

@Serializable
data class SearchGroupDto(
    val type: String = "",
    val total: Int = 0,
    val data: List<SearchEntityDto> = emptyList(),
)

@Serializable
data class ComprehensiveSearchResponse(
    val keyword: String = "",
    val groups: List<SearchGroupDto> = emptyList(),
)

@Serializable
data class SearchListResponse(
    val data: List<SearchEntityDto> = emptyList(),
    val total: Int = 0,
)

data class SearchTargetOption(
    val key: String?,
    val label: String,
)

val SEARCH_TARGET_OPTIONS: List<SearchTargetOption> = listOf(
    SearchTargetOption(null, "全部"),
    SearchTargetOption("song", "歌曲"),
    SearchTargetOption("video", "视频"),
    SearchTargetOption("vocalist", "歌手"),
    SearchTargetOption("producer", "P主"),
    SearchTargetOption("synthesizer", "合成器"),
    SearchTargetOption("uploader", "UP主"),
    SearchTargetOption("tag", "标签"),
)

fun searchTargetLabel(type: String?): String =
    SEARCH_TARGET_OPTIONS.firstOrNull { it.key == type }?.label ?: (type ?: "全部")
