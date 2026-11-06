package xyz.lisfox.vocaland.data.repository

import xyz.lisfox.vocaland.data.model.BgmTrack
import xyz.lisfox.vocaland.data.model.BgmVideoDto
import xyz.lisfox.vocaland.data.model.PlayUrlRequest
import xyz.lisfox.vocaland.data.model.ResolvedBgmAudio
import xyz.lisfox.vocaland.data.model.VideoBatchRequest
import xyz.lisfox.vocaland.data.remote.PlayUrlApi
import xyz.lisfox.vocaland.data.remote.VocaLandApi
import xyz.lisfox.vocaland.util.artistLine
import xyz.lisfox.vocaland.util.bvidToAid
import xyz.lisfox.vocaland.util.coverUrl
import xyz.lisfox.vocaland.util.songDisplayName

class BgmRepository(
    private val v3: VocaLandApi,
    private val playUrlApi: PlayUrlApi,
) {
    suspend fun loadTracks(): Result<List<BgmTrack>> = runCatching {
        val bvids = v3.bgmPlaylist().data
        if (bvids.isEmpty()) return@runCatching emptyList()

        val videos = mutableListOf<BgmVideoDto?>()
        for (batch in bvids.chunked(VIDEO_BATCH_SIZE)) {
            videos += v3.videosByBvids(VideoBatchRequest(batch)).data
        }
        videos.mapNotNull { it?.toTrack() }
    }

    suspend fun resolveAudio(bvid: String): Result<ResolvedBgmAudio> = runCatching {
        val aid = bvidToAid(bvid)
        check(aid > 0) { "无法解析 BV 号" }
        val response = playUrlApi.resolve(PlayUrlRequest(aid))
        val url = response.audios
            ?.firstOrNull()
            ?.baseUrl
            ?.takeIf { it.isNotBlank() }
            ?: error("暂无可用音轨")
        ResolvedBgmAudio(url = url, cid = response.cid?.takeIf { it > 0 })
    }

    private fun BgmVideoDto.toTrack(): BgmTrack? {
        if (disabled) return null
        val song = song ?: return null
        return BgmTrack(
            songId = song.id,
            bvid = bvid,
            title = songDisplayName(song),
            artist = song.artistLine(),
            cover = thumbnail?.takeIf { it.isNotBlank() } ?: song.coverUrl(),
            durationSeconds = duration ?: 0,
        )
    }

    private companion object {
        const val VIDEO_BATCH_SIZE = 100
    }
}
