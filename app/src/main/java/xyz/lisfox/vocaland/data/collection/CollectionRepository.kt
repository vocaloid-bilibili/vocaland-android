package xyz.lisfox.vocaland.data.collection

import kotlinx.serialization.json.JsonElement
import xyz.lisfox.vocaland.data.model.ArtistBriefDto
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.remote.AppJson

class CollectionRepository(
    private val api: CollectionApi,
) {
    suspend fun loadCollections(): Result<List<CollectionDto>> = runCatching {
        api.collections().data
    }

    suspend fun loadDetail(
        target: CollectionTarget,
        page: Int = 1,
        pageSize: Int = PAGE_SIZE,
        orderBy: String = "lit_count",
        order: String = "desc",
    ): Result<CollectionDetailResult> = runCatching {
        when (target) {
            is CollectionTarget.Manual -> {
                val data = api.collectionDetail(
                    id = target.id,
                    page = page,
                    pageSize = pageSize,
                    orderBy = orderBy,
                    order = order,
                ).data
                CollectionDetailResult(
                    header = data.collection.toHeader(),
                    entries = parseEntries(data.entries, data.collection.entityType),
                    total = data.total,
                    pageSize = pageSize,
                )
            }

            is CollectionTarget.Derived -> {
                val data = api.derivedCollectionDetail(
                    kind = target.kind.key,
                    sourceId = target.sourceId,
                    entityType = target.entityType.key,
                    page = page,
                    pageSize = pageSize,
                    orderBy = orderBy,
                    order = order,
                ).data
                CollectionDetailResult(
                    header = data.collection.toHeader(),
                    entries = parseEntries(data.entries, data.collection.entityType),
                    total = data.total,
                    pageSize = pageSize,
                )
            }
        }
    }

    private fun CollectionDto.toHeader(): CollectionHeader = CollectionHeader(
        name = name,
        description = description,
        entityType = entityType,
        coverPicture = coverPicture,
        entityCount = entityCount,
        litCount = litCount,
        progress = progress,
        manualId = id.takeIf { it > 0 },
    )

    private fun DerivedCollectionDto.toHeader(): CollectionHeader = CollectionHeader(
        name = name,
        description = description,
        entityType = entityType,
        coverPicture = coverPicture,
        entityCount = entityCount,
        litCount = litCount,
        progress = progress,
        derivedKind = kind,
        sourceId = sourceId.takeIf { it > 0 },
    )

    private fun parseEntries(
        entries: List<CollectionEntryDto>,
        entityType: String,
    ): List<CollectionEntry> = entries.map { entry ->
        CollectionEntry(
            entityId = entry.entityId,
            litCount = entry.litCount,
            litByMe = entry.litByMe,
            entity = parseEntity(entry.entity, entityType),
        )
    }

    private fun parseEntity(
        element: JsonElement?,
        entityType: String,
    ): CollectionEntityItem? {
        if (element == null) return null
        return when (entityType) {
            "song" -> runCatching {
                AppJson.decodeFromJsonElement(SongDto.serializer(), element)
            }.getOrNull()?.let(CollectionEntityItem::Song)

            "producer" -> parseArtist(element, ArtistKind.Producer)
            "vocalist" -> parseArtist(element, ArtistKind.Vocalist)
            else -> null
        }
    }

    private fun parseArtist(
        element: JsonElement,
        kind: ArtistKind,
    ): CollectionEntityItem? = runCatching {
        AppJson.decodeFromJsonElement(ArtistBriefDto.serializer(), element)
    }.getOrNull()?.let { artist ->
        CollectionEntityItem.Artist(artist = artist, kind = kind)
    }

    private companion object {
        const val PAGE_SIZE = 48
    }
}
