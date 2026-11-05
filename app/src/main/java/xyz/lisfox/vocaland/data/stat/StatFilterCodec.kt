package xyz.lisfox.vocaland.data.stat

import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import xyz.lisfox.vocaland.data.remote.AppJson

/**
 * 筛选状态 <-> 后端查询参数 / URL 查询串。
 *
 * 与 website packages/shared/src/stats 的编码保持一致：
 * - 数值范围 `min-max,min-`，多条用逗号；
 * - 艺人 / 标签 / 原曲 / 衍生曲用 `=id,+id,-id,?id,id` 逻辑串；
 * - 歌手用 `mode:id:engine:role` 紧凑串；
 * - types / copyright 用逗号分隔；
 * - foreign 二态：foreign / all。
 *
 * 统计页与自定义排行榜共用这一份实现（禁止各自再写一份映射）。
 */
object StatFilterCodec {
    private val rangeKeys = mapOf(
        "view_ranges" to { f: StatFilterState -> f.viewRanges },
        "favorite_ranges" to { f: StatFilterState -> f.favoriteRanges },
        "coin_ranges" to { f: StatFilterState -> f.coinRanges },
        "like_ranges" to { f: StatFilterState -> f.likeRanges },
        "danmaku_ranges" to { f: StatFilterState -> f.danmakuRanges },
        "reply_ranges" to { f: StatFilterState -> f.replyRanges },
        "share_ranges" to { f: StatFilterState -> f.shareRanges },
        "daily_count_ranges" to { f: StatFilterState -> f.dailyCountRanges },
        "weekly_count_ranges" to { f: StatFilterState -> f.weeklyCountRanges },
    )

    fun toStatParams(query: StatQuery): Map<String, String> = buildMap {
        put("page", query.page.coerceAtLeast(1).toString())
        put("page_size", STAT_PAGE_SIZE.toString())
        put("order_by", query.orderBy.key)
        put("order", query.order.key)
        if (query.includeAll) put("include_all", "true")
        putAll(toFilterParams(query.filter))
    }

    fun toFilterParams(filter: StatFilterState): Map<String, String> = buildMap {
        rangeKeys.forEach { (key, selector) ->
            val ranges = selector(filter)
            if (ranges.isNotEmpty()) {
                put(key, rangesToString(ranges))
            }
        }
        if (filter.dailyRank.isNotBlank()) put("daily_rank", filter.dailyRank.trim())
        if (filter.weeklyRank.isNotBlank()) put("weekly_rank", filter.weeklyRank.trim())
        if (filter.pubdate.isNotBlank()) put("pubdate", filter.pubdate.trim())
        val vocalistRules = vocalistRulesToParam(filter.vocalists)
        if (vocalistRules != null) put("vocalist_rules", vocalistRules)
        artistsToIds(filter.producers)?.let { put("producer_ids", it) }
        artistsToIds(filter.synthesizers)?.let { put("synthesizer_ids", it) }
        artistsToIds(filter.uploaders)?.let { put("uploader_ids", it) }
        artistsToIds(filter.tags)?.let { put("tag_ids", it) }
        artistsToIds(filter.originalSongs)?.let { put("original_song_ids", it) }
        artistsToIds(filter.derivativeSongs)?.let { put("derivative_song_ids", it) }
        if (filter.types.isNotEmpty()) put("types", filter.types.joinToString(","))
        if (filter.copyright.isNotEmpty()) {
            put("copyright", filter.copyright.joinToString(","))
        }
        if (filter.foreignOnly) put("foreign", "true")
    }

    /**
     * 筛选状态 -> POST /ranking/custom 的 filter 字段（JSON body）。
     *
     * 与 GET query 的区别：
     * - `foreign` 必须是布尔值，且**必须显式发送**（后端 custom 默认 true，
     *   省略会把「全部」误当「仅外语」）；
     * - `include_all` 是候选池收录范围，非筛选维度，由调用方显式传入。
     */
    fun toFilterBody(
        filter: StatFilterState,
        includeAll: Boolean = false,
    ): JsonObject = buildJsonObject {
        toFilterParams(filter).forEach { (key, value) ->
            if (key != "foreign") put(key, value)
        }
        put("foreign", filter.foreignOnly)
        if (includeAll) put("include_all", true)
    }

    fun rangesToString(ranges: List<StatRange>): String = ranges
        .map { range ->
            when {
                range.min != null && range.max != null -> "${range.min}-${range.max}"
                range.min != null -> "${range.min}-"
                range.max != null -> "-${range.max}"
                else -> ""
            }
        }
        .filter { it.isNotBlank() }
        .joinToString(",")

    fun stringToRanges(value: String?): List<StatRange> {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty()) return emptyList()
        return raw.split(",").mapNotNull { part ->
            if (part.isBlank()) return@mapNotNull null
            val pieces = part.split("-")
            val min = pieces.getOrNull(0)?.takeIf { it.isNotBlank() }?.toLongOrNull()
            val max = pieces.getOrNull(1)?.takeIf { it.isNotBlank() }?.toLongOrNull()
            if (min == null && max == null) null else StatRange(min = min, max = max)
        }
    }

    fun artistsToIds(artists: List<StatArtistRef>): String? {
        if (artists.isEmpty()) return null
        return artists.joinToString(",") { artist ->
            val prefix = when (artist.mode) {
                StatArtistRef.MODE_EXACT -> "="
                StatArtistRef.MODE_ONLY -> "+"
                StatArtistRef.MODE_NOT -> "-"
                StatArtistRef.MODE_ANY -> "?"
                else -> ""
            }
            "$prefix${artist.id}"
        }
    }

    fun vocalistRulesToParam(rules: List<StatVocalistRule>): String? {
        if (rules.isEmpty()) return null
        return rules.joinToString(",") { rule ->
            val prefix = when (rule.mode) {
                StatArtistRef.MODE_EXACT -> "="
                StatArtistRef.MODE_ONLY -> "+"
                StatArtistRef.MODE_NOT -> "-"
                StatArtistRef.MODE_ANY -> "?"
                else -> ""
            }
            val engine = when {
                rule.engine == "null" -> ":null"
                rule.engine == "any" -> ""
                rule.engine.startsWith("!") -> ":${rule.engine}"
                else -> ":${rule.engine}"
            }
            val role = if (rule.role == "all") "" else "@${rule.role}"
            "$prefix${rule.vocalist.id}$engine$role"
        }
    }

    /** 筛选状态 -> URL query 参数（与 web 的 FILTER_KEYS 兼容）。 */
    fun encodeFilterParams(filter: StatFilterState): Map<String, String> = buildMap {
        rangeKeys.forEach { (key, selector) ->
            val ranges = selector(filter)
            if (ranges.isNotEmpty()) put(key, rangesToString(ranges))
        }
        if (filter.dailyRank.isNotBlank()) put("dailyRank", filter.dailyRank.trim())
        if (filter.weeklyRank.isNotBlank()) put("weeklyRank", filter.weeklyRank.trim())
        if (filter.pubdate.isNotBlank()) put("pubdate", filter.pubdate.trim())
        if (filter.vocalists.isNotEmpty()) {
            put("vocalists", filter.vocalists.joinToString(",") { rule ->
                listOf(
                    rule.mode,
                    rule.vocalist.id.toString(),
                    rule.vocalist.name,
                    rule.engine,
                    rule.role,
                ).joinToString(":")
            })
        }
        putArtists("producers", filter.producers)
        putArtists("synthesizers", filter.synthesizers)
        putArtists("uploaders", filter.uploaders)
        putArtists("tags", filter.tags)
        putArtists("originalSongs", filter.originalSongs)
        putArtists("derivativeSongs", filter.derivativeSongs)
        if (filter.types.isNotEmpty()) put("types", AppJson.encodeToString(filter.types))
        if (filter.copyright.isNotEmpty()) {
            put("copyright", AppJson.encodeToString(filter.copyright))
        }
        put("foreign", if (filter.foreignOnly) "foreign" else "all")
    }

    fun decodeFilterParams(params: Map<String, String>): StatFilterState {
        fun ranges(key: String) = stringToRanges(params[key])

        return StatFilterState(
            viewRanges = ranges("viewRanges"),
            favoriteRanges = ranges("favoriteRanges"),
            coinRanges = ranges("coinRanges"),
            likeRanges = ranges("likeRanges"),
            danmakuRanges = ranges("danmakuRanges"),
            replyRanges = ranges("replyRanges"),
            shareRanges = ranges("shareRanges"),
            dailyCountRanges = ranges("dailyCountRanges"),
            weeklyCountRanges = ranges("weeklyCountRanges"),
            dailyRank = params["dailyRank"].orEmpty(),
            weeklyRank = params["weeklyRank"].orEmpty(),
            pubdate = params["pubdate"].orEmpty(),
            vocalists = decodeVocalists(params["vocalists"]),
            producers = decodeArtists(params["producers"]),
            synthesizers = decodeArtists(params["synthesizers"]),
            uploaders = decodeArtists(params["uploaders"]),
            tags = decodeArtists(params["tags"]),
            originalSongs = decodeArtists(params["originalSongs"]),
            derivativeSongs = decodeArtists(params["derivativeSongs"]),
            types = decodeJsonList(params["types"]),
            copyright = decodeJsonList(params["copyright"]),
            foreignOnly = params["foreign"] != "all",
        )
    }

    fun encodeQueryString(params: Map<String, String>): String =
        params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }

    fun parseQueryString(raw: String?): Map<String, String> {
        val value = raw?.trim()?.removePrefix("?") ?: return emptyMap()
        if (value.isEmpty()) return emptyMap()
        return value.split("&").mapNotNull { part ->
            if (part.isBlank()) return@mapNotNull null
            val index = part.indexOf('=')
            if (index < 0) {
                decode(part) to ""
            } else {
                decode(part.substring(0, index)) to decode(part.substring(index + 1))
            }
        }.toMap()
    }

    private fun MutableMap<String, String>.putArtists(
        key: String,
        artists: List<StatArtistRef>,
    ) {
        if (artists.isNotEmpty()) {
            put(key, AppJson.encodeToString(artists))
        }
    }

    private fun decodeArtists(raw: String?): List<StatArtistRef> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            AppJson.decodeFromString<List<StatArtistRef>>(raw)
        }.getOrDefault(emptyList())
    }

    private fun decodeVocalists(raw: String?): List<StatVocalistRule> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(",").mapNotNull { part ->
            val pieces = part.split(":")
            if (pieces.size < 4) return@mapNotNull null
            val mode = pieces[0].takeIf { it in StatArtistRef.MODES }
                ?: StatArtistRef.MODE_MUST
            val id = pieces[1].toIntOrNull() ?: return@mapNotNull null
            val name = pieces[2]
            val engine = pieces[3]
            val role = pieces.getOrNull(4)?.takeIf { it.isNotBlank() } ?: "all"
            StatVocalistRule(
                vocalist = StatArtistRef(id = id, name = name, mode = mode),
                mode = mode,
                engine = engine,
                role = role,
            )
        }
    }

    private inline fun <reified T> decodeJsonList(raw: String?): List<T> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            AppJson.decodeFromString<List<T>>(raw)
        }.getOrDefault(emptyList())
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }
            .getOrDefault(value)
}
