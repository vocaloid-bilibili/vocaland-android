package xyz.lisfox.vocaland.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoBatchRequest(
    val bvids: List<String>,
)

@Serializable
data class BgmVideoDto(
    val bvid: String = "",
    val title: String? = null,
    val thumbnail: String? = null,
    val duration: Int? = null,
    val disabled: Boolean = false,
    val song: SongDto? = null,
)

@Serializable
data class PlayUrlRequest(
    val aid: Long,
)

@Serializable
data class PlayUrlResponse(
    val cid: Long? = null,
    val audios: List<PlayUrlAudioDto>? = null,
)

@Serializable
data class PlayUrlAudioDto(
    val baseUrl: String? = null,
)

data class ResolvedBgmAudio(
    val url: String,
    val cid: Long?,
)

@Serializable
data class BgmTrack(
    val songId: Int,
    val bvid: String,
    val title: String,
    val artist: String,
    val cover: String?,
    val durationSeconds: Int,
)
