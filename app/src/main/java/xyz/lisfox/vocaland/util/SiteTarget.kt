package xyz.lisfox.vocaland.util

import xyz.lisfox.vocaland.data.model.ArtistKind

sealed interface SiteTarget {
    data class Song(val id: Int) : SiteTarget

    data class Artist(val kind: ArtistKind, val id: Int) : SiteTarget

    data class Tag(val id: Int) : SiteTarget

    data class User(val id: Int) : SiteTarget

    data class Playlist(val id: Int) : SiteTarget

    data object Ranking : SiteTarget
}

fun SiteTarget.displayLabel(): String = when (this) {
    is SiteTarget.Song -> "歌曲 #$id"
    is SiteTarget.Artist -> "${kind.label} #$id"
    is SiteTarget.Tag -> "标签 #$id"
    is SiteTarget.User -> "用户 #$id"
    is SiteTarget.Playlist -> "歌单 #$id"
    SiteTarget.Ranking -> "排行榜"
}

fun parseTargetKey(targetKey: String): SiteTarget? {
    val separator = targetKey.indexOf(':')
    if (separator <= 0) return null

    val type = targetKey.substring(0, separator)
    val rest = targetKey.substring(separator + 1).trim()
    if (rest.isEmpty()) return null

    return when (type) {
        "song" -> rest.toId()?.let(SiteTarget::Song)
        "tag" -> rest.toId()?.let(SiteTarget::Tag)
        "user" -> rest.toId()?.let(SiteTarget::User)
        "artist" -> parseArtistTarget(rest)
        "interaction" -> {
            val subSeparator = rest.indexOf(':')
            if (subSeparator <= 0) {
                null
            } else {
                val subType = rest.substring(0, subSeparator).lowercase()
                val subId = rest.substring(subSeparator + 1).trim()
                if (subType == "tag") {
                    subId.toId()?.let(SiteTarget::Tag)
                } else {
                    null
                }
            }
        }

        else -> null
    }
}

fun parseSitePath(raw: String): SiteTarget? {
    val path = raw.substringBefore('#').substringBefore('?').trim()
    if (!path.startsWith('/')) return null

    val segments = path.trim('/').split('/').filter { it.isNotEmpty() }
    if (segments.isEmpty()) return null

    return when (segments.first()) {
        "song" -> segments.singleId()?.let(SiteTarget::Song)
        "producer" -> segments.singleId()?.let { SiteTarget.Artist(ArtistKind.Producer, it) }
        "vocalist" -> segments.singleId()?.let { SiteTarget.Artist(ArtistKind.Vocalist, it) }
        "synthesizer" -> segments.singleId()?.let { SiteTarget.Artist(ArtistKind.Synthesizer, it) }
        "uploader" -> segments.singleId()?.let { SiteTarget.Artist(ArtistKind.Uploader, it) }
        "user" -> segments.singleId()?.let(SiteTarget::User)
        "tag" -> segments.singleId()?.let(SiteTarget::Tag)
        "playlist" -> segments.singleId()?.let(SiteTarget::Playlist)
        "ranking" -> if (segments.size == 1) SiteTarget.Ranking else null
        else -> null
    }
}

private fun parseArtistTarget(rest: String): SiteTarget? {
    val boundary = rest.indexOf('-')
    if (boundary <= 0) return null

    val kind = ArtistKind.fromApiType(rest.substring(0, boundary)) ?: return null
    val id = rest.substring(boundary + 1).toId() ?: return null
    return SiteTarget.Artist(kind, id)
}

private fun String.toId(): Int? = toIntOrNull()?.takeIf { it > 0 }

private fun List<String>.singleId(): Int? =
    if (size == 2) this[1].toId() else null
