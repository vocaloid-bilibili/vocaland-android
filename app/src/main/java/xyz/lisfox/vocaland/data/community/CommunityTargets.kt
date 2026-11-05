package xyz.lisfox.vocaland.data.community

import xyz.lisfox.vocaland.data.model.ArtistKind

object CommunityTargets {
    fun song(id: Int): String = "song:$id"

    fun artist(kind: ArtistKind, id: Int): String = "artist:${kind.apiType}-$id"

    fun tag(id: Int): String = "interaction:tag:$id"

    fun user(id: Int): String = "user:$id"

    fun playlist(id: Int): String = "playlist:$id"

    fun page(key: String): String = "page:$key"

    fun ranking(board: String, issue: Int? = null): String =
        if (issue == null) "ranking:$board" else "ranking:$board:$issue"
}
