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
)
