@file:OptIn(ExperimentalSharedTransitionApi::class)

package xyz.lisfox.vocaland.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import xyz.lisfox.vocaland.R
import xyz.lisfox.vocaland.ui.components.LocalSharedElementVisibilityScope
import xyz.lisfox.vocaland.ui.components.LocalSharedTransitionScope
import xyz.lisfox.vocaland.ui.components.MotionDurations
import xyz.lisfox.vocaland.ui.theme.ComponentSize
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.io.File
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.collection.CollectionTarget
import xyz.lisfox.vocaland.data.collection.DerivedCollectionKind
import xyz.lisfox.vocaland.data.collection.LitEntityType
import xyz.lisfox.vocaland.data.community.CommunityTargets
import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.VotingKind
import xyz.lisfox.vocaland.di.AppContainer
import xyz.lisfox.vocaland.ui.account.WalletScreen
import xyz.lisfox.vocaland.ui.artist.ArtistDetailScreen
import xyz.lisfox.vocaland.ui.collection.CollectionDetailScreen
import xyz.lisfox.vocaland.ui.collection.CollectionListScreen
import xyz.lisfox.vocaland.ui.community.CommunityPanel
import xyz.lisfox.vocaland.ui.components.BrandTitle
import xyz.lisfox.vocaland.ui.customranking.CustomRankingScreen
import xyz.lisfox.vocaland.ui.home.HomeScreen
import xyz.lisfox.vocaland.ui.login.ForgotPasswordScreen
import xyz.lisfox.vocaland.ui.login.LoginScreen
import xyz.lisfox.vocaland.ui.login.RegisterScreen
import xyz.lisfox.vocaland.ui.navigation.Routes
import xyz.lisfox.vocaland.ui.navigation.TOP_LEVEL_DESTINATIONS
import xyz.lisfox.vocaland.ui.notifications.NotificationsScreen
import xyz.lisfox.vocaland.ui.placeholder.FeatureScreen
import xyz.lisfox.vocaland.ui.player.PlayerScreen
import xyz.lisfox.vocaland.ui.playlist.AddToPlaylistSheet
import xyz.lisfox.vocaland.ui.playlist.MyPlaylistsScreen
import xyz.lisfox.vocaland.ui.playlist.PlaylistDetailScreen
import xyz.lisfox.vocaland.ui.playlist.PlaylistScreen
import xyz.lisfox.vocaland.ui.profile.ProfileScreen
import xyz.lisfox.vocaland.ui.ranking.RankingScreen
import xyz.lisfox.vocaland.ui.search.SearchScreen
import xyz.lisfox.vocaland.ui.settings.SettingsScreen
import xyz.lisfox.vocaland.ui.share.CollectionShareScreen
import xyz.lisfox.vocaland.ui.shop.ShopScreen
import xyz.lisfox.vocaland.ui.song.SongDetailScreen
import xyz.lisfox.vocaland.ui.stat.StatScreen
import xyz.lisfox.vocaland.ui.tag.TagDetailScreen
import xyz.lisfox.vocaland.ui.tag.TagIndexScreen
import xyz.lisfox.vocaland.ui.theme.VocaLandTheme
import xyz.lisfox.vocaland.ui.update.UpdateDialog
import xyz.lisfox.vocaland.ui.update.UpdateDownloadDialog
import xyz.lisfox.vocaland.ui.update.UpdateDownloadStatus
import xyz.lisfox.vocaland.ui.update.UpdateViewModel
import xyz.lisfox.vocaland.ui.update.canInstallPackages
import xyz.lisfox.vocaland.ui.update.installApk
import xyz.lisfox.vocaland.ui.update.installPermissionIntent
import xyz.lisfox.vocaland.ui.user.UserScreen
import xyz.lisfox.vocaland.ui.voting.EdVotingScreen
import xyz.lisfox.vocaland.ui.voting.TrendingVotingScreen
import xyz.lisfox.vocaland.ui.voting.VotingHubScreen
import xyz.lisfox.vocaland.util.SiteTarget
import xyz.lisfox.vocaland.util.parseTargetKey

private val bottomBarRowHeight = ComponentSize.bottomBarHeight

@Composable
fun VocaLandApp(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val themeSettings by container.themePreferences.settings.collectAsStateWithLifecycle()
    val sessionState by container.session.state.collectAsStateWithLifecycle()
    val updateViewModel: UpdateViewModel = viewModel(
        factory = viewModelFactory {
            initializer { UpdateViewModel(container.updateRepository, container.updateDownloader) }
        },
    )
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingInstallFile by remember { mutableStateOf<File?>(null) }
    var showInstallPermission by remember { mutableStateOf(false) }
    var promptedInstallFile by rememberSaveable { mutableStateOf<String?>(null) }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val file = pendingInstallFile
        showInstallPermission = false
        if (file != null) {
            when {
                !canInstallPackages(context) -> {
                    Toast.makeText(context, context.getString(R.string.toast_install_permission_missing), Toast.LENGTH_SHORT).show()
                }
                installApk(context, file) -> {
                    pendingInstallFile = null
                }
                else -> {
                    Toast.makeText(context, context.getString(R.string.toast_install_launch_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val requestInstall: (File) -> Unit = { file ->
        if (canInstallPackages(context)) {
            if (!installApk(context, file)) {
                Toast.makeText(context, context.getString(R.string.toast_install_launch_failed), Toast.LENGTH_SHORT).show()
            }
        } else {
            pendingInstallFile = file
            showInstallPermission = true
        }
    }

    LaunchedEffect(updateState.message) {
        val message = updateState.message ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        updateViewModel.consumeMessage()
    }

    LaunchedEffect(updateState.download?.status, updateState.download?.apkFile) {
        val download = updateState.download ?: return@LaunchedEffect
        if (download.status != UpdateDownloadStatus.Completed) return@LaunchedEffect
        val file = download.apkFile ?: return@LaunchedEffect
        if (!canInstallPackages(context) && promptedInstallFile != file.absolutePath) {
            promptedInstallFile = file.absolutePath
            pendingInstallFile = file
            showInstallPermission = true
        }
    }

    VocaLandTheme(
        palette = themeSettings.palette,
        themeMode = themeSettings.mode,
    ) {
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val loggedIn = sessionState.user != null
        var addToPlaylistSongId by remember { mutableStateOf<Int?>(null) }
        val isTopLevel = currentRoute == null ||
            TOP_LEVEL_DESTINATIONS.any { it.route == currentRoute }
        val navigateToTopLevel: (String) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
        val openTarget: (SiteTarget) -> Unit = { target ->
            when (target) {
                is SiteTarget.Song -> navController.navigate(Routes.song(target.id)) {
                    launchSingleTop = true
                }

                is SiteTarget.Artist -> navController.navigate(
                    Routes.artist(target.kind, target.id),
                ) {
                    launchSingleTop = true
                }

                is SiteTarget.Tag -> navController.navigate(Routes.tag(target.id)) {
                    launchSingleTop = true
                }

                is SiteTarget.User -> navController.navigate(Routes.user(target.id)) {
                    launchSingleTop = true
                }

                is SiteTarget.Playlist -> navController.navigate(Routes.playlist(target.id)) {
                    launchSingleTop = true
                }

                is SiteTarget.Voting -> navController.navigate(
                    Routes.voting(target.kind, target.campaignId),
                ) {
                    launchSingleTop = true
                }

                SiteTarget.Ranking -> navigateToTopLevel(Routes.RANKING)
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            AnimatedVisibility(
                visible = isTopLevel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                enter = fadeIn(animationSpec = tween(MotionDurations.EMPHASIZED)) +
                    slideInHorizontally(animationSpec = tween(MotionDurations.EMPHASIZED)) { -it / 5 },
                exit = fadeOut(animationSpec = tween(MotionDurations.EMPHASIZED)) +
                    slideOutHorizontally(animationSpec = tween(MotionDurations.EMPHASIZED)) { -it / 5 },
            ) {
                CompactBottomBar(
                    currentRoute = currentRoute,
                    onSelect = navigateToTopLevel,
                    enabled = isTopLevel,
                )
            }
            SharedTransitionLayout { CompositionLocalProvider(LocalSharedTransitionScope provides this@SharedTransitionLayout) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.systemBars.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                        ),
                    ),
                enterTransition = {
                    val direction = tabDirection()
                    if (direction != null) {
                        fadeIn(animationSpec = tween(MotionDurations.EMPHASIZED)) +
                            scaleIn(
                                initialScale = 0.98f,
                                animationSpec = tween(MotionDurations.EMPHASIZED),
                            ) +
                            slideInHorizontally(animationSpec = tween(MotionDurations.EMPHASIZED)) {
                                it * direction
                            }
                    } else {
                        fadeIn(animationSpec = tween(MotionDurations.EMPHASIZED)) +
                            scaleIn(
                                initialScale = 0.98f,
                                animationSpec = tween(MotionDurations.EMPHASIZED),
                            ) +
                            slideInHorizontally(animationSpec = tween(MotionDurations.EMPHASIZED)) { it }
                    }
                },
                exitTransition = {
                    val direction = tabDirection()
                    if (direction != null) {
                        fadeOut(animationSpec = tween(MotionDurations.EMPHASIZED)) +
                            slideOutHorizontally(animationSpec = tween(MotionDurations.EMPHASIZED)) {
                                -it * direction
                            }
                    } else {
                        fadeOut(animationSpec = tween(MotionDurations.EMPHASIZED)) +
                            slideOutHorizontally(animationSpec = tween(MotionDurations.EMPHASIZED)) { -it / 5 }
                    }
                },
                popEnterTransition = {
                    val direction = tabDirection()
                    if (direction != null) {
                        fadeIn(animationSpec = tween(MotionDurations.EMPHASIZED)) +
                            scaleIn(
                                initialScale = 0.98f,
                                animationSpec = tween(MotionDurations.EMPHASIZED),
                            ) +
                            slideInHorizontally(animationSpec = tween(MotionDurations.EMPHASIZED)) {
                                it * direction
                            }
                    } else {
                        fadeIn(animationSpec = tween(MotionDurations.EMPHASIZED)) +
                            slideInHorizontally(animationSpec = tween(MotionDurations.EMPHASIZED)) { -it / 5 }
                    }
                },
                popExitTransition = {
                    val direction = tabDirection()
                    if (direction != null) {
                        fadeOut(animationSpec = tween(MotionDurations.EMPHASIZED)) +
                            slideOutHorizontally(animationSpec = tween(MotionDurations.EMPHASIZED)) {
                                -it * direction
                            }
                    } else {
                        fadeOut(animationSpec = tween(MotionDurations.EMPHASIZED)) +
                            slideOutHorizontally(animationSpec = tween(MotionDurations.EMPHASIZED)) { it }
                    }
                },
            ) {
                composable(Routes.HOME) {
                    PageScaffold(title = null, sharedElementScope = this) { contentModifier ->
                        HomeScreen(
                            repository = container.homeRepository,
                            onOpenFeature = { title ->
                                navController.navigate(Routes.feature(title))
                            },
                            onOpenTarget = openTarget,
                            onOpenSong = { id ->
                                navController.navigate(Routes.song(id)) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenUser = { id ->
                                navController.navigate(Routes.user(id)) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenPlaylist = { id ->
                                navController.navigate(Routes.playlist(id)) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenPlaylists = {
                                navController.navigate(Routes.PLAYLISTS) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenVotings = {
                                navController.navigate(Routes.VOTINGS) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenVoting = { kind, campaignId ->
                                navController.navigate(Routes.voting(kind, campaignId)) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.RANKING) {
                    PageScaffold(title = stringResource(R.string.page_ranking), sharedElementScope = this) { contentModifier ->
                        RankingScreen(
                            repository = container.rankingRepository,
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
                            session = container.session,
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.PLAYER) {
                    PageScaffold(title = stringResource(R.string.page_player)) { contentModifier ->
                        PlayerScreen(
                            controller = container.bgmPlayerController,
                            danmakuRepository = container.danmakuRepository,
                            videoPreferences = container.playerVideoPreferences,
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.SEARCH) {
                    PageScaffold(title = stringResource(R.string.page_search), sharedElementScope = this) { contentModifier ->
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
                    PageScaffold(title = stringResource(R.string.page_profile)) { contentModifier ->
                        ProfileScreen(
                            session = container.session,
                            authRepository = container.authRepository,
                            themePreferences = container.themePreferences,
                            onLogin = { navController.navigate(Routes.LOGIN) },
                            onOpenFeature = { title ->
                                when (title) {
                                    "我的收藏" -> navController.navigate(Routes.COLLECTION_LIST)
                                    "我的歌单" -> navController.navigate(Routes.MY_PLAYLISTS)
                                    "标签图鉴" -> navController.navigate(Routes.TAG_INDEX)
                                    "数据统计" -> navController.navigate(Routes.STAT)
                                    "自定义排行榜" -> navController.navigate(Routes.CUSTOM_RANKING)
                                    "个人资料" -> {
                                        val userId = sessionState.user?.id
                                        if (userId != null) {
                                            navController.navigate(Routes.user(userId)) {
                                                launchSingleTop = true
                                            }
                                        } else {
                                            navController.navigate(Routes.LOGIN)
                                        }
                                    }
                                    else -> navController.navigate(Routes.feature(title))
                                }
                            },
                            onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onOpenWallet = { navController.navigate(Routes.WALLET) },
                            onOpenShop = { navController.navigate(Routes.SHOP) },
                            onOpenCheckIn = { navController.navigate(Routes.WALLET) },
                            onCheckUpdate = {
                                updateViewModel.checkForUpdates(silent = false)
                            },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(
                    route = Routes.LOGIN,
                ) {
                    PageScaffold(
                        title = stringResource(R.string.page_login),
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
                                navController.navigate(Routes.REGISTER) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenForgotPassword = {
                                navController.navigate(Routes.FORGOT_PASSWORD) {
                                    launchSingleTop = true
                                }
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
                        title = stringResource(R.string.page_song),
                        onBack = { navController.popBackStack() },
                        sharedElementScope = this,
                    ) { contentModifier ->
                        if (songId <= 0) {
                            FeatureScreen(
                                title = stringResource(R.string.page_invalid_song),
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
                                onOpenUser = { id ->
                                    navController.navigate(Routes.user(id)) {
                                        launchSingleTop = true
                                    }
                                },
                                onLogin = { navController.navigate(Routes.LOGIN) },
                                playerController = container.bgmPlayerController,
                                songActionsRepository = container.songActionsRepository,
                                onAddToPlaylist = { id -> addToPlaylistSongId = id },
                                communitySlot = {
                                    CommunityPanel(
                                        repository = container.communityRepository,
                                        targetKey = CommunityTargets.song(songId),
                                        session = container.session,
                                        onLogin = { navController.navigate(Routes.LOGIN) },
                                        onOpenUser = { id ->
                                            navController.navigate(Routes.user(id)) {
                                                launchSingleTop = true
                                            }
                                        },
                                    )
                                },
                                modifier = contentModifier,
                            )
                        }
                    }
                }
                composable(
                    route = Routes.USER,
                    arguments = listOf(
                        navArgument("userId") { type = NavType.IntType },
                    ),
                ) { entry ->
                    val userId = entry.arguments?.getInt("userId") ?: 0
                    PageScaffold(
                        title = stringResource(R.string.page_user),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        if (userId <= 0) {
                            FeatureScreen(
                                title = stringResource(R.string.page_invalid_user),
                                modifier = contentModifier,
                            )
                        } else {
                            UserScreen(
                                repository = container.userRepository,
                                userId = userId,
                                onOpenSong = { id ->
                                    navController.navigate(Routes.song(id)) {
                                        launchSingleTop = true
                                    }
                                },
                                session = container.session,
                                communityRepository = container.communityRepository,
                                onLogin = { navController.navigate(Routes.LOGIN) },
                                onOpenUser = { id ->
                                    navController.navigate(Routes.user(id)) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                                onOpenWallet = { navController.navigate(Routes.WALLET) },
                                modifier = contentModifier,
                            )
                        }
                    }
                }
                composable(Routes.PLAYLISTS) {
                    PageScaffold(
                        title = stringResource(R.string.page_playlists),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        PlaylistScreen(
                            repository = container.playlistRepository,
                            onOpenPlaylist = { id ->
                                navController.navigate(Routes.playlist(id)) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenMyPlaylists = {
                                navController.navigate(Routes.MY_PLAYLISTS) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(
                    route = Routes.PLAYLIST,
                    arguments = listOf(
                        navArgument("playlistId") { type = NavType.IntType },
                    ),
                ) { entry ->
                    val playlistId = entry.arguments?.getInt("playlistId") ?: 0
                    PageScaffold(
                        title = stringResource(R.string.page_playlist),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        if (playlistId <= 0) {
                            FeatureScreen(
                                title = stringResource(R.string.page_invalid_playlist),
                                modifier = contentModifier,
                            )
                        } else {
                            PlaylistDetailScreen(
                                repository = container.playlistRepository,
                                playlistId = playlistId,
                                onOpenSong = { id ->
                                    navController.navigate(Routes.song(id)) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenUser = { id ->
                                    navController.navigate(Routes.user(id)) {
                                        launchSingleTop = true
                                    }
                                },
                                onPlayPlaylist = { tracks ->
                                    container.bgmPlayerController.replaceQueue(tracks)
                                    navigateToTopLevel(Routes.PLAYER)
                                },
                                searchRepository = container.searchRepository,
                                session = container.session,
                                onLogin = { navController.navigate(Routes.LOGIN) },
                                onPlaylistDeleted = { navController.popBackStack() },
                                communitySlot = {
                                    CommunityPanel(
                                        repository = container.communityRepository,
                                        targetKey = CommunityTargets.playlist(playlistId),
                                        session = container.session,
                                        onLogin = { navController.navigate(Routes.LOGIN) },
                                        onOpenUser = { id ->
                                            navController.navigate(Routes.user(id)) {
                                                launchSingleTop = true
                                            }
                                        },
                                    )
                                },
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
                        title = kind?.label ?: stringResource(R.string.page_artist),
                        onBack = { navController.popBackStack() },
                        sharedElementScope = this,
                    ) { contentModifier ->
                        if (kind == null || artistId <= 0) {
                            FeatureScreen(
                                title = stringResource(R.string.page_invalid_artist),
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
                                onOpenArtist = { artistKind, id ->
                                    navController.navigate(Routes.artist(artistKind, id)) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenTag = { id ->
                                    navController.navigate(Routes.tag(id)) {
                                        launchSingleTop = true
                                    }
                                },
                                rating = {
                                    CommunityPanel(
                                        repository = container.communityRepository,
                                        targetKey = CommunityTargets.artist(kind, artistId),
                                        session = container.session,
                                        onLogin = { navController.navigate(Routes.LOGIN) },
                                        onOpenUser = { id ->
                                            navController.navigate(Routes.user(id)) {
                                                launchSingleTop = true
                                            }
                                        },
                                        showComments = false,
                                        title = stringResource(R.string.page_ratings),
                                    )
                                },
                                comments = {
                                    CommunityPanel(
                                        repository = container.communityRepository,
                                        targetKey = CommunityTargets.artist(kind, artistId),
                                        session = container.session,
                                        onLogin = { navController.navigate(Routes.LOGIN) },
                                        onOpenUser = { id ->
                                            navController.navigate(Routes.user(id)) {
                                                launchSingleTop = true
                                            }
                                        },
                                        showRatings = false,
                                        title = stringResource(R.string.page_comments),
                                    )
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
                        title = stringResource(R.string.page_tag),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        if (tagId <= 0) {
                            FeatureScreen(
                                title = stringResource(R.string.page_invalid_tag),
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
                                onOpenArtist = { kind, id ->
                                    navController.navigate(Routes.artist(kind, id)) {
                                        launchSingleTop = true
                                    }
                                },
                                communitySlot = {
                                    CommunityPanel(
                                        repository = container.communityRepository,
                                        targetKey = CommunityTargets.tag(tagId),
                                        session = container.session,
                                        onLogin = { navController.navigate(Routes.LOGIN) },
                                        onOpenUser = { id ->
                                            navController.navigate(Routes.user(id)) {
                                                launchSingleTop = true
                                            }
                                        },
                                    )
                                },
                                modifier = contentModifier,
                            )
                        }
                    }
                }
                composable(Routes.VOTINGS) {
                    PageScaffold(
                        title = stringResource(R.string.page_votings),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        VotingHubScreen(
                            repository = container.votingRepository,
                            onOpenVoting = { kind, campaignId ->
                                navController.navigate(Routes.voting(kind, campaignId)) {
                                    launchSingleTop = true
                                }
                            },
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
                            modifier = contentModifier,
                        )
                    }
                }
                composable(
                    route = Routes.VOTING,
                    arguments = listOf(
                        navArgument("kind") { type = NavType.StringType },
                        navArgument("campaignId") { type = NavType.IntType },
                    ),
                ) { entry ->
                    val kind = VotingKind.fromRoute(entry.arguments?.getString("kind"))
                    val campaignId = entry.arguments?.getInt("campaignId") ?: 0
                    PageScaffold(
                        title = kind?.label ?: stringResource(R.string.page_votings),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        if (kind == null || campaignId <= 0) {
                            FeatureScreen(
                                title = stringResource(R.string.page_invalid_voting),
                                modifier = contentModifier,
                            )
                        } else {
                            when (kind) {
                                VotingKind.Ed -> EdVotingScreen(
                                    repository = container.votingRepository,
                                    session = container.session,
                                    campaignId = campaignId,
                                    onLogin = { navController.navigate(Routes.LOGIN) },
                                    onOpenSong = { id ->
                                        navController.navigate(Routes.song(id)) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onOpenShop = { navController.navigate(Routes.SHOP) },
                                    modifier = contentModifier,
                                )

                                VotingKind.Trending -> TrendingVotingScreen(
                                    repository = container.votingRepository,
                                    session = container.session,
                                    campaignId = campaignId,
                                    onLogin = { navController.navigate(Routes.LOGIN) },
                                    onOpenSong = { id ->
                                        navController.navigate(Routes.song(id)) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onOpenArtist = { artistKind, id ->
                                        navController.navigate(Routes.artist(artistKind, id)) {
                                            launchSingleTop = true
                                        }
                                    },
                                    modifier = contentModifier,
                                )
                            }
                        }
                    }
                }
                composable(Routes.REGISTER) {
                    PageScaffold(
                        title = stringResource(R.string.page_register),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        RegisterScreen(
                            repository = container.accountAuthRepository,
                            onBack = { navController.popBackStack() },
                            onRegistered = {
                                if (!navController.popBackStack(Routes.LOGIN, false)) {
                                    navController.navigate(Routes.LOGIN) {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.FORGOT_PASSWORD) {
                    PageScaffold(
                        title = stringResource(R.string.page_forgot_password),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        ForgotPasswordScreen(
                            repository = container.accountAuthRepository,
                            onBack = { navController.popBackStack() },
                            onFinished = {
                                if (!navController.popBackStack(Routes.LOGIN, false)) {
                                    navController.navigate(Routes.LOGIN) {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.SETTINGS) {
                    PageScaffold(
                        title = stringResource(R.string.page_settings),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        SettingsScreen(
                            accountRepository = container.accountRepository,
                            accountAuthRepository = container.accountAuthRepository,
                            notificationRepository = container.notificationRepository,
                            onLogin = { navController.navigate(Routes.LOGIN) },
                            onLogout = {
                                scope.launch {
                                    container.authRepository.logout()
                                    navController.popBackStack()
                                }
                            },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.NOTIFICATIONS) {
                    PageScaffold(
                        title = stringResource(R.string.page_notifications),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        NotificationsScreen(
                            repository = container.notificationRepository,
                            isLoggedIn = loggedIn,
                            onOpenNotification = { targetKey, commentId ->
                                val target = targetKey?.let(::parseTargetKey)
                                when {
                                    target != null -> openTarget(target)
                                    commentId != null -> navController.navigate(
                                        Routes.feature(context.getString(R.string.feature_comment_title, commentId)),
                                    )
                                }
                            },
                            onLogin = { navController.navigate(Routes.LOGIN) },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.SHOP) {
                    PageScaffold(
                        title = stringResource(R.string.page_shop),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        ShopScreen(
                            repository = container.shopRepository,
                            isLoggedIn = loggedIn,
                            onLogin = { navController.navigate(Routes.LOGIN) },
                            onOpenWallet = { navController.navigate(Routes.WALLET) },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.WALLET) {
                    PageScaffold(
                        title = stringResource(R.string.page_wallet),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        WalletScreen(
                            shopRepository = container.shopRepository,
                            accountRepository = container.accountRepository,
                            isLoggedIn = loggedIn,
                            onLogin = { navController.navigate(Routes.LOGIN) },
                            onOpenShop = { navController.navigate(Routes.SHOP) },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.MY_PLAYLISTS) {
                    PageScaffold(
                        title = stringResource(R.string.page_my_playlists),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        MyPlaylistsScreen(
                            repository = container.playlistRepository,
                            loggedIn = loggedIn,
                            onLogin = { navController.navigate(Routes.LOGIN) },
                            onOpenPlaylist = { id ->
                                navController.navigate(Routes.playlist(id)) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.TAG_INDEX) {
                    PageScaffold(
                        title = stringResource(R.string.page_tag_index),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        TagIndexScreen(
                            repository = container.tagIndexRepository,
                            onOpenTag = { id ->
                                navController.navigate(Routes.tag(id)) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.STAT) {
                    PageScaffold(
                        title = stringResource(R.string.page_stat),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        StatScreen(
                            repository = container.statRepository,
                            session = container.session,
                            onOpenSong = { id ->
                                navController.navigate(Routes.song(id)) {
                                    launchSingleTop = true
                                }
                            },
                            onLogin = { navController.navigate(Routes.LOGIN) },
                            onOpenSponsor = { navController.navigate(Routes.SHOP) },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.CUSTOM_RANKING) {
                    PageScaffold(
                        title = stringResource(R.string.page_custom_ranking),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        CustomRankingScreen(
                            repository = container.customRankingRepository,
                            session = container.session,
                            onOpenSong = { id ->
                                navController.navigate(Routes.song(id)) {
                                    launchSingleTop = true
                                }
                            },
                            onLogin = { navController.navigate(Routes.LOGIN) },
                            onOpenSponsor = { navController.navigate(Routes.SHOP) },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(Routes.COLLECTION_LIST) {
                    PageScaffold(
                        title = stringResource(R.string.page_collections),
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        CollectionListScreen(
                            repository = container.collectionRepository,
                            onOpenCollection = { target ->
                                navController.navigate(Routes.collection(target)) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = contentModifier,
                        )
                    }
                }
                composable(
                    route = Routes.COLLECTION_MANUAL,
                    arguments = listOf(
                        navArgument("collectionId") { type = NavType.IntType },
                    ),
                ) { entry ->
                    val collectionId = entry.arguments?.getInt("collectionId") ?: 0
                    if (collectionId <= 0) {
                        PageScaffold(
                            title = stringResource(R.string.page_collections),
                            onBack = { navController.popBackStack() },
                        ) { contentModifier ->
                            FeatureScreen(stringResource(R.string.page_invalid_collection), modifier = contentModifier)
                        }
                    } else {
                        val target = CollectionTarget.Manual(collectionId)
                        CollectionRouteContent(
                            container = container,
                            target = target,
                            navController = navController,
                        )
                    }
                }
                composable(
                    route = Routes.COLLECTION_DERIVED,
                    arguments = listOf(
                        navArgument("kind") { type = NavType.StringType },
                        navArgument("sourceId") { type = NavType.IntType },
                        navArgument("entityType") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val kind = DerivedCollectionKind.entries.firstOrNull {
                        it.key == entry.arguments?.getString("kind")
                    }
                    val sourceId = entry.arguments?.getInt("sourceId") ?: 0
                    val entityType = LitEntityType.fromKey(entry.arguments?.getString("entityType"))
                    if (kind == null || sourceId <= 0) {
                        PageScaffold(
                            title = stringResource(R.string.page_collections),
                            onBack = { navController.popBackStack() },
                        ) { contentModifier ->
                            FeatureScreen(stringResource(R.string.page_invalid_collection), modifier = contentModifier)
                        }
                    } else {
                        CollectionRouteContent(
                            container = container,
                            target = CollectionTarget.Derived(kind, sourceId, entityType),
                            navController = navController,
                        )
                    }
                }
                composable(
                    route = Routes.COLLECTION_SHARE_MANUAL,
                    arguments = listOf(
                        navArgument("collectionId") { type = NavType.IntType },
                    ),
                ) { entry ->
                    val collectionId = entry.arguments?.getInt("collectionId") ?: 0
                    if (collectionId <= 0) {
                        PageScaffold(
                            title = stringResource(R.string.page_share_card),
                            onBack = { navController.popBackStack() },
                        ) { contentModifier ->
                            FeatureScreen(stringResource(R.string.page_invalid_collection), modifier = contentModifier)
                        }
                    } else {
                        CollectionShareRouteContent(
                            container = container,
                            target = CollectionTarget.Manual(collectionId),
                            navController = navController,
                        )
                    }
                }
                composable(
                    route = Routes.COLLECTION_SHARE_DERIVED,
                    arguments = listOf(
                        navArgument("kind") { type = NavType.StringType },
                        navArgument("sourceId") { type = NavType.IntType },
                        navArgument("entityType") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val kind = DerivedCollectionKind.entries.firstOrNull {
                        it.key == entry.arguments?.getString("kind")
                    }
                    val sourceId = entry.arguments?.getInt("sourceId") ?: 0
                    val entityType = LitEntityType.fromKey(entry.arguments?.getString("entityType"))
                    if (kind == null || sourceId <= 0) {
                        PageScaffold(
                            title = stringResource(R.string.page_share_card),
                            onBack = { navController.popBackStack() },
                        ) { contentModifier ->
                            FeatureScreen(stringResource(R.string.page_invalid_collection), modifier = contentModifier)
                        }
                    } else {
                        CollectionShareRouteContent(
                            container = container,
                            target = CollectionTarget.Derived(kind, sourceId, entityType),
                            navController = navController,
                        )
                    }
                }
                composable(
                    route = Routes.FEATURE,
                    arguments = listOf(
                        navArgument("title") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val title = entry.arguments?.getString("title") ?: stringResource(R.string.feature_default_title)
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
            addToPlaylistSongId?.let { songId ->
                AddToPlaylistSheet(
                    repository = container.playlistRepository,
                    songId = songId,
                    loggedIn = loggedIn,
                    onDismiss = { addToPlaylistSongId = null },
                    onRequireLogin = {
                        addToPlaylistSongId = null
                        navController.navigate(Routes.LOGIN)
                    },
                    onOpenMyPlaylists = {
                        addToPlaylistSongId = null
                        navController.navigate(Routes.MY_PLAYLISTS)
                    },
                )
            }
            if (updateState.visible) {
                updateState.update?.let { update ->
                    UpdateDialog(
                        update = update,
                        onDismiss = updateViewModel::dismiss,
                        onDownload = { source ->
                            updateViewModel.startDownload(update, source)
                        },
                    )
                }
            }
            updateState.download?.let { download ->
                UpdateDownloadDialog(
                    download = download,
                    onCancel = updateViewModel::cancelDownload,
                    onClose = updateViewModel::closeDownload,
                    onRetry = {
                        updateState.update?.let { update ->
                            updateViewModel.startDownload(update, download.source)
                        }
                    },
                    onInstall = {
                        download.apkFile?.let(requestInstall)
                    },
                )
            }
            if (showInstallPermission) {
                AlertDialog(
                    onDismissRequest = { showInstallPermission = false },
                    title = { Text(stringResource(R.string.update_install_permission_title)) },
                    text = {
                        Text(stringResource(R.string.update_install_permission_message))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showInstallPermission = false
                                runCatching {
                                    installPermissionLauncher.launch(installPermissionIntent(context))
                                }.onFailure {
                                    Toast.makeText(context, context.getString(R.string.toast_install_permission_settings_failed), Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) {
                            Text(stringResource(R.string.action_go_settings))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showInstallPermission = false }) {
                            Text(stringResource(R.string.action_later))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CollectionRouteContent(
    container: AppContainer,
    target: CollectionTarget,
    navController: NavHostController,
) {
    PageScaffold(
        title = stringResource(R.string.page_collections),
        onBack = { navController.popBackStack() },
    ) { contentModifier ->
        CollectionDetailScreen(
            repository = container.collectionRepository,
            target = target,
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
            onShare = {
                navController.navigate(Routes.collectionShare(target)) {
                    launchSingleTop = true
                }
            },
            modifier = contentModifier,
        )
    }
}

@Composable
private fun CollectionShareRouteContent(
    container: AppContainer,
    target: CollectionTarget,
    navController: NavHostController,
) {
    PageScaffold(
        title = stringResource(R.string.page_share_card),
        onBack = { navController.popBackStack() },
    ) { contentModifier ->
        CollectionShareScreen(
            repository = container.collectionRepository,
            target = target,
            modifier = contentModifier,
        )
    }
}

@Composable
private fun CompactBottomBar(
    currentRoute: String?,
    onSelect: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        color = scheme.surfaceContainer,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = bottomBarRowHeight)
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TOP_LEVEL_DESTINATIONS.forEach { destination ->
                    val selected = currentRoute == destination.route
                    val isPlayer = destination.route == Routes.PLAYER
                    val label = stringResource(destination.labelRes)
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) scheme.primary else scheme.onSurfaceVariant,
                        animationSpec = tween(MotionDurations.FAST),
                        label = "bottomBarContentColor",
                    )
                    val playerContainerColor by animateColorAsState(
                        targetValue = if (selected) scheme.primary else scheme.primaryContainer,
                        animationSpec = tween(MotionDurations.FAST),
                        label = "bottomBarPlayerContainer",
                    )
                    val playerContentColor by animateColorAsState(
                        targetValue = if (selected) scheme.onPrimary else scheme.onPrimaryContainer,
                        animationSpec = tween(MotionDurations.FAST),
                        label = "bottomBarPlayerContent",
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = selected,
                                enabled = enabled,
                                role = Role.Tab,
                                onClick = { onSelect(destination.route) },
                            )
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (isPlayer) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(playerContainerColor),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = playerContentColor,
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                        } else {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = null,
                                modifier = Modifier.size(23.dp),
                                tint = contentColor,
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageScaffold(
    title: String?,
    onBack: (() -> Unit)? = null,
    sharedElementScope: AnimatedVisibilityScope? = null,
    content: @Composable (Modifier) -> Unit,
) {
    val topLevelBottomInset = if (onBack == null) {
        Modifier
            .windowInsetsPadding(
                WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
            )
            .padding(bottom = bottomBarRowHeight)
    } else {
        Modifier
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(topLevelBottomInset)
            .background(MaterialTheme.colorScheme.background),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentSize.topBarHeight),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = if (onBack != null) 56.dp else 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (title == null) {
                        BrandTitle()
                    } else {
                        Text(
                            text = title,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(
                    if (onBack != null) {
                        Modifier.windowInsetsPadding(
                            WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            CompositionLocalProvider(LocalSharedElementVisibilityScope provides sharedElementScope) {
                content(Modifier.fillMaxSize())
            }
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
