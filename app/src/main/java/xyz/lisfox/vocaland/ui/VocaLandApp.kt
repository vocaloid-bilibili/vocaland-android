package xyz.lisfox.vocaland.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.di.AppContainer
import xyz.lisfox.vocaland.ui.artist.ArtistDetailScreen
import xyz.lisfox.vocaland.ui.components.BrandTitle
import xyz.lisfox.vocaland.ui.home.HomeScreen
import xyz.lisfox.vocaland.ui.login.LoginScreen
import xyz.lisfox.vocaland.ui.navigation.Routes
import xyz.lisfox.vocaland.ui.navigation.TOP_LEVEL_DESTINATIONS
import xyz.lisfox.vocaland.ui.placeholder.FeatureScreen
import xyz.lisfox.vocaland.ui.profile.ProfileScreen
import xyz.lisfox.vocaland.ui.ranking.RankingScreen
import xyz.lisfox.vocaland.ui.search.SearchScreen
import xyz.lisfox.vocaland.ui.song.SongDetailScreen
import xyz.lisfox.vocaland.ui.tag.TagDetailScreen
import xyz.lisfox.vocaland.ui.theme.VocaLandTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocaLandApp(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val themeSettings by container.themePreferences.settings.collectAsStateWithLifecycle()

    VocaLandTheme(
        palette = themeSettings.palette,
        themeMode = themeSettings.mode,
    ) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val isLogin = currentRoute == Routes.LOGIN

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.systemBars.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
            ),
            bottomBar = {
                AnimatedVisibility(
                    visible = !isLogin,
                    enter = fadeIn(animationSpec = tween(180)) +
                        expandVertically(
                            animationSpec = tween(240),
                            expandFrom = Alignment.Bottom,
                        ),
                    exit = fadeOut(animationSpec = tween(140)) +
                        shrinkVertically(
                            animationSpec = tween(220),
                            shrinkTowards = Alignment.Bottom,
                        ),
                ) {
                    NavigationBar {
                        TOP_LEVEL_DESTINATIONS.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label,
                                    )
                                },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    val direction = tabDirection()
                    if (direction != null) {
                        fadeIn(animationSpec = tween(220)) +
                            slideInHorizontally(animationSpec = tween(320)) {
                                it * direction
                            }
                    } else {
                        fadeIn(animationSpec = tween(200)) +
                            slideInHorizontally(animationSpec = tween(320)) { it }
                    }
                },
                exitTransition = {
                    val direction = tabDirection()
                    if (direction != null) {
                        fadeOut(animationSpec = tween(220)) +
                            slideOutHorizontally(animationSpec = tween(320)) {
                                -it * direction
                            }
                    } else {
                        fadeOut(animationSpec = tween(220)) +
                            slideOutHorizontally(animationSpec = tween(320)) { -it / 5 }
                    }
                },
                popEnterTransition = {
                    val direction = tabDirection()
                    if (direction != null) {
                        fadeIn(animationSpec = tween(220)) +
                            slideInHorizontally(animationSpec = tween(320)) {
                                it * direction
                            }
                    } else {
                        fadeIn(animationSpec = tween(200)) +
                            slideInHorizontally(animationSpec = tween(320)) { -it / 5 }
                    }
                },
                popExitTransition = {
                    val direction = tabDirection()
                    if (direction != null) {
                        fadeOut(animationSpec = tween(220)) +
                            slideOutHorizontally(animationSpec = tween(320)) {
                                -it * direction
                            }
                    } else {
                        fadeOut(animationSpec = tween(200)) +
                            slideOutHorizontally(animationSpec = tween(320)) { it }
                    }
                },
            ) {
                composable(Routes.HOME) {
                    PageScaffold(title = null) { contentModifier ->
                        HomeScreen(
                            repository = container.homeRepository,
                            onOpenFeature = { title ->
                                navController.navigate(Routes.feature(title))
                            },
                            onOpenSong = { id ->
                                navController.navigate(Routes.song(id)) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.RANKING) {
                    PageScaffold(title = "排行榜") { contentModifier ->
                        RankingScreen(
                            repository = container.rankingRepository,
                            onOpenSong = { id ->
                                navController.navigate(Routes.song(id)) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.SEARCH) {
                    PageScaffold(title = "搜索") { contentModifier ->
                        SearchScreen(
                            repository = container.searchRepository,
                            onOpenSong = { id ->
                                navController.navigate(Routes.song(id)) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenArtist = { kind, id ->
                                navController.navigate(Routes.artist(kind, id)) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenTag = { id ->
                                navController.navigate(Routes.tag(id)) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.PROFILE) {
                    PageScaffold(title = "我的") { contentModifier ->
                        ProfileScreen(
                            session = container.session,
                            authRepository = container.authRepository,
                            themePreferences = container.themePreferences,
                            onLogin = { navController.navigate(Routes.LOGIN) },
                            onOpenFeature = { title ->
                                navController.navigate(Routes.feature(title))
                            },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(
                    route = Routes.LOGIN,
                    enterTransition = {
                        fadeIn(animationSpec = tween(200)) +
                            slideInHorizontally(animationSpec = tween(320)) { it }
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(200)) +
                            slideOutHorizontally(animationSpec = tween(320)) { -it / 5 }
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(200)) +
                            slideInHorizontally(animationSpec = tween(320)) { -it / 5 }
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(200)) +
                            slideOutHorizontally(animationSpec = tween(320)) { it }
                    },
                ) {
                    PageScaffold(
                        title = "登录",
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        LoginScreen(
                            repository = container.authRepository,
                            onBack = { navController.popBackStack() },
                            onLoginSuccess = {
                                if (!navController.popBackStack()) {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.HOME)
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onOpenRegister = {
                                navController.navigate(Routes.feature("注册账号"))
                            },
                            onOpenForgotPassword = {
                                navController.navigate(Routes.feature("找回密码"))
                            },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(
                    route = Routes.SONG,
                    arguments = listOf(
                        navArgument("songId") { type = NavType.IntType },
                    ),
                ) { entry ->
                    val songId = entry.arguments?.getInt("songId") ?: 0
                    PageScaffold(
                        title = "歌曲",
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        if (songId <= 0) {
                            FeatureScreen(
                                title = "无效的歌曲",
                                modifier = contentModifier,
                            )
                        } else {
                            SongDetailScreen(
                                repository = container.songDetailRepository,
                                session = container.session,
                                songId = songId,
                                onOpenArtist = { kind, id ->
                                    navController.navigate(Routes.artist(kind, id)) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenTag = { id ->
                                    navController.navigate(Routes.tag(id)) {
                                        launchSingleTop = true
                                    }
                                },
                                onLogin = { navController.navigate(Routes.LOGIN) },
                                modifier = contentModifier,
                            )
                        }
                    }
                }
                composable(
                    route = Routes.ARTIST,
                    arguments = listOf(
                        navArgument("kind") { type = NavType.StringType },
                        navArgument("artistId") { type = NavType.IntType },
                    ),
                ) { entry ->
                    val kindParam = entry.arguments?.getString("kind").orEmpty()
                    val artistId = entry.arguments?.getInt("artistId") ?: 0
                    val kind = ArtistKind.fromApiType(kindParam)
                    PageScaffold(
                        title = kind?.label ?: "艺人",
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        if (kind == null || artistId <= 0) {
                            FeatureScreen(
                                title = "无效的艺人",
                                modifier = contentModifier,
                            )
                        } else {
                            ArtistDetailScreen(
                                repository = container.artistDetailRepository,
                                kind = kind,
                                artistId = artistId,
                                onOpenSong = { id ->
                                    navController.navigate(Routes.song(id)) {
                                        launchSingleTop = true
                                    }
                                },
                                modifier = contentModifier,
                            )
                        }
                    }
                }
                composable(
                    route = Routes.TAG,
                    arguments = listOf(
                        navArgument("tagId") { type = NavType.IntType },
                    ),
                ) { entry ->
                    val tagId = entry.arguments?.getInt("tagId") ?: 0
                    PageScaffold(
                        title = "标签",
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        if (tagId <= 0) {
                            FeatureScreen(
                                title = "无效的标签",
                                modifier = contentModifier,
                            )
                        } else {
                            TagDetailScreen(
                                repository = container.tagDetailRepository,
                                tagId = tagId,
                                onOpenSong = { id ->
                                    navController.navigate(Routes.song(id)) {
                                        launchSingleTop = true
                                    }
                                },
                                modifier = contentModifier,
                            )
                        }
                    }
                }
                composable(
                    route = Routes.FEATURE,
                    arguments = listOf(
                        navArgument("title") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val title = entry.arguments?.getString("title") ?: "详情"
                    PageScaffold(
                        title = title,
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        FeatureScreen(
                            title = title,
                            modifier = contentModifier,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageScaffold(
    title: String?,
    onBack: (() -> Unit)? = null,
    content: @Composable (Modifier) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            windowInsets = WindowInsets(0, 0, 0, 0),
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                }
            },
            title = {
                if (title == null) {
                    BrandTitle()
                } else {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            content(Modifier.fillMaxSize())
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): Int? {
    val from = TOP_LEVEL_DESTINATIONS.indexOfFirst {
        it.route == initialState.destination.route
    }
    val to = TOP_LEVEL_DESTINATIONS.indexOfFirst {
        it.route == targetState.destination.route
    }
    if (from == -1 || to == -1 || from == to) return null
    return if (to > from) 1 else -1
}
