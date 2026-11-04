package xyz.lisfox.vocaland.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.lisfox.vocaland.data.model.ComprehensiveSearchResponse
import xyz.lisfox.vocaland.data.model.DataEnvelope
import xyz.lisfox.vocaland.data.model.EdCampaignDto
import xyz.lisfox.vocaland.data.model.HeroSlideDto
import xyz.lisfox.vocaland.data.model.MilestoneDto
import xyz.lisfox.vocaland.data.model.RankingListResponse
import xyz.lisfox.vocaland.data.model.SearchListResponse
import xyz.lisfox.vocaland.data.model.SiteStatsDto
import xyz.lisfox.vocaland.data.model.SongBriefDto
import xyz.lisfox.vocaland.data.model.SongListResponse
import xyz.lisfox.vocaland.data.model.TrendingCampaignDto

interface VocaLandApi {
    @GET("hero/slides")
    suspend fun heroSlides(): DataEnvelope<List<HeroSlideDto>>

    @GET("site/stats")
    suspend fun siteStats(): DataEnvelope<SiteStatsDto>

    @GET("milestones/recent")
    suspend fun recentMilestones(@Query("limit") limit: Int = 12): List<MilestoneDto>

    @GET("song/latest")
    suspend fun latestSongs(@Query("limit") limit: Int = 8): List<SongBriefDto>

    @GET("song/list")
    suspend fun songsList(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10,
        @Query("order_by") orderBy: String,
        @Query("order") order: String = "desc",
    ): SongListResponse

    @GET("song/on-this-day")
    suspend fun songsOnThisDay(
        @Query("month") month: Int,
        @Query("day") day: Int,
    ): SongListResponse

    @GET("ed/campaigns")
    suspend fun edCampaigns(): DataEnvelope<List<EdCampaignDto>>

    @GET("voting/trending/campaigns")
    suspend fun trendingCampaigns(): DataEnvelope<List<TrendingCampaignDto>>

    @GET("ranking/{board}/latest_issue")
    suspend fun latestIssue(@Path("board") board: String): DataEnvelope<Int>

    @GET("ranking/{board}/{part}/{issue}")
    suspend fun ranking(
        @Path("board") board: String,
        @Path("part") part: String,
        @Path("issue") issue: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("order_type") orderType: String = "point",
    ): RankingListResponse

    @GET("search/all")
    suspend fun comprehensiveSearch(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("entity_limit") entityLimit: Int = 5,
    ): ComprehensiveSearchResponse

    @GET("search/{type}")
    suspend fun search(
        @Path("type") type: String,
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): SearchListResponse
}
