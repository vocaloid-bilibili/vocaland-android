package xyz.lisfox.vocaland.data.remote

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.lisfox.vocaland.data.model.CommentDto
import xyz.lisfox.vocaland.data.model.CommentListResponse
import xyz.lisfox.vocaland.data.model.DataEnvelope
import xyz.lisfox.vocaland.data.model.RatingDto
import xyz.lisfox.vocaland.data.model.RatingListResponse
import xyz.lisfox.vocaland.data.model.RatingSummaryDto

interface CommunityApi {
    @GET("ratings/recent")
    suspend fun recentRatings(@Query("limit") limit: Int = 9): DataEnvelope<List<RatingDto>>

    @GET("ratings")
    suspend fun ratings(
        @Query("target_key") targetKey: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10,
    ): RatingListResponse

    @GET("ratings/summary")
    suspend fun ratingSummaries(
        @Query("target_keys") targetKeys: String,
    ): DataEnvelope<List<RatingSummaryDto>>

    @GET("comments")
    suspend fun comments(
        @Query("target_key") targetKey: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10,
    ): CommentListResponse

    @GET("comments/recent")
    suspend fun recentComments(@Query("limit") limit: Int = 8): DataEnvelope<List<CommentDto>>

    @GET("user/{id}")
    suspend fun publicUser(@Path("id") id: Int): JsonElement

    @GET("ratings/users/{id}")
    suspend fun userRatings(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): RatingListResponse
}
