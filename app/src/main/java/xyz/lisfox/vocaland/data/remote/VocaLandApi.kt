package xyz.lisfox.vocaland.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
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
import xyz.lisfox.vocaland.data.model.EdCandidateDto
import xyz.lisfox.vocaland.data.model.EdCandidateListResponse
import xyz.lisfox.vocaland.data.model.EdCastVotesRequest
import xyz.lisfox.vocaland.data.model.EdEligibilityDto
import xyz.lisfox.vocaland.data.model.EdResultListResponse
import xyz.lisfox.vocaland.data.model.EdSubmitNominationRequest
import xyz.lisfox.vocaland.data.model.EdSubmitNominationResponse
import xyz.lisfox.vocaland.data.model.EdVoteDto
import xyz.lisfox.vocaland.data.model.EdVotingCandidatesResponse
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
import xyz.lisfox.vocaland.data.model.TrendingCandidateDto
import xyz.lisfox.vocaland.data.model.TrendingCandidateListResponse
import xyz.lisfox.vocaland.data.model.TrendingCastVotesRequest
import xyz.lisfox.vocaland.data.model.TrendingHistoryListResponse
import xyz.lisfox.vocaland.data.model.TrendingMyVotesDto
import xyz.lisfox.vocaland.data.model.TrendingNominateRequest
import xyz.lisfox.vocaland.data.model.TrendingResultListResponse
import xyz.lisfox.vocaland.data.model.TrendingTallyDto
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

    @GET("ed/campaign/{id}")
    suspend fun edCampaign(@Path("id") id: Int): EdCampaignDto

    @GET("ed/eligibility")
    suspend fun edEligibility(
        @Query("campaign_id") campaignId: Int,
        @Query("bvid") bvid: String,
    ): EdEligibilityDto

    @GET("ed/candidates")
    suspend fun edCandidates(
        @Query("campaign_id") campaignId: Int,
    ): EdCandidateListResponse

    @GET("ed/my-nomination")
    suspend fun edMyNomination(
        @Query("campaign_id") campaignId: Int,
    ): EdCandidateDto?

    @POST("ed/submit")
    suspend fun submitEdNomination(
        @Body request: EdSubmitNominationRequest,
    ): EdSubmitNominationResponse

    @DELETE("ed/submission/{candidateId}")
    suspend fun withdrawEdNomination(
        @Path("candidateId") candidateId: Int,
    )

    @GET("ed/voting/candidates")
    suspend fun edVotingCandidates(
        @Query("campaign_id") campaignId: Int,
    ): EdVotingCandidatesResponse

    @GET("ed/my-votes")
    suspend fun edMyVotes(@Query("campaign_id") campaignId: Int): List<EdVoteDto>

    @POST("ed/vote")
    suspend fun castEdVotes(@Body request: EdCastVotesRequest): List<EdVoteDto>

    @GET("ed/results/{id}")
    suspend fun edResults(@Path("id") id: Int): EdResultListResponse

    @GET("ed/results/{id}/ranking")
    suspend fun edFullRanking(@Path("id") id: Int): EdResultListResponse

    @GET("voting/trending/campaigns")
    suspend fun trendingCampaigns(): DataEnvelope<List<TrendingCampaignDto>>

    @GET("voting/trending/campaign")
    suspend fun trendingCampaign(@Query("campaign_id") campaignId: Int): TrendingCampaignDto

    @POST("voting/trending/nominate")
    suspend fun submitTrendingNomination(
        @Query("campaign_id") campaignId: Int,
        @Body request: TrendingNominateRequest,
    ): TrendingCandidateDto

    @DELETE("voting/trending/candidate")
    suspend fun withdrawTrendingNomination(
        @Query("candidate_id") candidateId: Int,
    )

    @GET("voting/trending/my-nominations")
    suspend fun trendingMyNominations(
        @Query("campaign_id") campaignId: Int,
    ): TrendingCandidateListResponse

    @GET("voting/trending/candidates")
    suspend fun trendingCandidates(
        @Query("campaign_id") campaignId: Int,
    ): TrendingCandidateListResponse

    @GET("voting/trending/tally")
    suspend fun trendingTally(@Query("campaign_id") campaignId: Int): TrendingTallyDto

    @GET("voting/trending/my-votes")
    suspend fun trendingMyVotes(
        @Query("campaign_id") campaignId: Int,
    ): TrendingMyVotesDto

    @POST("voting/trending/vote")
    suspend fun castTrendingVotes(
        @Query("campaign_id") campaignId: Int,
        @Body request: TrendingCastVotesRequest,
    ): TrendingMyVotesDto

    @GET("voting/trending/results")
    suspend fun trendingResults(
        @Query("campaign_id") campaignId: Int,
    ): TrendingResultListResponse

    @GET("voting/trending/history")
    suspend fun trendingHistory(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): TrendingHistoryListResponse

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
