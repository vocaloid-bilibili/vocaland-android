package xyz.lisfox.vocaland.data.community

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.lisfox.vocaland.data.model.CheckinResultDto
import xyz.lisfox.vocaland.data.model.LevelInfoDto
import xyz.lisfox.vocaland.data.model.RatingDto

interface CommunityExtrasApi {
    // 现有 CommunityApi.comments 的 CommentDto 不含 replies，这里单独读取完整楼中楼。
    @GET("comments")
    suspend fun commentsWithReplies(
        @Query("target_key") targetKey: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10,
    ): CommunityCommentListResponse

    @POST("comments")
    suspend fun postComment(@Body request: PostCommentRequest): PostCommentResponse

    @DELETE("comments/{id}")
    suspend fun deleteComment(@Path("id") id: Int)

    @POST("comments/{id}/like")
    suspend fun toggleCommentLike(
        @Path("id") id: Int,
        @Body request: EmptyRequest,
    ): CommentLikeResponse

    @GET("ratings/mine")
    suspend fun myRating(@Query("target_key") targetKey: String): JsonElement

    @POST("ratings")
    suspend fun upsertRating(@Body request: UpsertRatingRequest): RatingDto

    @DELETE("ratings")
    suspend fun deleteRating(@Query("target_key") targetKey: String)

    @GET("profile/{id}")
    suspend fun publicProfile(@Path("id") id: Int): JsonElement

    @GET("user/me/level")
    suspend fun myLevel(): LevelInfoDto

    @POST("user/me/checkin")
    suspend fun checkin(@Body request: EmptyRequest): CheckinResultDto
}
