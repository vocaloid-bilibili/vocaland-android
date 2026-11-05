package xyz.lisfox.vocaland.data.customranking

import kotlin.math.ceil
import kotlin.math.min
import xyz.lisfox.vocaland.data.model.PublicPlaylistDto
import xyz.lisfox.vocaland.data.model.SearchEntityDto
import xyz.lisfox.vocaland.data.stat.StatFilterCodec
import xyz.lisfox.vocaland.data.stat.searchEntities
import xyz.lisfox.vocaland.data.remote.VocaLandApi

class CustomRankingRepository(
    private val api: CustomRankingApi,
    private val v3: VocaLandApi,
) {
    suspend fun loadPage(query: CustomRankingQuery): Result<CustomRankingPage> = runCatching {
        val response = api.customRanking(
            toRequest(
                query = query,
                page = query.page,
                pageSize = CUSTOM_RANKING_PAGE_SIZE,
            ),
        )
        val total = response.total.coerceAtLeast(response.data.size)
        CustomRankingPage(
            entries = response.data,
            total = total,
            page = query.page,
            totalPages = maxOf(1, ceil(total.toDouble() / CUSTOM_RANKING_PAGE_SIZE).toInt()),
            oldDate = response.oldDate,
            newDate = response.newDate,
            query = query,
        )
    }

    suspend fun loadAll(query: CustomRankingQuery): Result<List<CustomRankingEntryDto>> =
        runCatching {
            val target = min(query.limit.coerceAtLeast(1), MAX_CUSTOM_RANKING_LIMIT)
            val first = api.customRanking(
                toRequest(
                    query = query,
                    page = 1,
                    pageSize = CUSTOM_RANKING_EXPORT_PAGE_SIZE,
                ),
            )
            val all = first.data.toMutableList()
            val totalPages = min(
                maxOf(1, ceil(first.total.toDouble() / CUSTOM_RANKING_EXPORT_PAGE_SIZE).toInt()),
                ceil(target.toDouble() / CUSTOM_RANKING_EXPORT_PAGE_SIZE).toInt(),
            )
            var page = 2
            while (page <= totalPages && all.size < target) {
                val response = api.customRanking(
                    toRequest(
                        query = query,
                        page = page,
                        pageSize = CUSTOM_RANKING_EXPORT_PAGE_SIZE,
                    ),
                )
                if (response.data.isEmpty()) break
                all.addAll(response.data)
                page += 1
            }
            all.take(target)
        }

    suspend fun searchEntities(
        type: String,
        keyword: String,
        pageSize: Int = 8,
    ): Result<List<SearchEntityDto>> = v3.searchEntities(type, keyword, pageSize)

    suspend fun loadPlaylists(
        page: Int = 1,
        keyword: String? = null,
    ): Result<List<PublicPlaylistDto>> = runCatching {
        v3.publicPlaylists(
            page = page.coerceAtLeast(1),
            perPage = 20,
            sort = "hot",
            keyword = keyword?.trim()?.takeIf { it.isNotEmpty() },
        ).data
    }

    fun toRequest(
        query: CustomRankingQuery,
        page: Int,
        pageSize: Int,
    ): CustomRankingRequestDto {
        val formula = query.formula
        val isPlaylist = query.source.typeEnum == CustomRankingSourceType.Playlist
        return CustomRankingRequestDto(
            formula = formula.toPayload(),
            oldDate = formula.oldDate.trim().takeIf { it.isNotEmpty() },
            newDate = formula.newDate.trim().takeIf { it.isNotEmpty() },
            filter = if (isPlaylist) {
                null
            } else {
                StatFilterCodec.toFilterBody(query.filter, query.includeAll)
            },
            source = query.source.typeEnum.key,
            playlistId = query.source.playlistId.takeIf { isPlaylist && it != null && it > 0 },
            useStats = CUSTOM_RANKING_STAT_KEYS,
            dedup = query.dedup,
            limit = query.limit.coerceIn(1, MAX_CUSTOM_RANKING_LIMIT),
            page = page.coerceAtLeast(1),
            pageSize = pageSize,
        )
    }
}
