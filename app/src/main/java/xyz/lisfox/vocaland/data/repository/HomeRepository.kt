package xyz.lisfox.vocaland.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import xyz.lisfox.vocaland.data.model.EdCampaignDto
import xyz.lisfox.vocaland.data.model.HomeContent
import xyz.lisfox.vocaland.data.model.SongListItemDto
import xyz.lisfox.vocaland.data.model.TrendingCampaignDto
import xyz.lisfox.vocaland.data.model.TOP_STATS
import xyz.lisfox.vocaland.data.model.VotingEntryUi
import xyz.lisfox.vocaland.data.remote.CommunityApi
import xyz.lisfox.vocaland.data.remote.VocaLandApi
import xyz.lisfox.vocaland.util.deadlineText
import xyz.lisfox.vocaland.util.parseIsoMillis
import java.time.LocalDate
import java.time.ZoneId

class HomeRepository(
    private val v3: VocaLandApi,
    private val v2: CommunityApi,
) {
    suspend fun loadHome(): HomeContent = coroutineScope {
        val today = LocalDate.now(ZoneId.of("Asia/Shanghai"))
        val topStat = TOP_STATS.random()

        val heroDeferred = async { runCatching { v3.heroSlides().data } }
        val statsDeferred = async { runCatching { v3.siteStats().data } }
        val milestonesDeferred = async { runCatching { v3.recentMilestones() } }
        val latestDeferred = async { runCatching { v3.latestSongs() } }
        val topSongsDeferred = async {
            runCatching { v3.songsList(pageSize = 10, orderBy = topStat.key).data }
        }
        val onThisDayDeferred = async {
            runCatching {
                v3.songsOnThisDay(month = today.monthValue, day = today.dayOfMonth).data
            }
        }
        val ratingsDeferred = async { runCatching { v2.recentRatings().data } }
        val commentsDeferred = async { runCatching { v2.recentComments().data } }
        val playlistsDeferred = async {
            runCatching {
                v3.publicPlaylists(page = 1, perPage = 10, sort = "new").data
            }
        }
        val edDeferred = async { runCatching { v3.edCampaigns().data } }
        val trendingDeferred = async { runCatching { v3.trendingCampaigns().data } }

        val hero = heroDeferred.await()
        val stats = statsDeferred.await()
        val milestones = milestonesDeferred.await()
        val latest = latestDeferred.await()
        val topSongs = topSongsDeferred.await()
        val onThisDay = onThisDayDeferred.await()
        val ratings = ratingsDeferred.await()
        val comments = commentsDeferred.await()
        val playlists = playlistsDeferred.await()
        val ed = edDeferred.await()
        val trending = trendingDeferred.await()

        val failures = buildList {
            if (hero.isFailure) add("主视觉")
            if (stats.isFailure) add("本站统计")
            if (milestones.isFailure) add("最近里程碑")
            if (latest.isFailure) add("最新入库")
            if (topSongs.isFailure) add("数据总榜")
            if (onThisDay.isFailure) add("历史上的今天")
            if (ratings.isFailure) add("最近评分")
            if (comments.isFailure) add("最近评论")
            if (playlists.isFailure) add("歌单推荐")
        }

        val voting = buildVotingEntries(
            ed = ed.getOrDefault(emptyList()),
            trending = trending.getOrDefault(emptyList()),
        )

        HomeContent(
            heroSlides = hero.getOrDefault(emptyList()),
            stats = stats.getOrNull(),
            votingEntries = voting.first,
            votingSummary = voting.second,
            milestones = milestones.getOrDefault(emptyList()),
            ratings = ratings.getOrDefault(emptyList()),
            comments = comments.getOrDefault(emptyList()),
            playlists = playlists.getOrDefault(emptyList()),
            latestSongs = latest.getOrDefault(emptyList()),
            topStat = topStat.key,
            topSongs = topSongs.getOrDefault(emptyList()),
            onThisDay = onThisDay.getOrDefault(emptyList()),
            todayMonth = today.monthValue,
            todayDay = today.dayOfMonth,
            failedSections = failures,
        )
    }

    suspend fun loadTopSongs(stat: String): Result<List<SongListItemDto>> = runCatching {
        v3.songsList(pageSize = 10, orderBy = stat).data
    }

    private fun buildVotingEntries(
        ed: List<EdCampaignDto>,
        trending: List<TrendingCampaignDto>,
    ): Pair<List<VotingEntryUi>, String> {
        val edVoting = ed.filter { it.status == "voting" }
        val edNomination = ed.filter { it.status == "nomination" }
        val trendingVoting = trending.filter { it.status == "voting" }
        val trendingNomination = trending.filter { it.status == "nomination" }

        val voting = edVoting.map { it.toEntry() } + trendingVoting.map { it.toEntry() }
        val nomination = edNomination.map { it.toEntry() } + trendingNomination.map { it.toEntry() }

        val useVoting = voting.isNotEmpty()
        val entries = if (useVoting) voting else nomination
        val summary = when {
            voting.isNotEmpty() -> "${voting.size} 个投票进行中"
            nomination.isNotEmpty() -> "${nomination.size} 个提名进行中"
            else -> "暂无进行中的投票"
        }

        return entries.sortedBy { it.deadlineMillis ?: Long.MAX_VALUE }
            .map { it.entry } to summary
    }

    private fun EdCampaignDto.toEntry(): VotingEntryWithDeadline {
        val isVoting = status == "voting"
        val end = if (isVoting) voteEnd else submitEnd
        return VotingEntryWithDeadline(
            entry = VotingEntryUi(
                key = "ed-$id",
                typeLabel = "ED 投票",
                title = "${boardLabel(board)} · 第 $issue 期",
                deadlineText = deadlineText(end),
                isVoting = isVoting,
            ),
            deadlineMillis = parseIsoMillis(end),
        )
    }

    private fun TrendingCampaignDto.toEntry(): VotingEntryWithDeadline {
        val isVoting = status == "voting"
        val end = if (isVoting) voteEnd else submitEnd
        return VotingEntryWithDeadline(
            entry = VotingEntryUi(
                key = "trending-$id",
                typeLabel = "人气投票",
                title = "第 $issue 期 · ${entityKindLabel(entityKind)}",
                deadlineText = deadlineText(end),
                isVoting = isVoting,
            ),
            deadlineMillis = parseIsoMillis(end),
        )
    }

    private fun boardLabel(board: String): String = when (board) {
        "vocaloid-weekly" -> "周刊"
        "vocaloid-monthly" -> "月刊"
        "vocaloid-daily" -> "日刊"
        else -> board
    }

    private fun entityKindLabel(kind: String): String = when (kind) {
        "song" -> "歌曲"
        "producer" -> "P主"
        "vocalist" -> "歌手"
        else -> kind
    }

    private data class VotingEntryWithDeadline(
        val entry: VotingEntryUi,
        val deadlineMillis: Long?,
    )
}
