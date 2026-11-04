package xyz.lisfox.vocaland.data.repository

import kotlin.math.ceil
import xyz.lisfox.vocaland.data.model.RankingBoard
import xyz.lisfox.vocaland.data.model.RankingOrder
import xyz.lisfox.vocaland.data.model.RankingPage
import xyz.lisfox.vocaland.data.remote.VocaLandApi

class RankingRepository(
    private val api: VocaLandApi,
) {
    suspend fun loadRanking(
        board: RankingBoard,
        part: String,
        order: RankingOrder,
        issue: Int?,
        page: Int,
        pageSize: Int = PAGE_SIZE,
    ): Result<RankingPage> = runCatching {
        val latestIssue = if (issue == null) {
            runCatching { api.latestIssue(board.key).data }.getOrNull()
        } else {
            null
        }
        val targetIssue = issue ?: latestIssue ?: 1

        var response = api.ranking(
            board = board.key,
            part = part,
            issue = targetIssue,
            page = page,
            pageSize = pageSize,
            orderType = order.key,
        )
        var actualPage = page

        if (page > 1 && response.data.isEmpty()) {
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
        )
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
