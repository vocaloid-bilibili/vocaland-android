package xyz.lisfox.vocaland.data.model

data class HomeContent(
    val heroSlides: List<HeroSlideDto> = emptyList(),
    val stats: SiteStatsDto? = null,
    val votingEntries: List<VotingEntryUi> = emptyList(),
    val votingSummary: String = "暂无进行中的投票",
    val milestones: List<MilestoneDto> = emptyList(),
    val ratings: List<RatingDto> = emptyList(),
    val comments: List<CommentDto> = emptyList(),
    val latestSongs: List<SongBriefDto> = emptyList(),
    val topStat: String = TOP_STATS.first().key,
    val topSongs: List<SongListItemDto> = emptyList(),
    val onThisDay: List<SongListItemDto> = emptyList(),
    val todayMonth: Int = 1,
    val todayDay: Int = 1,
    val failedSections: List<String> = emptyList(),
)

data class VotingEntryUi(
    val key: String,
    val typeLabel: String,
    val title: String,
    val deadlineText: String?,
    val isVoting: Boolean,
)

data class TopStat(
    val key: String,
    val label: String,
)

val TOP_STATS: List<TopStat> = listOf(
    TopStat("view", "播放"),
    TopStat("favorite", "收藏"),
    TopStat("coin", "硬币"),
    TopStat("like", "点赞"),
    TopStat("danmaku", "弹幕"),
    TopStat("reply", "评论"),
    TopStat("share", "分享"),
)
