package xyz.lisfox.vocaland.data.repository

import kotlin.math.ceil
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

        if (page > 1 && songGroup(response.groups)?.data.isNullOrEmpty()) {
            response = api.comprehensiveSearch(
                keyword = keyword,
                page = 1,
                limit = pageSize,
                entityLimit = ENTITY_LIMIT,
            )
            actualPage = 1
        }

        val total = songGroup(response.groups)?.total ?: 0
        SearchPageResult(
            page = actualPage,
            total = total,
            totalPages = if (total > 0) totalPages(total, pageSize) else 1,
            groups = response.groups,
        )
    }

    suspend fun searchType(
        type: String,
        keyword: String,
        page: Int,
        pageSize: Int = PAGE_SIZE,
    ): Result<SearchPageResult> = runCatching {
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

    private fun songGroup(groups: List<SearchGroupDto>): SearchGroupDto? =
        groups.firstOrNull { it.type == "song" }

    private fun totalPages(total: Int, pageSize: Int): Int =
        maxOf(1, ceil(total.toDouble() / pageSize).toInt())

    private companion object {
        const val PAGE_SIZE = 20
        const val ENTITY_LIMIT = 5
    }
}
