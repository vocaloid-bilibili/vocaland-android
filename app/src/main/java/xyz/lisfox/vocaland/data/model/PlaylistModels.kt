package xyz.lisfox.vocaland.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistCoverDto(
    val bvid: String = "",
    val thumbnail: String? = null,
)

@Serializable
data class PublicPlaylistDto(
    val id: Int = 0,
    val name: String = "",
    val description: String? = null,
    val songCount: Int = 0,
    val coverVideo: PlaylistCoverDto? = null,
    val clickCount: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class PublicPlaylistListResponse(
    val data: List<PublicPlaylistDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val perPage: Int = 24,
)

@Serializable
data class PlaylistOwnerDto(
    val id: Int = 0,
    val username: String = "",
    val nickname: String = "",
    val avatar: String? = null,
)

@Serializable
data class PlaylistInfoDto(
    val id: Int = 0,
    val name: String = "",
    val description: String? = null,
    val songCount: Int = 0,
    val coverVideo: PlaylistCoverDto? = null,
    val clickCount: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
    val owner: PlaylistOwnerDto? = null,
)

@Serializable
data class PlaylistSongDto(
    val id: Int = 0,
    val songId: Int = 0,
    val position: Int = 0,
    val note: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val song: SongDto? = null,
)

@Serializable
data class PlaylistDetailDto(
    val playlist: PlaylistInfoDto = PlaylistInfoDto(),
    val songs: List<PlaylistSongDto> = emptyList(),
)
