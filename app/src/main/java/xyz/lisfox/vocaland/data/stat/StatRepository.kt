package xyz.lisfox.vocaland.data.stat

import kotlin.math.ceil
import xyz.lisfox.vocaland.data.model.SearchEntityDto
import xyz.lisfox.vocaland.data.remote.VocaLandApi

/**
 * 统计页 / 自定义排行共用的实体搜索（艺人 / 标签 / 歌曲选择控件）。
 * 唯一实现，两个 Repository 都委托到这里。
 */
suspend fun VocaLandApi.searchEntities(
    type: String,
    keyword: String,
    pageSize: Int = 8,
): Result<List<SearchEntityDto>> = runCatching {
    search(
        type = type,
        keyword = keyword.trim(),
        page = 1,
        pageSize = pageSize,
    ).data
}

class StatRepository(
    private val api: StatApi,
    private val v3: VocaLandApi,
) {
    suspend fun loadSongs(query: StatQuery): Result<StatPage> = runCatching {
        val response = api.songsList(StatFilterCodec.toStatParams(query))
        val total = response.total.coerceAtLeast(response.data.size)
        StatPage(
            items = response.data,
            total = total,
            page = query.page,
            totalPages = maxOf(1, ceil(total.toDouble() / STAT_PAGE_SIZE).toInt()),
            query = query,
        )
    }

    suspend fun searchEntities(
        type: String,
        keyword: String,
        pageSize: Int = 8,
    ): Result<List<SearchEntityDto>> = v3.searchEntities(type, keyword, pageSize)
}
