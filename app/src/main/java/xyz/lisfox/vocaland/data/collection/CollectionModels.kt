package xyz.lisfox.vocaland.data.collection

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import xyz.lisfox.vocaland.data.model.ArtistBriefDto
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.SongDto

@Serializable
data class CollectionDto(
    val id: Int = 0,
    val name: String = "",
    val description: String? = null,
    val entityType: String = "song",
    val coverPicture: String? = null,
    val entityCount: Int = 0,
    val litCount: Int = 0,
    val progress: Double = 0.0,
)

@Serializable
data class DerivedCollectionDto(
    val kind: String = "",
    val sourceId: Int = 0,
    val name: String = "",
    val description: String? = null,
    val entityType: String = "song",
    val coverPicture: String? = null,
    val entityCount: Int = 0,
    val litCount: Int = 0,
    val progress: Double = 0.0,
)

@Serializable
data class CollectionEntryDto(
    val entityId: Int = 0,
    val litCount: Int = 0,
    val litByMe: Boolean = false,
    val entity: JsonElement? = null,
)

@Serializable
data class CollectionDetailDto(
    val collection: CollectionDto = CollectionDto(),
    val entries: List<CollectionEntryDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class DerivedCollectionDetailDto(
    val collection: DerivedCollectionDto = DerivedCollectionDto(),
    val entries: List<CollectionEntryDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class CollectionDetailEnvelope(
    val data: CollectionDetailDto = CollectionDetailDto(),
)

@Serializable
data class DerivedCollectionDetailEnvelope(
    val data: DerivedCollectionDetailDto = DerivedCollectionDetailDto(),
)

data class CollectionHeader(
    val name: String,
    val description: String?,
    val entityType: String,
    val coverPicture: String?,
    val entityCount: Int,
    val litCount: Int,
    val progress: Double,
    val manualId: Int? = null,
    val derivedKind: String? = null,
    val sourceId: Int? = null,
)

sealed interface CollectionEntityItem {
    data class Song(val song: SongDto) : CollectionEntityItem

    data class Artist(
        val artist: ArtistBriefDto,
        val kind: ArtistKind,
    ) : CollectionEntityItem
}

data class CollectionEntry(
    val entityId: Int,
    val litCount: Int,
    val litByMe: Boolean,
    val entity: CollectionEntityItem?,
)

data class CollectionDetailResult(
    val header: CollectionHeader,
    val entries: List<CollectionEntry>,
    val total: Int,
    val pageSize: Int,
)

sealed interface CollectionTarget {
    data class Manual(val id: Int) : CollectionTarget

    data class Derived(
        val kind: DerivedCollectionKind,
        val sourceId: Int,
        val entityType: LitEntityType = LitEntityType.Song,
    ) : CollectionTarget
}

enum class DerivedCollectionKind(val key: String) {
    Tag("tag"),
    Producer("producer"),
    Vocalist("vocalist"),
    Synthesizer("synthesizer"),
}

enum class LitEntityType(
    val key: String,
    val label: String,
) {
    Song("song", "歌曲"),
    Producer("producer", "P主"),
    Vocalist("vocalist", "歌手"),
    ;

    companion object {
        fun fromKey(key: String?): LitEntityType =
            entries.firstOrNull { it.key == key } ?: Song
    }
}
