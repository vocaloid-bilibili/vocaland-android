package xyz.lisfox.vocaland.di

import android.content.Context
import android.content.pm.ApplicationInfo
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import xyz.lisfox.vocaland.data.account.AccountApi
import xyz.lisfox.vocaland.data.account.AccountAuthApi
import xyz.lisfox.vocaland.data.account.AccountAuthRepository
import xyz.lisfox.vocaland.data.account.AccountRepository
import xyz.lisfox.vocaland.data.artist.ArtistStatsApi
import xyz.lisfox.vocaland.data.collection.CollectionApi
import xyz.lisfox.vocaland.data.collection.CollectionRepository
import xyz.lisfox.vocaland.data.community.CommunityExtrasApi
import xyz.lisfox.vocaland.data.community.CommunityRepository
import xyz.lisfox.vocaland.data.customranking.CustomRankingApi
import xyz.lisfox.vocaland.data.customranking.CustomRankingRepository
import xyz.lisfox.vocaland.data.danmaku.DanmakuRepository
import xyz.lisfox.vocaland.data.discovery.DiscoveryApiFactory
import xyz.lisfox.vocaland.data.discovery.DiscoveryAuth
import xyz.lisfox.vocaland.data.discovery.DiscoveryRepository
import xyz.lisfox.vocaland.data.discovery.TagIndexRepository
import xyz.lisfox.vocaland.data.notification.NotificationApi
import xyz.lisfox.vocaland.data.notification.NotificationRepository
import xyz.lisfox.vocaland.data.player.BgmPlayerController
import xyz.lisfox.vocaland.data.preferences.PlayerVideoPreferences
import xyz.lisfox.vocaland.data.preferences.ThemePreferences
import xyz.lisfox.vocaland.data.remote.AppJson
import xyz.lisfox.vocaland.data.remote.AuthApi
import xyz.lisfox.vocaland.data.remote.CommunityApi
import xyz.lisfox.vocaland.data.remote.PlayUrlApi
import xyz.lisfox.vocaland.data.remote.VocaLandApi
import xyz.lisfox.vocaland.data.repository.ArtistDetailRepository
import xyz.lisfox.vocaland.data.repository.AuthRepository
import xyz.lisfox.vocaland.data.repository.BgmRepository
import xyz.lisfox.vocaland.data.repository.HomeRepository
import xyz.lisfox.vocaland.data.repository.PlaylistRepository
import xyz.lisfox.vocaland.data.repository.PlaylistWriteApi
import xyz.lisfox.vocaland.data.repository.RankingRepository
import xyz.lisfox.vocaland.data.repository.SearchRepository
import xyz.lisfox.vocaland.data.repository.SongDetailRepository
import xyz.lisfox.vocaland.data.repository.TagDetailRepository
import xyz.lisfox.vocaland.data.repository.UserRepository
import xyz.lisfox.vocaland.data.repository.VotingHistoryApi
import xyz.lisfox.vocaland.data.repository.VotingRepository
import xyz.lisfox.vocaland.data.session.AuthInterceptor
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.data.session.TokenAuthenticator
import xyz.lisfox.vocaland.data.shop.ShopApi
import xyz.lisfox.vocaland.data.shop.ShopRepository
import xyz.lisfox.vocaland.data.songactions.SongActionsApi
import xyz.lisfox.vocaland.data.songactions.SongActionsRepository
import xyz.lisfox.vocaland.data.stat.StatApi
import xyz.lisfox.vocaland.data.stat.StatRepository
import xyz.lisfox.vocaland.data.update.UpdateApi
import xyz.lisfox.vocaland.data.update.UpdateDownloader
import xyz.lisfox.vocaland.data.update.UpdateRepository
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val authRetrofit = Retrofit.Builder()
        .baseUrl(AUTH_BASE_URL)
        .addConverterFactory(AppJson.asConverterFactory(JSON_MEDIA_TYPE))
        .build()

    val authApi: AuthApi = authRetrofit.create(AuthApi::class.java)

    val session = AuthSession(appContext, authApi)

    init {
        DiscoveryAuth.bindSession(session)
    }

    private val apiClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(session))
        .authenticator(TokenAuthenticator(session))
        .apply {
            if (isDebuggable()) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    },
                )
            }
        }
        .build()

    private val playUrlClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val updateClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val accountAuthRetrofit = Retrofit.Builder()
        .baseUrl(AUTH_BASE_URL)
        .client(apiClient)
        .addConverterFactory(AppJson.asConverterFactory(JSON_MEDIA_TYPE))
        .build()

    private val v3Retrofit = retrofit(V3_BASE_URL)
    private val v2Retrofit = retrofit(V2_BASE_URL)

    val v3Api: VocaLandApi = v3Retrofit.create(VocaLandApi::class.java)
    val v2Api: CommunityApi = v2Retrofit.create(CommunityApi::class.java)
    val playUrlApi: PlayUrlApi = retrofit(PLAYURL_BASE_URL, playUrlClient)
        .create(PlayUrlApi::class.java)
    val danmakuRepository = DanmakuRepository(playUrlClient)
    val accountApi: AccountApi = v2Retrofit.create(AccountApi::class.java)
    val accountAuthApi: AccountAuthApi = accountAuthRetrofit.create(AccountAuthApi::class.java)
    val notificationApi: NotificationApi = v2Retrofit.create(NotificationApi::class.java)
    val shopApi: ShopApi = v2Retrofit.create(ShopApi::class.java)
    val communityExtrasApi: CommunityExtrasApi =
        v2Retrofit.create(CommunityExtrasApi::class.java)
    val songActionsApi: SongActionsApi = v3Retrofit.create(SongActionsApi::class.java)
    val playlistWriteApi: PlaylistWriteApi = v3Retrofit.create(PlaylistWriteApi::class.java)
    val artistStatsApi: ArtistStatsApi = v3Retrofit.create(ArtistStatsApi::class.java)
    val collectionApi: CollectionApi = v3Retrofit.create(CollectionApi::class.java)
    val statApi: StatApi = v3Retrofit.create(StatApi::class.java)
    val customRankingApi: CustomRankingApi = v3Retrofit.create(CustomRankingApi::class.java)
    val votingHistoryApi: VotingHistoryApi = v3Retrofit.create(VotingHistoryApi::class.java)

    private val discoveryRepository = DiscoveryRepository(DiscoveryApiFactory.create())

    val authRepository = AuthRepository(authApi, session)
    val accountRepository = AccountRepository(accountApi, session)
    val accountAuthRepository = AccountAuthRepository(accountAuthApi)
    val notificationRepository = NotificationRepository(notificationApi)
    val shopRepository = ShopRepository(shopApi)
    val communityRepository = CommunityRepository(v2Api, communityExtrasApi)
    val bgmRepository = BgmRepository(v3Api, playUrlApi)
    val bgmPlayerController = BgmPlayerController(appContext, bgmRepository)
    val homeRepository = HomeRepository(v3Api, v2Api)
    val votingRepository = VotingRepository(v3Api, v2Api, votingHistoryApi)
    val rankingRepository = RankingRepository(v3Api, discoveryRepository)
    val searchRepository = SearchRepository(v3Api)
    val songDetailRepository = SongDetailRepository(v3Api, v2Api)
    val songActionsRepository = SongActionsRepository(songActionsApi)
    val artistDetailRepository = ArtistDetailRepository(v3Api, artistStatsApi)
    val tagDetailRepository = TagDetailRepository(v3Api, discoveryRepository)
    val tagIndexRepository = TagIndexRepository(discoveryRepository)
    val playlistRepository = PlaylistRepository(v3Api, playlistWriteApi)
    val collectionRepository = CollectionRepository(collectionApi)
    val statRepository = StatRepository(statApi, v3Api)
    val customRankingRepository = CustomRankingRepository(customRankingApi, v3Api)
    val userRepository = UserRepository(v3Api, v2Api, communityExtrasApi)
    val updateRepository = UpdateRepository(
        retrofit(UPDATE_BASE_URL, updateClient).create(UpdateApi::class.java),
    )
    val updateDownloader = UpdateDownloader(appContext)
    val themePreferences = ThemePreferences(appContext)
    val playerVideoPreferences = PlayerVideoPreferences(appContext)

    private fun retrofit(
        baseUrl: String,
        client: OkHttpClient = apiClient,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(AppJson.asConverterFactory(JSON_MEDIA_TYPE))
        .build()

    private fun isDebuggable(): Boolean =
        (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private companion object {
        const val AUTH_BASE_URL = "https://auth.vocabili.top/api/auth/"
        const val V2_BASE_URL = "https://api.vocabili.top/v2/"
        const val V3_BASE_URL = "https://api.vocabili.top/v3/"
        const val PLAYURL_BASE_URL = "https://proxy.vocaloid.world:3013/"
        const val UPDATE_BASE_URL = "https://api.github.com/"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
