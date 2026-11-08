package xyz.lisfox.vocaland.data.community

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import xyz.lisfox.vocaland.data.model.CheckinResultDto
import xyz.lisfox.vocaland.data.model.LevelInfoDto
import xyz.lisfox.vocaland.data.model.PublicProfileResponseDto
import xyz.lisfox.vocaland.data.model.RatingDto
import xyz.lisfox.vocaland.data.model.RatingSummaryDto
import xyz.lisfox.vocaland.data.model.WalletDto
import xyz.lisfox.vocaland.data.remote.AppJson
import xyz.lisfox.vocaland.data.remote.CommunityApi
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

data class CommunityRatingPage(
    val items: List<RatingDto>,
    val total: Int,
    val page: Int,
    val totalPages: Int,
)

data class CommunityCommentPage(
    val items: List<CommunityCommentDto>,
    val total: Int,
    val page: Int,
    val totalPages: Int,
)

class CommunityRepository(
    private val v2: CommunityApi,
    private val extras: CommunityExtrasApi,
) {
    // 封面引用解析结果按 query 长缓存：同一条评论可能在多处出现，避免重复请求
    private val imageRefCache = ConcurrentHashMap<String, CommentImageRefDto>()

    suspend fun resolveCommentImageRef(query: String): CommentImageRefDto? {
        val normalized = query.trim()
        if (normalized.isEmpty()) return null
        imageRefCache[normalized]?.let { return it }
        return runCatching { extras.commentImageRef(normalized) }
            .getOrNull()
            ?.takeIf { it.url.isNotBlank() }
            ?.also { imageRefCache[normalized] = it }
    }

    suspend fun loadRatingSummary(targetKey: String): Result<RatingSummaryDto?> = runCatching {
        v2.ratingSummaries(targetKey).data.firstOrNull()
    }

    suspend fun loadRatings(
        targetKey: String,
        page: Int,
    ): Result<CommunityRatingPage> = runCatching {
        val response = v2.ratings(
            targetKey = targetKey,
            page = page,
            pageSize = RATING_PAGE_SIZE,
        )
        val total = response.total.coerceAtLeast(response.data.size)
        CommunityRatingPage(
            items = response.data,
            total = total,
            page = page,
            totalPages = pagesFor(total, RATING_PAGE_SIZE),
        )
    }

    suspend fun loadComments(
        targetKey: String,
        page: Int,
    ): Result<CommunityCommentPage> = runCatching {
        val response = extras.commentsWithReplies(
            targetKey = targetKey,
            page = page,
            pageSize = COMMENT_PAGE_SIZE,
        )
        val total = response.total.coerceAtLeast(response.data.size)
        CommunityCommentPage(
            items = response.data,
            total = total,
            page = page,
            totalPages = pagesFor(total, COMMENT_PAGE_SIZE),
        )
    }

    suspend fun loadMyRating(targetKey: String): Result<RatingDto?> = runCatching {
        val element = extras.myRating(targetKey)
        val payload = unwrap(element)
        if (payload == null || payload is JsonNull) {
            null
        } else {
            runCatching {
                AppJson.decodeFromJsonElement(RatingDto.serializer(), payload)
            }.getOrNull()?.takeIf { it.id.isNotBlank() }
        }
    }

    suspend fun submitRating(
        targetKey: String,
        score: Int,
        comment: String?,
        isAnonymous: Boolean,
    ): Result<RatingDto> = runCatching {
        extras.upsertRating(
            UpsertRatingRequest(
                targetKey = targetKey,
                score = score,
                comment = comment?.trim()?.takeIf { it.isNotEmpty() },
                isAnonymous = isAnonymous,
            ),
        )
    }

    suspend fun withdrawRating(targetKey: String): Result<Unit> = runCatching {
        extras.deleteRating(targetKey)
    }

    suspend fun postComment(
        targetKey: String,
        content: String,
        parentId: Int?,
        replyToId: Int?,
    ): Result<Unit> = runCatching {
        extras.postComment(
            PostCommentRequest(
                targetKey = targetKey,
                content = content,
                parentId = parentId,
                replyToId = replyToId,
            ),
        )
        Unit
    }

    suspend fun deleteComment(commentId: Int): Result<Unit> = runCatching {
        extras.deleteComment(commentId)
        Unit
    }

    suspend fun toggleCommentLike(commentId: Int): Result<CommentLikeResponse> = runCatching {
        extras.toggleCommentLike(commentId, EmptyRequest())
    }

    suspend fun loadProfile(userId: Int): Result<PublicProfileResponseDto?> = runCatching {
        val payload = unwrap(extras.publicProfile(userId))
        if (payload == null || payload is JsonNull) {
            null
        } else {
            runCatching {
                AppJson.decodeFromJsonElement(
                    PublicProfileResponseDto.serializer(),
                    payload,
                )
            }.getOrNull()
        }
    }

    suspend fun loadWallet(): Result<WalletDto> = runCatching { v2.myWallet() }

    suspend fun loadLevel(): Result<LevelInfoDto> = runCatching { extras.myLevel() }

    suspend fun checkin(): Result<CheckinResultDto> = runCatching {
        extras.checkin(EmptyRequest())
    }

    private fun unwrap(element: JsonElement): JsonElement? {
        val objectValue = element as? JsonObject ?: return element
        return objectValue["data"] ?: objectValue
    }

    private fun pagesFor(total: Int, pageSize: Int): Int =
        maxOf(1, ceil(total.toDouble() / pageSize).toInt())

    private companion object {
        const val RATING_PAGE_SIZE = 10
        const val COMMENT_PAGE_SIZE = 10
    }
}
