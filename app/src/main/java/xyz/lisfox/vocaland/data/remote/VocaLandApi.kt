package xyz.lisfox.vocaland.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.lisfox.vocaland.data.model.ArtistDetailDto
import xyz.lisfox.vocaland.data.model.ArtistStatsSummaryDto
import xyz.lisfox.vocaland.data.model.BgmVideoDto
import xyz.lisfox.vocaland.data.model.ComprehensiveSearchResponse
import xyz.lisfox.vocaland.data.model.DataEnvelope
import xyz.lisfox.vocaland.data.model.EdCampaignDto
import xyz.lisfox.vocaland.data.model.HeroSlideDto
import xyz.lisfox.vocaland.data.model.ListEnvelope
import xyz.lisfox.vocaland.data.model.MilestoneDto
import xyz.lisfox.vocaland.data.model.RankingListResponse
import xyz.lisfox.vocaland.data.model.SearchListResponse
import xyz.lisfox.vocaland.data.model.SiteStatsDto
import xyz.lisfox.vocaland.data.model.SongBriefDto
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.SongListResponse
import xyz.lisfox.vocaland.data.model.SongRankingStatsDto
import xyz.lisfox.vocaland.data.model.SongWithVideosListResponse
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.model.PlaylistDetailDto
import xyz.lisfox.vocaland.data.model.PublicPlaylistListResponse
import xyz.lisfox.vocaland.data.model.TrendingCampaignDto
import xyz.lisfox.vocaland.data.model.UserLitStatsDto
import xyz.lisfox.vocaland.data.model.VideoBatchRequest

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

    @GET("song/{id}")
    suspend fun songDetail(
        @Path("id") id: Int,
        @Query("include_all_video") includeAllVideo: Boolean = false,
    ): DataEnvelope<SongDto>

    @GET("{type}/{id}")
    suspend fun artistDetail(
        @Path("type") type: String,
        @Path("id") id: Int,
    ): DataEnvelope<ArtistDetailDto>

    @GET("{type}/{id}/stats/summary")
    suspend fun artistStatsSummary(
        @Path("type") type: String,
        @Path("id") id: Int,
    ): DataEnvelope<ArtistStatsSummaryDto>

    @GET("song/ranking")
    suspend fun songRanking(
        @Query("id") id: Int,
        @Query("board") board: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): RankingListResponse

    @GET("song/ranking-stats")
    suspend fun songRankingStats(
        @Query("id") id: Int,
        @Query("board") board: String,
    ): DataEnvelope<SongRankingStatsDto?>

    @GET("song/by_artist")
    suspend fun songsByArtist(
        @Query("type") type: String,
        @Query("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): SongWithVideosListResponse

    @GET("song/list/by_tag")
    suspend fun songsByTag(
        @Query("tag_id") tagId: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("sort") sort: String = "latest",
    ): SongWithVideosListResponse

    @GET("tag/{entityType}/{entityId}")
    suspend fun entityTags(
        @Path("entityType") entityType: String,
        @Path("entityId") entityId: Int,
    ): DataEnvelope<List<TagItemDto>>

    @GET("tag/{id}")
    suspend fun tagDetail(@Path("id") id: Int): TagItemDto

    @GET("lit/users/{id}")
    suspend fun userLitStats(@Path("id") id: Int): UserLitStatsDto

    @GET("playlist/public")
    suspend fun publicPlaylists(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 24,
        @Query("sort") sort: String = "hot",
        @Query("keyword") keyword: String? = null,
    ): PublicPlaylistListResponse

    @GET("playlist/{id}")
    suspend fun playlistDetail(@Path("id") id: Int): DataEnvelope<PlaylistDetailDto>

    @GET("bgm-playlist")
    suspend fun bgmPlaylist(): DataEnvelope<List<String>>

    @POST("video/batch")
    suspend fun videosByBvids(@Body request: VideoBatchRequest): ListEnvelope<BgmVideoDto?>
}
