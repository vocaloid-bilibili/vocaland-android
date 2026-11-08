package xyz.lisfox.vocaland.data.community

import kotlinx.serialization.Serializable
import xyz.lisfox.vocaland.data.model.CommentUserDto
import xyz.lisfox.vocaland.data.model.ReplyToDto

@Serializable
data class CommunityCommentDto(
    val id: Int = 0,
    val content: String = "",
    val targetKey: String = "",
    val user: CommentUserDto = CommentUserDto(),
    val parentId: Int? = null,
    val replyTo: ReplyToDto? = null,
    val isDeleted: Boolean = false,
    val isAuthor: Boolean = false,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val replyCount: Int = 0,
    val createdAt: String = "",
    val mentions: List<CommentMentionDto> = emptyList(),
    val replies: List<CommunityCommentDto> = emptyList(),
)

/**
 * 正文里的 @提及，区间由服务端给出（UTF-16 下标记，与 Kotlin String 下标一致）。
 * kind 为 "all" 时是 @全体成员，没有可跳转的用户。
 */
@Serializable
data class CommentMentionDto(
    val kind: String = "user",
    val userId: Int = 0,
    val start: Int = 0,
    val end: Int = 0,
    val display: String = "",
)

/** `[[image:某首歌]]` 封面引用解析结果。 */
@Serializable
data class CommentImageRefDto(
    val kind: String = "",
    val key: String = "",
    val markup: String = "",
    val url: String = "",
    val alt: String = "",
    val href: String = "",
)

@Serializable
data class CommunityCommentListResponse(
    val data: List<CommunityCommentDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class PostCommentRequest(
    val targetKey: String,
    val content: String,
    val parentId: Int? = null,
    val replyToId: Int? = null,
)

@Serializable
data class PostCommentResponse(
    val id: Int = 0,
    val status: String = "",
)

@Serializable
data class CommentLikeResponse(
    val liked: Boolean = false,
    val likeCount: Int = 0,
)

@Serializable
data class UpsertRatingRequest(
    val targetKey: String,
    val score: Int,
    val comment: String? = null,
    val isAnonymous: Boolean = false,
)

@Serializable
class EmptyRequest
