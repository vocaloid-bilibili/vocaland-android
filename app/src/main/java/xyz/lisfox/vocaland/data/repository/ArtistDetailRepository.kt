package xyz.lisfox.vocaland.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import xyz.lisfox.vocaland.data.model.ArtistDetailDto
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.ArtistStatsSummaryDto
import xyz.lisfox.vocaland.data.model.SongPage
import xyz.lisfox.vocaland.data.remote.VocaLandApi

data class ArtistProfile(
    val artist: ArtistDetailDto,
    val stats: ArtistStatsSummaryDto?,
)

data class ArtistDetailContent(
    val profile: ArtistProfile,
    val songs: SongPage,
)

class ArtistDetailRepository(
    private val api: VocaLandApi,
) {
    suspend fun load(
        kind: ArtistKind,
        artistId: Int,
    ): Result<ArtistDetailContent> = runCatching {
        coroutineScope {
            val profile = async { loadProfile(kind, artistId) }
            val songs = async { loadSongs(kind, artistId, page = 1) }

            ArtistDetailContent(
                profile = profile.await().getOrThrow(),
                songs = songs.await().getOrThrow(),
            )
        }
    }

    suspend fun loadProfile(
        kind: ArtistKind,
        artistId: Int,
    ): Result<ArtistProfile> = runCatching {
        coroutineScope {
            val artist = async { api.artistDetail(kind.apiType, artistId).data }
            val stats = async {
                runCatching { api.artistStatsSummary(kind.apiType, artistId).data }
                    .getOrNull()
            }

            ArtistProfile(
                artist = artist.await(),
                stats = stats.await(),
            )
        }
    }

    suspend fun loadSongs(
        kind: ArtistKind,
        artistId: Int,
        page: Int,
        pageSize: Int = PAGE_SIZE,
    ): Result<SongPage> = runCatching {
        val response = api.songsByArtist(
            type = kind.apiType,
            id = artistId,
            page = page,
            pageSize = pageSize,
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
