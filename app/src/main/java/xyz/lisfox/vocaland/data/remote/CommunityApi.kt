package xyz.lisfox.vocaland.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import xyz.lisfox.vocaland.data.model.CommentDto
import xyz.lisfox.vocaland.data.model.DataEnvelope
import xyz.lisfox.vocaland.data.model.RatingDto

interface CommunityApi {
    @GET("ratings/recent")
    suspend fun recentRatings(@Query("limit") limit: Int = 9): DataEnvelope<List<RatingDto>>

    @GET("comments/recent")
    suspend fun recentComments(@Query("limit") limit: Int = 8): DataEnvelope<List<CommentDto>>
}
