package xyz.lisfox.vocaland.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.remote.VocaLandApi

data class SongDetailContent(
    val song: SongDto,
    val tags: List<TagItemDto>,
)

class SongDetailRepository(
    private val api: VocaLandApi,
) {
    suspend fun load(songId: Int): Result<SongDetailContent> = runCatching {
        coroutineScope {
            val song = async { api.songDetail(songId).data }
            val tags = async {
                runCatching { api.entityTags("song", songId).data }
                    .getOrDefault(emptyList())
            }

            SongDetailContent(
                song = song.await(),
                tags = tags.await(),
            )
        }
    }
}
