package xyz.lisfox.vocaland.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import xyz.lisfox.vocaland.data.model.SongPage
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.remote.VocaLandApi

data class TagDetailContent(
    val tag: TagItemDto,
    val songs: SongPage,
)

class TagDetailRepository(
    private val api: VocaLandApi,
) {
    suspend fun load(
        tagId: Int,
        sort: String,
    ): Result<TagDetailContent> = runCatching {
        coroutineScope {
            val tag = async { api.tagDetail(tagId) }
            val songs = async {
                api.songsByTag(
                    tagId = tagId,
                    page = 1,
                    pageSize = PAGE_SIZE,
                    sort = sort,
                )
            }
            val songResponse = songs.await()

            TagDetailContent(
                tag = tag.await(),
                songs = SongPage(
                    songs = songResponse.data,
                    total = songResponse.total.coerceAtLeast(songResponse.data.size),
                ),
            )
        }
    }

    suspend fun loadSongs(
        tagId: Int,
        page: Int,
        sort: String,
    ): Result<SongPage> = runCatching {
        val response = api.songsByTag(
            tagId = tagId,
            page = page,
            pageSize = PAGE_SIZE,
            sort = sort,
        )
        SongPage(
            songs = response.data,
            total = response.total.coerceAtLeast(response.data.size),
        )
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
