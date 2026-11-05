package xyz.lisfox.vocaland.data.discovery

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import xyz.lisfox.vocaland.data.model.ArtistBriefDto
import xyz.lisfox.vocaland.data.model.MilestoneDto
import xyz.lisfox.vocaland.data.model.TagItemDto
import kotlin.math.ceil

data class TagWallData(
    val groups: List<TagGroupedCategoryDto>,
    val categories: List<TagCategoryCountDto>,
    val totalTags: Int,
)

data class TagFlatPage(
    val tags: List<TagItemDto>,
    val total: Int,
    val totalPages: Int,
)

data class TagArtistPage(
    val artists: List<ArtistBriefDto>,
    val total: Int,
    val totalPages: Int,
)

data class ArtistRankingPage(
    val items: List<ArtistRankingItemDto>,
    val total: Int,
    val totalPages: Int,
    val issue: Int,
)

data class ArtistRankingSongsPage(
    val songs: List<ArtistRankingSongDetailDto>,
    val total: Int,
    val totalPages: Int,
)

data class MilestonePage(
    val items: List<MilestoneDto>,
    val total: Int,
    val totalPages: Int,
)

class DiscoveryRepository(
    private val api: DiscoveryApi = DiscoveryApiFactory.create(),
) {
    suspend fun loadTagWall(
        scope: String?,
        sort: String,
    ): Result<TagWallData> = runCatching {
        coroutineScope {
            val groupsDeferred = async {
                api.tagGroups(
                    entityType = scope,
                    mode = "category",
                    perCategoryLimit = PER_CATEGORY_LIMIT,
                    sort = sort,
                )
            }
            val summaryDeferred = async {
                runCatching { api.tagCategorySummary(entityType = scope) }.getOrNull()
            }

            val groups = groupsDeferred.await()
            val summary = summaryDeferred.await()
            TagWallData(
                groups = groups.data,
                categories = summary?.categories ?: emptyList(),
                totalTags = summary?.totalTags ?: groups.data.sumOf { it.total },
            )
        }
    }

    suspend fun loadTagList(
        scope: String?,
        category: String?,
        search: String?,
        sort: String,
        page: Int,
        pageSize: Int = TAG_PAGE_SIZE,
    ): Result<TagFlatPage> = runCatching {
        val response = api.tagList(
            category = category,
            search = search,
            entityType = scope,
            mode = "global",
            limit = pageSize,
            page = page,
            sort = sort,
        )
        val total = response.total.coerceAtLeast(response.data.size)
        TagFlatPage(
            tags = response.data,
            total = total,
            totalPages = totalPages(total, pageSize),
        )
    }

    suspend fun loadTagArtists(
        artistType: String,
        tagId: Int,
        page: Int,
        pageSize: Int = ARTIST_PAGE_SIZE,
    ): Result<TagArtistPage> = runCatching {
        val response = api.artistsByTag(
            type = artistType,
            tagId = tagId,
            page = page,
            pageSize = pageSize,
        )
        val total = response.total.coerceAtLeast(response.data.size)
        TagArtistPage(
            artists = response.data,
            total = total,
            totalPages = totalPages(total, pageSize),
        )
    }

    suspend fun loadArtistRanking(
        board: String,
        issue: Int?,
        artistType: String,
        page: Int,
        pageSize: Int = RANKING_PAGE_SIZE,
    ): Result<ArtistRankingPage> = runCatching {
        val response = api.artistRanking(
            artistType = artistType,
            board = board,
            issue = issue,
            page = page,
            pageSize = pageSize,
        )
        val total = response.total.coerceAtLeast(response.data.size)
        ArtistRankingPage(
            items = response.data,
            total = total,
            totalPages = totalPages(total, pageSize),
            issue = response.issue,
        )
    }

    suspend fun loadArtistRankingSongs(
        board: String,
        issue: Int,
        artistType: String,
        artistId: Int,
        page: Int,
        pageSize: Int = ARTIST_SONG_PAGE_SIZE,
    ): Result<ArtistRankingSongsPage> = runCatching {
        val response = api.artistRankingSongs(
            artistType = artistType,
            board = board,
            issue = issue,
            artistId = artistId,
            page = page,
            pageSize = pageSize,
        )
        val total = response.total.coerceAtLeast(response.data.size)
        ArtistRankingSongsPage(
            songs = response.data,
            total = total,
            totalPages = totalPages(total, pageSize),
        )
    }

    suspend fun loadDailyMilestones(
        date: String,
        page: Int,
        pageSize: Int = RANKING_PAGE_SIZE,
    ): Result<MilestonePage> = runCatching {
        milestonePage(api.dailyMilestones(date = date, page = page, pageSize = pageSize), pageSize)
    }

    suspend fun loadWeeklyMilestones(
        issue: Int,
        page: Int,
        pageSize: Int = RANKING_PAGE_SIZE,
    ): Result<MilestonePage> = runCatching {
        milestonePage(api.weeklyMilestones(issue = issue, page = page, pageSize = pageSize), pageSize)
    }

    private fun milestonePage(
        response: xyz.lisfox.vocaland.data.model.ListEnvelope<MilestoneDto>,
        pageSize: Int,
    ): MilestonePage {
        val total = response.total.coerceAtLeast(response.data.size)
        return MilestonePage(
            items = response.data,
            total = total,
            totalPages = totalPages(total, pageSize),
        )
    }

    private fun totalPages(total: Int, pageSize: Int): Int =
        maxOf(1, ceil(total.toDouble() / pageSize).toInt())

    private companion object {
        const val TAG_PAGE_SIZE = 48
        const val PER_CATEGORY_LIMIT = 30
        const val ARTIST_PAGE_SIZE = 24
        const val RANKING_PAGE_SIZE = 20
        const val ARTIST_SONG_PAGE_SIZE = 10
    }
}
