package xyz.lisfox.vocaland.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import xyz.lisfox.vocaland.data.collection.CollectionTarget
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.VotingKind

object Routes {
    const val HOME = "home"
    const val RANKING = "ranking"
    const val PLAYER = "player"
    const val SEARCH = "search"
    const val PROFILE = "profile"
    const val LOGIN = "login"
    const val FEATURE = "feature/{title}"
    const val SONG = "song/{songId}"
    const val ARTIST = "artist/{kind}/{artistId}"
    const val TAG = "tag/{tagId}"
    const val USER = "user/{userId}"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST = "playlist/{playlistId}"
    const val VOTINGS = "votings"
    const val VOTING = "voting/{kind}/{campaignId}"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot-password"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val SHOP = "shop"
    const val WALLET = "wallet"
    const val MY_PLAYLISTS = "my-playlists"
    const val TAG_INDEX = "tags"
    const val STAT = "stat"
    const val CUSTOM_RANKING = "custom-ranking"
    const val COLLECTION_LIST = "collections"
    const val COLLECTION_MANUAL = "collection/manual/{collectionId}"
    const val COLLECTION_DERIVED = "collection/{kind}/{sourceId}/{entityType}"
    const val COLLECTION_SHARE_MANUAL = "share/collection/manual/{collectionId}"
    const val COLLECTION_SHARE_DERIVED = "share/collection/{kind}/{sourceId}/{entityType}"

    fun feature(title: String): String = "feature/${Uri.encode(title)}"

    fun song(id: Int): String = "song/$id"

    fun artist(kind: ArtistKind, id: Int): String = "artist/${kind.apiType}/$id"

    fun tag(id: Int): String = "tag/$id"

    fun user(id: Int): String = "user/$id"

    fun playlist(id: Int): String = "playlist/$id"

    fun voting(kind: VotingKind, campaignId: Int): String =
        "voting/${kind.routeValue}/$campaignId"

    fun collection(target: CollectionTarget): String = when (target) {
        is CollectionTarget.Manual -> "collection/manual/${target.id}"
        is CollectionTarget.Derived ->
            "collection/${target.kind.key}/${target.sourceId}/${target.entityType.key}"
    }

    fun collectionShare(target: CollectionTarget): String = when (target) {
        is CollectionTarget.Manual -> "share/collection/manual/${target.id}"
        is CollectionTarget.Derived ->
            "share/collection/${target.kind.key}/${target.sourceId}/${target.entityType.key}"
    }
}

data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val TOP_LEVEL_DESTINATIONS = listOf(
    TopLevelDestination(Routes.HOME, "首页", Icons.Filled.Home),
    TopLevelDestination(Routes.RANKING, "榜单", Icons.Filled.Leaderboard),
    TopLevelDestination(Routes.PLAYER, "播放器", Icons.Filled.PlayArrow),
    TopLevelDestination(Routes.SEARCH, "搜索", Icons.Filled.Search),
    TopLevelDestination(Routes.PROFILE, "我的", Icons.Filled.Person),
)
