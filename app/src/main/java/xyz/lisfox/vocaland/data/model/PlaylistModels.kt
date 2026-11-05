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
    val visibility: String = "private",
    val isDefault: Boolean = false,
    val sortMode: String = "custom",
    val canEdit: Boolean = false,
    val containsSong: Boolean = false,
    val lastClickedAt: String? = null,
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

@Serializable
data class PlaylistListResponse(
    val data: List<PlaylistInfoDto> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
)

@Serializable
data class PlaylistAddSongsResult(
    val added: List<Int> = emptyList(),
    val skipped: List<Int> = emptyList(),
)

@Serializable
data class PlaylistNoteResult(
    val id: Int = 0,
    val note: String? = null,
)

@Serializable
data class PlaylistMoveResult(
    val id: Int = 0,
    val songId: Int = 0,
    val position: Int = 0,
    val renormalized: Boolean = false,
)

@Serializable
data class PlaylistCoverResult(
    val coverBvid: String? = null,
)

@Serializable
data class PlaylistPage(
    val items: List<PlaylistInfoDto> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
)

enum class PlaylistSortMode(
    val key: String,
    val label: String,
) {
    Custom("custom", "自定义顺序"),
    DateAdded("date_added", "添加时间"),
    PublishTime("publish_time", "发布时间"),
    ViewCount("view_count", "播放量"),
    ;

    companion object {
        fun fromKey(key: String?): PlaylistSortMode =
            entries.firstOrNull { it.key == key } ?: Custom
    }
}

enum class PlaylistVisibility(
    val key: String,
    val label: String,
) {
    Public("public", "公开"),
    Private("private", "私有"),
    ;

    companion object {
        fun fromKey(key: String?): PlaylistVisibility =
            entries.firstOrNull { it.key == key } ?: Private
    }
}
