package xyz.lisfox.vocaland.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import xyz.lisfox.vocaland.data.discovery.DiscoveryRepository
import xyz.lisfox.vocaland.data.discovery.TagArtistPage
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.SongPage
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.remote.VocaLandApi

data class TagDetailContent(
    val tag: TagItemDto,
    val hot: SongPage,
    val latest: SongPage,
    val artists: Map<ArtistKind, TagArtistPage>,
)

class TagDetailRepository(
    private val api: VocaLandApi,
    private val discovery: DiscoveryRepository = DiscoveryRepository(),
) {
    suspend fun load(tagId: Int): Result<TagDetailContent> = runCatching {
        val tag = api.tagDetail(tagId)
        val artistKinds = resolveArtistKinds(tag.scope)

        coroutineScope {
            val hotDeferred = async { loadSongs(tagId, page = 1, sort = "hot") }
            val latestDeferred = async { loadSongs(tagId, page = 1, sort = "latest") }
            val artistsDeferred = artistKinds.map { kind ->
                async {
                    kind to loadArtistPage(kind, tagId, page = 1)
                        .getOrDefault(TagArtistPage(emptyList(), 0, 1))
                }
            }

            TagDetailContent(
                tag = tag,
                hot = hotDeferred.await().getOrDefault(SongPage()),
                latest = latestDeferred.await().getOrDefault(SongPage()),
                artists = artistsDeferred.awaitAll().toMap(),
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
            pageSize = SONG_PAGE_SIZE,
            sort = sort,
        )
        SongPage(
            songs = response.data,
            total = response.total.coerceAtLeast(response.data.size),
        )
    }

    suspend fun loadArtistPage(
        kind: ArtistKind,
        tagId: Int,
        page: Int,
    ): Result<TagArtistPage> = discovery.loadTagArtists(
        artistType = kind.apiType,
        tagId = tagId,
        page = page,
        pageSize = ARTIST_PAGE_SIZE,
    )

    private fun resolveArtistKinds(scope: List<String>): List<ArtistKind> {
        if (scope.contains("all")) return ArtistKind.entries
        return ArtistKind.entries.filter { it.apiType in scope }
    }

    private companion object {
        const val SONG_PAGE_SIZE = 20
        const val ARTIST_PAGE_SIZE = 24
    }
}
