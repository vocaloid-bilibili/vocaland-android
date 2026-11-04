package xyz.lisfox.vocaland.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import xyz.lisfox.vocaland.data.model.ArtistKind

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

    fun feature(title: String): String = "feature/${Uri.encode(title)}"

    fun song(id: Int): String = "song/$id"

    fun artist(kind: ArtistKind, id: Int): String = "artist/${kind.apiType}/$id"

    fun tag(id: Int): String = "tag/$id"

    fun user(id: Int): String = "user/$id"

    fun playlist(id: Int): String = "playlist/$id"
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
