package xyz.lisfox.vocaland.data.artist

import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.TagItemDto

data class RelatedArtist(
    val kind: ArtistKind,
    val id: Int,
    val name: String,
    val picture: String?,
    val count: Int,
)

data class ArtistExtras(
    val topSongs: List<SongDto> = emptyList(),
    val latestSongs: List<SongDto> = emptyList(),
    val related: List<RelatedArtist> = emptyList(),
    val tags: List<TagItemDto> = emptyList(),
)
