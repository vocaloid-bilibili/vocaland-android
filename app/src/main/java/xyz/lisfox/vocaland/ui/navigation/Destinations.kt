package xyz.lisfox.vocaland.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val RANKING = "ranking"
    const val SEARCH = "search"
    const val PROFILE = "profile"
    const val LOGIN = "login"
    const val FEATURE = "feature/{title}"

    fun feature(title: String): String = "feature/${Uri.encode(title)}"
}

data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val TOP_LEVEL_DESTINATIONS = listOf(
    TopLevelDestination(Routes.HOME, "首页", Icons.Filled.Home),
    TopLevelDestination(Routes.RANKING, "榜单", Icons.Filled.Leaderboard),
    TopLevelDestination(Routes.SEARCH, "搜索", Icons.Filled.Search),
    TopLevelDestination(Routes.PROFILE, "我的", Icons.Filled.Person),
)
