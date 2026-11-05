package xyz.lisfox.vocaland.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import xyz.lisfox.vocaland.ui.user.UserScreen
import xyz.lisfox.vocaland.ui.voting.EdVotingScreen
import xyz.lisfox.vocaland.ui.voting.TrendingVotingScreen
import xyz.lisfox.vocaland.ui.voting.VotingHubScreen
import xyz.lisfox.vocaland.util.SiteTarget
import xyz.lisfox.vocaland.util.parseTargetKey

private val bottomBarRowHeight = 62.dp

@Composable
fun VocaLandApp(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val themeSettings by container.themePreferences.settings.collectAsStateWithLifecycle()
    val sessionState by container.session.state.collectAsStateWithLifecycle()

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
                enter = fadeIn(animationSpec = tween(200)) +
                    slideInHorizontally(animationSpec = tween(320)) { -it / 5 },
                exit = fadeOut(animationSpec = tween(220)) +
                    slideOutHorizontally(animationSpec = tween(320)) { -it / 5 },
            ) {
                CompactBottomBar(
                    currentRoute = currentRoute,
                    onSelect = navigateToTopLevel,
                    enabled = isTopLevel,
                )
            }
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
                    PageScaffold(title = "排行榜") { contentModifier ->
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
                    PageScaffold(title = "播放器") { contentModifier ->
                        PlayerScreen(
                            controller = container.bgmPlayerController,
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
                        title = "用户",
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        if (userId <= 0) {
                            FeatureScreen(
                                title = "无效的用户",
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
                        title = "歌单广场",
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
                        title = "歌单",
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        if (playlistId <= 0) {
                            FeatureScreen(
                                title = "无效的歌单",
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
                                onOpenMyPlaylists = {
                                    navController.navigate(Routes.MY_PLAYLISTS) {
                                        launchSingleTop = true
                                    }
                                },
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
                                        title = "评分",
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
                                        title = "评论",
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
                        title = "投票",
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
                        title = kind?.label ?: "投票",
                        onBack = { navController.popBackStack() },
                    ) { contentModifier ->
                        if (kind == null || campaignId <= 0) {
                            FeatureScreen(
                                title = "无效的投票",
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
                        title = "注册",
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
                        title = "找回密码",
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
                        title = "账号设置",
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
                        title = "通知中心",
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
                                        Routes.feature("评论 #$commentId"),
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
                        title = "商城",
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
                        title = "我的钱包",
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
                        title = "我的歌单",
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
                        title = "标签图鉴",
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
                        title = "数据统计",
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
                        title = "自定义排行榜",
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
                        title = "收藏册",
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
                            title = "收藏册",
                            onBack = { navController.popBackStack() },
                        ) { contentModifier ->
                            FeatureScreen("无效的收藏册", modifier = contentModifier)
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
                            title = "收藏册",
                            onBack = { navController.popBackStack() },
                        ) { contentModifier ->
                            FeatureScreen("无效的收藏册", modifier = contentModifier)
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
                            title = "分享卡",
                            onBack = { navController.popBackStack() },
                        ) { contentModifier ->
                            FeatureScreen("无效的收藏册", modifier = contentModifier)
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
                            title = "分享卡",
                            onBack = { navController.popBackStack() },
                        ) { contentModifier ->
                            FeatureScreen("无效的收藏册", modifier = contentModifier)
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
        title = "收藏册",
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
        title = "分享卡",
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
                    .height(bottomBarRowHeight)
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TOP_LEVEL_DESTINATIONS.forEach { destination ->
                    val selected = currentRoute == destination.route
                    val isPlayer = destination.route == Routes.PLAYER
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
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) scheme.primary else scheme.primaryContainer,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (selected) scheme.onPrimary else scheme.onPrimaryContainer,
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                        } else {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                modifier = Modifier.size(23.dp),
                                tint = if (selected) scheme.primary else scheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
                            maxLines = 1,
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
                .height(48.dp),
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
                            contentDescription = "返回",
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
