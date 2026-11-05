package xyz.lisfox.vocaland.data.customranking

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.VideoDto
import xyz.lisfox.vocaland.data.stat.StatFilterState

const val CUSTOM_RANKING_PAGE_SIZE = 20
const val CUSTOM_RANKING_EXPORT_PAGE_SIZE = 100
const val DEFAULT_CUSTOM_RANKING_LIMIT = 100
const val MAX_CUSTOM_RANKING_LIMIT = 500

val CUSTOM_RANKING_STAT_KEYS: List<String> = listOf(
    "view",
    "favorite",
    "coin",
    "like",
    "danmaku",
    "reply",
    "share",
)

val CUSTOM_RANKING_STAT_LABELS: Map<String, String> = linkedMapOf(
    "view" to "播放",
    "favorite" to "收藏",
    "coin" to "硬币",
    "like" to "点赞",
    "danmaku" to "弹幕",
    "reply" to "评论",
    "share" to "分享",
)

enum class CustomRankingFormulaMode(val key: String, val label: String) {
    Preset("preset", "预设公式"),
    Linear("linear", "线性权重"),
    Expression("expression", "自定义表达式");

    companion object {
        fun fromKey(value: String?): CustomRankingFormulaMode =
            entries.firstOrNull { it.key == value } ?: Preset
    }
}

enum class CustomRankingSourceType(val key: String) {
    Songs("songs"),
    Playlist("playlist");

    companion object {
        fun fromKey(value: String?): CustomRankingSourceType =
            entries.firstOrNull { it.key == value } ?: Songs
    }
}

val CUSTOM_RANKING_PRESET_BOARDS: List<Pair<String, String>> = listOf(
    "vocaloid-daily" to "日榜",
    "vocaloid-weekly" to "周榜",
    "vocaloid-monthly" to "月榜",
    "vocaloid-annual" to "年榜",
    "special" to "特别榜",
    "cat-weekly" to "猫榜",
    "vc" to "VC 榜",
    "biliboard" to "B 站榜",
)

val DEFAULT_CUSTOM_RANKING_WEIGHTS: Map<String, Double> = linkedMapOf(
    "view" to 1.0,
    "favorite" to 20.0,
    "coin" to 10.0,
    "like" to 5.0,
)

@Serializable
data class CustomRankingFormula(
    val mode: String = CustomRankingFormulaMode.Preset.key,
    val board: String = "vocaloid-weekly",
    val weights: Map<String, Double> = DEFAULT_CUSTOM_RANKING_WEIGHTS,
    val expression: String = "",
    val oldDate: String = "",
    val newDate: String = "",
) {
    val modeEnum: CustomRankingFormulaMode get() = CustomRankingFormulaMode.fromKey(mode)

    fun toPayload(): CustomRankingFormulaPayloadDto = when (modeEnum) {
        CustomRankingFormulaMode.Preset -> CustomRankingFormulaPayloadDto(
            type = "preset",
            board = board,
        )

        CustomRankingFormulaMode.Expression -> CustomRankingFormulaPayloadDto(
            type = "expression",
            expression = expression.trim(),
        )

        CustomRankingFormulaMode.Linear -> {
            val effective = weights.filterValues { it.isFinite() && it != 0.0 }
            CustomRankingFormulaPayloadDto(
                type = "linear",
                linear = CustomRankingFormulaLinearDto(
                    weights = if (effective.isEmpty()) mapOf("view" to 1.0) else effective,
                ),
            )
        }
    }
}

@Serializable
data class CustomRankingSource(
    val type: String = CustomRankingSourceType.Songs.key,
    val playlistId: Int? = null,
) {
    val typeEnum: CustomRankingSourceType get() = CustomRankingSourceType.fromKey(type)
}

@Serializable
data class CustomRankingQuery(
    val source: CustomRankingSource = CustomRankingSource(),
    val formula: CustomRankingFormula = CustomRankingFormula(),
    val filter: StatFilterState = StatFilterState(),
    val includeAll: Boolean = false,
    val dedup: Boolean = true,
    val limit: Int = DEFAULT_CUSTOM_RANKING_LIMIT,
    val page: Int = 1,
)

@Serializable
data class CustomRankingFormulaPayloadDto(
    val type: String,
    val board: String? = null,
    val linear: CustomRankingFormulaLinearDto? = null,
    val expression: String? = null,
)

@Serializable
data class CustomRankingFormulaLinearDto(
    val weights: Map<String, Double> = emptyMap(),
)

@Serializable
data class CustomRankingRequestDto(
    val formula: CustomRankingFormulaPayloadDto,
    val oldDate: String? = null,
    val newDate: String? = null,
    val filter: JsonObject? = null,
    val source: String = CustomRankingSourceType.Songs.key,
    val playlistId: Int? = null,
    val useStats: List<String> = CUSTOM_RANKING_STAT_KEYS,
    val dedup: Boolean = true,
    val limit: Int = DEFAULT_CUSTOM_RANKING_LIMIT,
    val page: Int = 1,
    val pageSize: Int = CUSTOM_RANKING_PAGE_SIZE,
)

@Serializable
data class CustomRankingEntryDto(
    val id: Int = 0,
    val board: String = "",
    val part: String = "",
    val issue: Int = 0,
    val rank: Int = 0,
    val songId: Int = 0,
    val bvid: String = "",
    val count: Int? = null,
    val song: SongDto = SongDto(),
    val video: VideoDto? = null,
    val view: Long = 0,
    val favorite: Long = 0,
    val coin: Long = 0,
    val like: Long = 0,
    val danmaku: Long = 0,
    val reply: Long = 0,
    val share: Long = 0,
    val point: Double = 0.0,
    val viewRank: Int = 0,
    val favoriteRank: Int = 0,
    val coinRank: Int = 0,
    val likeRank: Int = 0,
    val danmakuRank: Int = 0,
    val replyRank: Int = 0,
    val shareRank: Int = 0,
)

@Serializable
data class CustomRankingResponseDto(
    val data: List<CustomRankingEntryDto> = emptyList(),
    val total: Int = 0,
    val oldDate: String? = null,
    val newDate: String? = null,
)

data class CustomRankingPage(
    val entries: List<CustomRankingEntryDto>,
    val total: Int,
    val page: Int,
    val totalPages: Int,
    val oldDate: String?,
    val newDate: String?,
    val query: CustomRankingQuery,
)

/** 数据项数值（唯一映射，CSV 与结果卡片共用）。 */
fun CustomRankingEntryDto.statValue(key: String): Long = when (key) {
    "view" -> view
    "favorite" -> favorite
    "coin" -> coin
    "like" -> like
    "danmaku" -> danmaku
    "reply" -> reply
    "share" -> share
    else -> 0
}

/** 数据项排名（唯一映射，CSV 与结果卡片共用）。 */
fun CustomRankingEntryDto.statRank(key: String): Int = when (key) {
    "view" -> viewRank
    "favorite" -> favoriteRank
    "coin" -> coinRank
    "like" -> likeRank
    "danmaku" -> danmakuRank
    "reply" -> replyRank
    "share" -> shareRank
    else -> 0
}
