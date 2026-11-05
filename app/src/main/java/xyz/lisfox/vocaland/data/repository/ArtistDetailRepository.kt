package xyz.lisfox.vocaland.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import xyz.lisfox.vocaland.data.artist.ArtistExtras
import xyz.lisfox.vocaland.data.artist.ArtistStatsApi
import xyz.lisfox.vocaland.data.artist.RelatedArtist
import xyz.lisfox.vocaland.data.model.ArtistBriefDto
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
    private val statsApi: ArtistStatsApi? = null,
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

    suspend fun loadExtras(
        kind: ArtistKind,
        artistId: Int,
    ): Result<ArtistExtras> = runCatching {
        coroutineScope {
            val stats = statsApi
            val tags = async {
                runCatching { api.entityTags(kind.apiType, artistId).data }
                    .getOrDefault(emptyList())
            }

            if (stats == null) {
                return@coroutineScope ArtistExtras(tags = tags.await())
            }

            val topSongs = async {
                runCatching { stats.topSongs(kind.apiType, artistId).data }
                    .getOrDefault(emptyList())
            }
            val latestSongs = async {
                runCatching { stats.latestSongs(kind.apiType, artistId).data }
                    .getOrDefault(emptyList())
            }
            val related = async { loadRelated(stats, kind, artistId) }

            ArtistExtras(
                topSongs = topSongs.await(),
                latestSongs = latestSongs.await(),
                related = related.await(),
                tags = tags.await(),
            )
        }
    }

    private suspend fun loadRelated(
        stats: ArtistStatsApi,
        kind: ArtistKind,
        artistId: Int,
    ): List<RelatedArtist> = coroutineScope {
        when (kind) {
            ArtistKind.Producer -> relatedVocalists(stats, artistId)

            ArtistKind.Vocalist -> {
                val synthesizers = async { relatedSynthesizers(stats, artistId) }
                val producers = async { relatedProducers(stats, artistId) }
                (synthesizers.await() + producers.await())
            }

            ArtistKind.Synthesizer -> {
                val vocalists = async { relatedVocalistsFromSynthesizer(stats, artistId) }
                val producers = async { relatedProducers(stats, artistId) }
                (vocalists.await() + producers.await())
            }

            ArtistKind.Uploader -> {
                val vocalists = async { relatedVocalistsFromUploader(stats, artistId) }
                val producers = async { relatedProducersFromUploader(stats, artistId) }
                (vocalists.await() + producers.await())
            }
        }.distinctBy { it.kind to it.id }
    }

    private suspend fun relatedVocalists(
        stats: ArtistStatsApi,
        artistId: Int,
    ): List<RelatedArtist> = runCatching {
        stats.producerVocalists(artistId).data.mapNotNull { item ->
            val vocalist = item.vocalist
            if (vocalist.id <= 0) return@mapNotNull null
            RelatedArtist(
                kind = ArtistKind.Vocalist,
                id = vocalist.id,
                name = vocalist.name,
                picture = vocalist.picture,
                count = item.count,
            )
        }
    }.getOrDefault(emptyList())

    private suspend fun relatedSynthesizers(
        stats: ArtistStatsApi,
        artistId: Int,
    ): List<RelatedArtist> = runCatching {
        stats.vocalistSynthesizers(artistId).data.mapNotNull { item ->
            val synthesizer = item.synthesizer ?: return@mapNotNull null
            if (synthesizer.id <= 0) return@mapNotNull null
            RelatedArtist(
                kind = ArtistKind.Synthesizer,
                id = synthesizer.id,
                name = synthesizer.name,
                picture = synthesizer.picture,
                count = item.count,
            )
        }
    }.getOrDefault(emptyList())

    private suspend fun relatedProducers(
        stats: ArtistStatsApi,
        artistId: Int,
    ): List<RelatedArtist> = runCatching {
        stats.vocalistProducers(artistId).data.mapNotNull { item ->
            item.producer.toRelatedProducer(item.count)
        }
    }.getOrDefault(emptyList())

    private suspend fun relatedVocalistsFromSynthesizer(
        stats: ArtistStatsApi,
        artistId: Int,
    ): List<RelatedArtist> = runCatching {
        stats.synthesizerVocalists(artistId).data.mapNotNull { item ->
            val vocalist = item.vocalist
            if (vocalist.id <= 0) return@mapNotNull null
            RelatedArtist(
                kind = ArtistKind.Vocalist,
                id = vocalist.id,
                name = vocalist.name,
                picture = vocalist.picture,
                count = item.count,
            )
        }
    }.getOrDefault(emptyList())

    private suspend fun relatedProducersFromSynthesizer(
        stats: ArtistStatsApi,
        artistId: Int,
    ): List<RelatedArtist> = runCatching {
        stats.synthesizerProducers(artistId).data.mapNotNull { item ->
            item.producer.toRelatedProducer(item.count)
        }
    }.getOrDefault(emptyList())

    private suspend fun relatedVocalistsFromUploader(
        stats: ArtistStatsApi,
        artistId: Int,
    ): List<RelatedArtist> = runCatching {
        stats.uploaderVocalists(artistId).data.mapNotNull { item ->
            val vocalist = item.vocalist
            if (vocalist.id <= 0) return@mapNotNull null
            RelatedArtist(
                kind = ArtistKind.Vocalist,
                id = vocalist.id,
                name = vocalist.name,
                picture = vocalist.picture,
                count = item.count,
            )
        }
    }.getOrDefault(emptyList())

    private suspend fun relatedProducersFromUploader(
        stats: ArtistStatsApi,
        artistId: Int,
    ): List<RelatedArtist> = runCatching {
        stats.uploaderProducers(artistId).data.mapNotNull { item ->
            item.producer.toRelatedProducer(item.count)
        }
    }.getOrDefault(emptyList())

    private fun ArtistBriefDto.toRelatedProducer(
        count: Int,
    ): RelatedArtist? {
        if (id <= 0) return null
        return RelatedArtist(
            kind = ArtistKind.Producer,
            id = id,
            name = name,
            picture = picture,
            count = count,
        )
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
