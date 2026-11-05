package xyz.lisfox.vocaland.data.stat

import kotlinx.serialization.Serializable

/**
 * 角色口径，与 website shared/types/roles.ts 的 ROLE_LEVEL 一致。
 * 统计页与自定义排行榜共用：guest 只能看前 10、user 前 100、sponsor+ 不限。
 */
enum class VocaRole(val key: String, val level: Int) {
    Guest("guest", 0),
    User("user", 1),
    Sponsor("sponsor", 2),
    Worker("worker", 3),
    Admin("admin", 4),
    Superuser("superuser", 5);

    companion object {
        fun fromKey(value: String?): VocaRole =
            entries.firstOrNull { it.key == value } ?: Guest
    }
}

fun hasRole(current: VocaRole, required: VocaRole): Boolean =
    current.level >= required.level

@Serializable
data class StatRange(
    val min: Long? = null,
    val max: Long? = null,
) {
    val isSet: Boolean get() = min != null || max != null

    fun label(): String = when {
        min != null && max != null -> "$min~$max"
        min != null -> "≥$min"
        max != null -> "≤$max"
        else -> ""
    }
}

@Serializable
data class StatArtistRef(
    val id: Int,
    val name: String,
    val mode: String = MODE_MUST,
) {
    companion object {
        const val MODE_EXACT = "exact"
        const val MODE_ONLY = "only"
        const val MODE_MUST = "must"
        const val MODE_ANY = "any"
        const val MODE_NOT = "not"

        val MODES: List<String> = listOf(
            MODE_EXACT,
            MODE_ONLY,
            MODE_MUST,
            MODE_ANY,
            MODE_NOT,
        )

        fun modeLabel(mode: String): String = when (mode) {
            MODE_EXACT -> "精确"
            MODE_ONLY -> "限选"
            MODE_MUST -> "必选"
            MODE_ANY -> "任一"
            MODE_NOT -> "排除"
            else -> mode
        }

        fun nextMode(mode: String): String {
            val index = MODES.indexOf(mode)
            if (index < 0) return MODE_MUST
            return MODES[(index + 1) % MODES.size]
        }
    }
}

@Serializable
data class StatVocalistRule(
    val vocalist: StatArtistRef,
    val mode: String = StatArtistRef.MODE_MUST,
    val engine: String = "any",
    val role: String = "all",
)

@Serializable
data class StatFilterState(
    val viewRanges: List<StatRange> = emptyList(),
    val favoriteRanges: List<StatRange> = emptyList(),
    val coinRanges: List<StatRange> = emptyList(),
    val likeRanges: List<StatRange> = emptyList(),
    val danmakuRanges: List<StatRange> = emptyList(),
    val replyRanges: List<StatRange> = emptyList(),
    val shareRanges: List<StatRange> = emptyList(),
    val dailyCountRanges: List<StatRange> = emptyList(),
    val weeklyCountRanges: List<StatRange> = emptyList(),
    val dailyRank: String = "",
    val weeklyRank: String = "",
    val pubdate: String = "",
    val vocalists: List<StatVocalistRule> = emptyList(),
    val producers: List<StatArtistRef> = emptyList(),
    val synthesizers: List<StatArtistRef> = emptyList(),
    val uploaders: List<StatArtistRef> = emptyList(),
    val tags: List<StatArtistRef> = emptyList(),
    val originalSongs: List<StatArtistRef> = emptyList(),
    val derivativeSongs: List<StatArtistRef> = emptyList(),
    val types: List<String> = emptyList(),
    val copyright: List<Int> = emptyList(),
    val foreignOnly: Boolean = true,
) {
    fun activeCount(): Int = buildList {
        listOf(
            viewRanges,
            favoriteRanges,
            coinRanges,
            likeRanges,
            danmakuRanges,
            replyRanges,
            shareRanges,
            dailyCountRanges,
            weeklyCountRanges,
        ).forEach { if (it.isNotEmpty()) add(Unit) }
        if (dailyRank.isNotBlank()) add(Unit)
        if (weeklyRank.isNotBlank()) add(Unit)
        if (pubdate.isNotBlank()) add(Unit)
        if (vocalists.isNotEmpty()) add(Unit)
        if (producers.isNotEmpty()) add(Unit)
        if (synthesizers.isNotEmpty()) add(Unit)
        if (uploaders.isNotEmpty()) add(Unit)
        if (tags.isNotEmpty()) add(Unit)
        if (originalSongs.isNotEmpty()) add(Unit)
        if (derivativeSongs.isNotEmpty()) add(Unit)
        if (types.isNotEmpty()) add(Unit)
        if (copyright.isNotEmpty()) add(Unit)
        if (!foreignOnly) add(Unit)
    }.size
}

enum class StatOrderBy(val key: String, val label: String) {
    View("view", "播放"),
    Favorite("favorite", "收藏"),
    Coin("coin", "硬币"),
    Like("like", "点赞"),
    Danmaku("danmaku", "弹幕"),
    Reply("reply", "评论"),
    Share("share", "分享"),
    Pubdate("pubdate", "发布时间"),
    DailyCount("daily_count", "日刊上榜"),
    WeeklyCount("weekly_count", "周刊上榜");

    companion object {
        fun fromKey(value: String?): StatOrderBy =
            entries.firstOrNull { it.key == value } ?: View

        val guestAllowed: Set<StatOrderBy> = setOf(
            View,
            Favorite,
            Coin,
            Like,
            Danmaku,
            Reply,
            Share,
        )
    }
}

enum class StatOrder(val key: String, val label: String) {
    Asc("asc", "升序"),
    Desc("desc", "降序");

    companion object {
        fun fromKey(value: String?): StatOrder =
            entries.firstOrNull { it.key == value } ?: Desc
    }
}

data class StatQuery(
    val filter: StatFilterState = StatFilterState(),
    val page: Int = 1,
    val orderBy: StatOrderBy = StatOrderBy.View,
    val order: StatOrder = StatOrder.Desc,
    val includeAll: Boolean = false,
)

data class StatPage(
    val items: List<xyz.lisfox.vocaland.data.model.SongListItemDto>,
    val total: Int,
    val page: Int,
    val totalPages: Int,
    val query: StatQuery,
)

const val STAT_PAGE_SIZE = 50
const val STAT_GUEST_ITEM_LIMIT = 10
const val STAT_USER_ITEM_LIMIT = 100

/** 当前角色能访问的最大页码；null = 不限制（sponsor+） */
fun statMaxAccessiblePage(role: VocaRole, pageSize: Int = STAT_PAGE_SIZE): Int? {
    if (hasRole(role, VocaRole.Sponsor)) return null
    val limit = if (role == VocaRole.Guest) STAT_GUEST_ITEM_LIMIT else STAT_USER_ITEM_LIMIT
    return maxOf(1, (limit + pageSize - 1) / pageSize)
}

/**
 * 把请求查询按角色钳制成真正会生效的查询，与 website shared/stats/access.ts 一致。
 * 后端还会再钳一次；这里钳是为了让分页 / 排序 / 提示与实际拿到的数据一致。
 */
fun resolveStatAccess(role: VocaRole, requested: StatQuery): StatQuery {
    val maxPage = statMaxAccessiblePage(role)
    if (hasRole(role, VocaRole.Sponsor)) {
        return requested.copy(
            page = maxPage?.let { requested.page.coerceIn(1, it) } ?: requested.page.coerceAtLeast(1),
            includeAll = hasRole(role, VocaRole.Worker) && requested.includeAll,
        )
    }

    val isGuest = role == VocaRole.Guest
    return requested.copy(
        filter = StatFilterState(),
        page = maxPage?.let { requested.page.coerceIn(1, it) } ?: 1,
        orderBy = if (isGuest && requested.orderBy !in StatOrderBy.guestAllowed) {
            StatOrderBy.View
        } else {
            requested.orderBy
        },
        order = if (isGuest) StatOrder.Desc else requested.order,
        includeAll = false,
    )
}
