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
    val replies: List<CommunityCommentDto> = emptyList(),
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
