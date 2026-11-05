package xyz.lisfox.vocaland.data.discovery

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.lisfox.vocaland.data.model.ArtistBriefDto
import xyz.lisfox.vocaland.data.model.ListEnvelope
import xyz.lisfox.vocaland.data.model.MilestoneDto

interface DiscoveryApi {
    @GET("tag/list")
    suspend fun tagGroups(
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("entityType") entityType: String? = null,
        @Query("mode") mode: String = "category",
        @Query("perCategoryLimit") perCategoryLimit: Int = 30,
        @Query("perCategoryPage") perCategoryPage: Int = 1,
        @Query("sort") sort: String = "count",
    ): TagGroupedListResponseDto

    @GET("tag/list")
    suspend fun tagList(
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("entityType") entityType: String? = null,
        @Query("mode") mode: String = "global",
        @Query("limit") limit: Int = 48,
        @Query("page") page: Int = 1,
        @Query("sort") sort: String = "count",
    ): TagFlatListResponseDto

    @GET("tag/category-summary")
    suspend fun tagCategorySummary(
        @Query("entityType") entityType: String? = null,
        @Query("search") search: String? = null,
    ): TagCategorySummaryDto

    @GET("{type}/list/by_tag")
    suspend fun artistsByTag(
        @Path("type") type: String,
        @Query("tag_id") tagId: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 24,
    ): ListEnvelope<ArtistBriefDto>

    @GET("ranking/artist")
    suspend fun artistRanking(
        @Query("artist_type") artistType: String,
        @Query("board") board: String,
        @Query("issue") issue: Int? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("song_limit") songLimit: Int = 10,
    ): ArtistRankingResponseDto

    @GET("ranking/artist/songs")
    suspend fun artistRankingSongs(
        @Query("artist_type") artistType: String,
        @Query("board") board: String,
        @Query("issue") issue: Int,
        @Query("artist_id") artistId: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10,
    ): ArtistRankingSongsResponseDto

    @GET("milestones/daily")
    suspend fun dailyMilestones(
        @Query("date") date: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): ListEnvelope<MilestoneDto>

    @GET("milestones/weekly")
    suspend fun weeklyMilestones(
        @Query("issue") issue: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): ListEnvelope<MilestoneDto>
}
