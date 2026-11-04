package xyz.lisfox.vocaland.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import xyz.lisfox.vocaland.data.model.CommentDto
import xyz.lisfox.vocaland.data.model.RankingBoard
import xyz.lisfox.vocaland.data.model.RankingDto
import xyz.lisfox.vocaland.data.model.RatingDto
import xyz.lisfox.vocaland.data.model.RatingSummaryDto
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.SongRankingStatsDto
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.remote.CommunityApi
import xyz.lisfox.vocaland.data.remote.VocaLandApi
import kotlin.math.ceil

data class SongDetailContent(
    val song: SongDto,
    val tags: List<TagItemDto>,
)

data class RatingPage(
    val items: List<RatingDto>,
    val total: Int,
    val page: Int,
    val totalPages: Int,
)

data class CommentPage(
    val items: List<CommentDto>,
    val total: Int,
    val page: Int,
    val totalPages: Int,
)

data class SongRankingContent(
    val stats: SongRankingStatsDto?,
    val history: List<RankingDto>,
    val total: Int,
)

class SongDetailRepository(
    private val v3: VocaLandApi,
    private val v2: CommunityApi,
) {
    suspend fun load(songId: Int): Result<SongDetailContent> = runCatching {
        coroutineScope {
            val song = async { v3.songDetail(songId).data }
            val tags = async {
                runCatching { v3.entityTags("song", songId).data }
                    .getOrDefault(emptyList())
            }

            SongDetailContent(
                song = song.await(),
                tags = tags.await(),
            )
        }
    }

    suspend fun loadRatingSummary(songId: Int): Result<RatingSummaryDto?> = runCatching {
        v2.ratingSummaries(songTargetKey(songId)).data.firstOrNull()
    }

    suspend fun loadRatings(
        songId: Int,
        page: Int,
    ): Result<RatingPage> = runCatching {
        val response = v2.ratings(
            targetKey = songTargetKey(songId),
            page = page,
            pageSize = RATING_PAGE_SIZE,
        )
        RatingPage(
            items = response.data,
            total = response.total.coerceAtLeast(response.data.size),
            page = page,
            totalPages = pagesFor(response.total.coerceAtLeast(response.data.size), RATING_PAGE_SIZE),
        )
    }

    suspend fun loadComments(
        songId: Int,
        page: Int,
    ): Result<CommentPage> = runCatching {
        val response = v2.comments(
            targetKey = songTargetKey(songId),
            page = page,
            pageSize = COMMENT_PAGE_SIZE,
        )
        CommentPage(
            items = response.data,
            total = response.total.coerceAtLeast(response.data.size),
            page = page,
            totalPages = pagesFor(response.total.coerceAtLeast(response.data.size), COMMENT_PAGE_SIZE),
        )
    }

    suspend fun loadRanking(
        songId: Int,
        board: RankingBoard,
    ): Result<SongRankingContent> = runCatching {
        coroutineScope {
            val stats = async {
                runCatching { v3.songRankingStats(songId, board.key).data }
                    .getOrNull()
            }
            val history = async {
                runCatching { v3.songRanking(songId, board.key, page = 1, pageSize = 20) }
            }

            val historyResult = history.await()
            val statsValue = stats.await()
            if (historyResult.isFailure && statsValue == null) {
                throw historyResult.exceptionOrNull() ?: IllegalStateException("排行数据加载失败")
            }

            val response = historyResult.getOrNull()
            val historyItems = response?.data.orEmpty()
            SongRankingContent(
                stats = statsValue,
                history = historyItems,
                total = response?.total?.coerceAtLeast(historyItems.size) ?: 0,
            )
        }
    }

    private fun pagesFor(total: Int, pageSize: Int): Int =
        maxOf(1, ceil(total.toDouble() / pageSize).toInt())

    private companion object {
        const val RATING_PAGE_SIZE = 10
        const val COMMENT_PAGE_SIZE = 10

        fun songTargetKey(songId: Int): String = "song:$songId"
    }
}
