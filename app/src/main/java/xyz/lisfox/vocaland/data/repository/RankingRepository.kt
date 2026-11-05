package xyz.lisfox.vocaland.data.repository

import java.time.LocalDate
import kotlin.math.ceil
import xyz.lisfox.vocaland.data.discovery.ArtistRankingPage
import xyz.lisfox.vocaland.data.discovery.ArtistRankingSongsPage
import xyz.lisfox.vocaland.data.discovery.DiscoveryRepository
import xyz.lisfox.vocaland.data.discovery.MilestonePage
import xyz.lisfox.vocaland.data.discovery.roleAtLeast
import xyz.lisfox.vocaland.data.model.RankingBoard
import xyz.lisfox.vocaland.data.model.RankingOrder
import xyz.lisfox.vocaland.data.model.RankingPage
import xyz.lisfox.vocaland.data.remote.VocaLandApi

class RankingRepository(
    private val api: VocaLandApi,
    private val discovery: DiscoveryRepository = DiscoveryRepository(),
) {
    suspend fun loadRanking(
        board: RankingBoard,
        part: String,
        order: RankingOrder,
        issue: Int?,
        page: Int,
        role: String = "guest",
        pageSize: Int = PAGE_SIZE,
    ): Result<RankingPage> = runCatching {
        val latestIssue = runCatching { api.latestIssue(board.key).data }.getOrNull()
        val minIssue = if (board == RankingBoard.Annual) ANNUAL_MIN_ISSUE else 1
        val oldestIssue = resolveOldestIssue(board, role, latestIssue, minIssue)
        val maxPage = resolveMaxPage(board, role)

        var targetIssue = issue ?: latestIssue ?: 1
        if (latestIssue != null) {
            val lowerBound = minOf(oldestIssue, latestIssue)
            targetIssue = targetIssue.coerceIn(lowerBound, latestIssue)
        }
        val requestedPage = page.coerceIn(1, maxPage)
        var targetPage = requestedPage

        var response = api.ranking(
            board = board.key,
            part = part,
            issue = targetIssue,
            page = targetPage,
            pageSize = pageSize,
            orderType = order.key,
        )
        var actualPage = targetPage

        if (targetPage > 1 && response.data.isEmpty()) {
            targetPage = 1
            response = api.ranking(
                board = board.key,
                part = part,
                issue = targetIssue,
                page = 1,
                pageSize = pageSize,
                orderType = order.key,
            )
            actualPage = 1
        }

        val actualIssue = response.data.firstOrNull()?.issue?.takeIf { it > 0 }
            ?: targetIssue
        val total = response.total.coerceAtLeast(response.data.size)
        val totalPages = maxOf(1, ceil(total.toDouble() / pageSize).toInt())
        val clamped = (issue != null && issue != targetIssue) ||
            page != requestedPage

        RankingPage(
            board = board,
            part = part,
            order = order,
            issue = actualIssue,
            latestIssue = latestIssue,
            page = actualPage,
            total = total,
            totalPages = totalPages,
            items = response.data,
            oldestIssue = oldestIssue,
            requestedIssue = issue,
            requestedPage = page,
            clamped = clamped,
        )
    }

    suspend fun loadArtistRanking(
        board: RankingBoard,
        issue: Int?,
        artistType: String,
        page: Int,
        pageSize: Int = ARTIST_PAGE_SIZE,
    ): Result<ArtistRankingPage> = discovery.loadArtistRanking(
        board = board.key,
        issue = issue,
        artistType = artistType,
        page = page,
        pageSize = pageSize,
    )

    suspend fun loadArtistRankingSongs(
        board: RankingBoard,
        issue: Int,
        artistType: String,
        artistId: Int,
        page: Int,
        pageSize: Int = ARTIST_SONG_PAGE_SIZE,
    ): Result<ArtistRankingSongsPage> = discovery.loadArtistRankingSongs(
        board = board.key,
        issue = issue,
        artistType = artistType,
        artistId = artistId,
        page = page,
        pageSize = pageSize,
    )

    suspend fun loadMilestones(
        board: RankingBoard,
        issue: Int,
        page: Int,
        pageSize: Int = PAGE_SIZE,
    ): Result<MilestonePage> = when (board) {
        RankingBoard.Daily -> discovery.loadDailyMilestones(
            date = dailyDateKey(issue),
            page = page,
            pageSize = pageSize,
        )

        RankingBoard.Weekly -> discovery.loadWeeklyMilestones(
            issue = issue,
            page = page,
            pageSize = pageSize,
        )

        else -> Result.failure(IllegalArgumentException("该榜单没有里程碑数据"))
    }

    private fun resolveOldestIssue(
        board: RankingBoard,
        role: String,
        latestIssue: Int?,
        minIssue: Int,
    ): Int {
        if (latestIssue == null) return minIssue
        if (roleAtLeast(role, "sponsor")) return minIssue
        if (!roleAtLeast(role, "user")) return latestIssue
        val window = when (board) {
            RankingBoard.Daily -> 7
            RankingBoard.Weekly -> 5
            RankingBoard.Monthly -> 3
            else -> return minIssue
        }
        return maxOf(minIssue, latestIssue - window + 1)
    }

    private fun resolveMaxPage(board: RankingBoard, role: String): Int {
        if (roleAtLeast(role, "sponsor")) return Int.MAX_VALUE
        if (!roleAtLeast(role, "user")) return 1
        val itemLimit = when (board) {
            RankingBoard.Monthly, RankingBoard.Annual -> 200
            else -> 100
        }
        return maxOf(1, ceil(itemLimit.toDouble() / PAGE_SIZE).toInt())
    }

    private fun dailyDateKey(issue: Int): String =
        LocalDate.of(2024, 7, 3).plusDays(issue.toLong()).toString()

    private companion object {
        const val PAGE_SIZE = 20
        const val ARTIST_PAGE_SIZE = 20
        const val ARTIST_SONG_PAGE_SIZE = 10
        const val ANNUAL_MIN_ISSUE = 2025
    }
}
