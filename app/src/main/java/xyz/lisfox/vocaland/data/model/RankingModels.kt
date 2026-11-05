package xyz.lisfox.vocaland.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RankingDto(
    val id: Int = 0,
    val board: String = "",
    val part: String = "",
    val issue: Int = 0,
    val rank: Int = 0,
    val count: Int = 0,
    val point: Double = 0.0,
    val pointBefore: Double = 0.0,
    val rate: Double = 0.0,
    val songId: Int = 0,
    val bvid: String = "",
    val view: Long = 0,
    val favorite: Long = 0,
    val coin: Long = 0,
    val like: Long = 0,
    val danmaku: Long = 0,
    val reply: Long = 0,
    val share: Long = 0,
    val viewRank: Int = 0,
    val favoriteRank: Int = 0,
    val coinRank: Int = 0,
    val likeRank: Int = 0,
    val danmakuRank: Int = 0,
    val replyRank: Int = 0,
    val shareRank: Int = 0,
    val seperates: List<Int> = emptyList(),
    val song: SongDto = SongDto(),
    val video: VideoDto? = null,
    val last: RankingDto? = null,
)

@Serializable
data class RankingListResponse(
    val data: List<RankingDto> = emptyList(),
    val total: Int = 0,
    val count: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
)

enum class RankingBoard(
    val key: String,
    val label: String,
    val supportsNewPart: Boolean = true,
) {
    Daily("vocaloid-daily", "日榜"),
    Weekly("vocaloid-weekly", "周榜"),
    Monthly("vocaloid-monthly", "月榜"),
    Annual("vocaloid-annual", "年榜", supportsNewPart = false),
    CoverWeekly("cover-weekly", "翻唱周榜", supportsNewPart = false),
}

enum class RankingOrder(val key: String, val label: String) {
    Point("point", "得分"),
    View("view", "播放"),
    Favorite("favorite", "收藏"),
    Coin("coin", "硬币"),
    Like("like", "点赞"),
    Danmaku("danmaku", "弹幕"),
    Reply("reply", "评论"),
    Share("share", "分享"),
}

enum class RankingTab(val key: String, val label: String) {
    Main("main", "主榜"),
    New("new", "新曲"),
    Vocalist("vocalist", "歌手排行"),
    Producer("producer", "P主排行"),
    Milestone("milestone", "里程碑"),
    ;

    val isSongTab: Boolean
        get() = this == Main || this == New
}

fun availableRankingTabs(board: RankingBoard): List<RankingTab> {
    val tabs = mutableListOf(RankingTab.Main)
    if (board.supportsNewPart) {
        tabs += RankingTab.New
    }
    if (board != RankingBoard.Annual && board != RankingBoard.CoverWeekly) {
        tabs += RankingTab.Vocalist
        tabs += RankingTab.Producer
    }
    if (board == RankingBoard.Daily || board == RankingBoard.Weekly) {
        tabs += RankingTab.Milestone
    }
    return tabs
}

data class RankingPage(
    val board: RankingBoard,
    val part: String,
    val order: RankingOrder,
    val issue: Int,
    val latestIssue: Int?,
    val page: Int,
    val total: Int,
    val totalPages: Int,
    val items: List<RankingDto>,
    val oldestIssue: Int = 1,
    val requestedIssue: Int? = null,
    val requestedPage: Int = 1,
    val clamped: Boolean = false,
)

fun rankingIssueDateRange(board: RankingBoard, issue: Int): String? {
    if (issue <= 0) return null
    val baseDaily = java.time.LocalDate.of(2024, 7, 3)
    val baseWeekly = java.time.LocalDate.of(2024, 8, 31)
    val baseMonthly = java.time.LocalDate.of(2024, 7, 1)
    val baseCoverWeekly = java.time.LocalDate.of(2026, 4, 1)
    return when (board) {
        RankingBoard.Daily -> {
            val start = baseDaily.plusDays((issue - 1).toLong())
            val end = baseDaily.plusDays(issue.toLong())
            "$start ~ $end"
        }

        RankingBoard.Weekly -> {
            val start = baseWeekly.plusDays(((issue - 1) * 7).toLong())
            val end = baseWeekly.plusDays((issue * 7).toLong())
            "$start ~ $end"
        }

        RankingBoard.Monthly -> {
            val start = baseMonthly.plusMonths((issue - 1).toLong())
            val end = baseMonthly.plusMonths(issue.toLong())
            "$start ~ $end"
        }

        RankingBoard.Annual -> {
            val start = java.time.LocalDate.of(issue, 1, 1)
            val end = java.time.LocalDate.of(issue + 1, 1, 1)
            "$start ~ $end"
        }

        RankingBoard.CoverWeekly -> {
            val start = baseCoverWeekly.plusDays(((issue - 1) * 7).toLong())
            val end = baseCoverWeekly.plusDays((issue * 7).toLong())
            "$start ~ $end"
        }
    }
}
