package xyz.lisfox.vocaland.data.repository

import kotlin.math.ceil
import xyz.lisfox.vocaland.data.model.SearchEntityDto
import xyz.lisfox.vocaland.data.model.SearchGroupDto
import xyz.lisfox.vocaland.data.remote.VocaLandApi

data class SearchPageResult(
    val page: Int,
    val total: Int,
    val totalPages: Int,
    val groups: List<SearchGroupDto>,
)

class SearchRepository(
    private val api: VocaLandApi,
) {
    suspend fun searchAll(
        keyword: String,
        page: Int,
        pageSize: Int = PAGE_SIZE,
    ): Result<SearchPageResult> = runCatching {
        var response = api.comprehensiveSearch(
            keyword = keyword,
            page = page,
            limit = pageSize,
            entityLimit = ENTITY_LIMIT,
        )
        var actualPage = page

        if (page > 1 && primaryGroup(response.groups)?.data.isNullOrEmpty()) {
            response = api.comprehensiveSearch(
                keyword = keyword,
                page = 1,
                limit = pageSize,
                entityLimit = ENTITY_LIMIT,
            )
            actualPage = 1
        }

        val total = primaryGroup(response.groups)?.total ?: 0
        SearchPageResult(
            page = actualPage,
            total = total,
            totalPages = if (total > 0) totalPages(total, pageSize) else 1,
            groups = promoteHighMatchGroups(response.groups),
        )
    }

    suspend fun searchType(
        type: String,
        keyword: String,
        page: Int,
        pageSize: Int = PAGE_SIZE,
    ): Result<SearchPageResult> = runCatching {
        val direct = findEntityById(type, keyword)
        if (direct != null) {
            return@runCatching SearchPageResult(
                page = 1,
                total = 1,
                totalPages = 1,
                groups = listOf(
                    SearchGroupDto(
                        type = type,
                        total = 1,
                        data = listOf(direct),
                    ),
                ),
            )
        }

        var response = api.search(
            type = type,
            keyword = keyword,
            page = page,
            pageSize = pageSize,
        )
        var actualPage = page

        if (page > 1 && response.data.isEmpty()) {
            response = api.search(
                type = type,
                keyword = keyword,
                page = 1,
                pageSize = pageSize,
            )
            actualPage = 1
        }

        val total = response.total.coerceAtLeast(response.data.size)
        SearchPageResult(
            page = actualPage,
            total = total,
            totalPages = totalPages(total, pageSize),
            groups = listOf(
                SearchGroupDto(
                    type = type,
                    total = total,
                    data = response.data,
                ),
            ),
        )
    }

    private suspend fun findEntityById(
        type: String,
        keyword: String,
    ): SearchEntityDto? {
        if (!keyword.all { it.isDigit() }) return null
        val id = keyword.toIntOrNull()?.takeIf { it > 0 } ?: return null

        return when (type) {
            "song" -> runCatching { api.songDetail(id).data }.getOrNull()?.let { song ->
                SearchEntityDto(
                    id = song.id,
                    name = song.name,
                    displayName = song.displayName,
                    type = song.type,
                    thumbnail = song.thumbnail,
                    song = song,
                    songId = song.id,
                )
            }

            "vocalist", "producer", "synthesizer", "uploader" ->
                runCatching { api.artistDetail(type, id).data }.getOrNull()?.let { artist ->
                    SearchEntityDto(
                        id = artist.id,
                        name = artist.name,
                        displayName = artist.name,
                        picture = artist.picture,
                        abbreviation = artist.abbreviation,
                        uid = artist.uid,
                    )
                }

            else -> null
        }
    }

    private fun primaryGroup(groups: List<SearchGroupDto>): SearchGroupDto? =
        groups.firstOrNull { it.type == "song" } ?: groups.firstOrNull()

    private fun promoteHighMatchGroups(groups: List<SearchGroupDto>): List<SearchGroupDto> {
        val promoted = mutableListOf<SearchGroupDto>()
        val songGroups = mutableListOf<SearchGroupDto>()
        val rest = mutableListOf<SearchGroupDto>()

        groups.forEach { group ->
            if (group.type == "song") {
                songGroups += group
                return@forEach
            }

            val kept = group.data.filter { it.score >= HIGH_MATCH_SCORE }
            if (kept.isEmpty()) {
                rest += group
            } else {
                promoted += group.copy(data = kept, total = kept.size)
            }
        }

        return promoted + songGroups + rest
    }

    private fun totalPages(total: Int, pageSize: Int): Int =
        maxOf(1, ceil(total.toDouble() / pageSize).toInt())

    private companion object {
        const val PAGE_SIZE = 20
        const val ENTITY_LIMIT = 5
        const val HIGH_MATCH_SCORE = 100.0
    }
}
