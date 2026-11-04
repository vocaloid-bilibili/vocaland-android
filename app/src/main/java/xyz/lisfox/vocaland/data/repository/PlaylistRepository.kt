package xyz.lisfox.vocaland.data.repository

import xyz.lisfox.vocaland.data.model.PlaylistDetailDto
import xyz.lisfox.vocaland.data.model.PublicPlaylistDto
import xyz.lisfox.vocaland.data.remote.VocaLandApi

data class PlaylistPage(
    val items: List<PublicPlaylistDto>,
    val total: Int,
    val page: Int,
    val perPage: Int,
)

class PlaylistRepository(
    private val v3: VocaLandApi,
) {
    suspend fun loadPublicPage(
        page: Int = 1,
        sort: String = "hot",
        keyword: String? = null,
    ): Result<PlaylistPage> = runCatching {
        val response = v3.publicPlaylists(
            page = page,
            sort = sort,
            keyword = keyword?.trim()?.takeIf { it.isNotEmpty() },
        )
        PlaylistPage(
            items = response.data,
            total = response.total,
            page = response.page,
            perPage = response.perPage,
        )
    }

    suspend fun loadDetail(id: Int): Result<PlaylistDetailDto> = runCatching {
        v3.playlistDetail(id).data
    }
}
