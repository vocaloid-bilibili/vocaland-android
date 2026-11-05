package xyz.lisfox.vocaland.di

import android.content.Context
import android.content.pm.ApplicationInfo
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import xyz.lisfox.vocaland.data.preferences.ThemePreferences
import xyz.lisfox.vocaland.data.remote.AppJson
import xyz.lisfox.vocaland.data.player.BgmPlayerController
import xyz.lisfox.vocaland.data.remote.AuthApi
import xyz.lisfox.vocaland.data.remote.CommunityApi
import xyz.lisfox.vocaland.data.remote.PlayUrlApi
import xyz.lisfox.vocaland.data.remote.VocaLandApi
import xyz.lisfox.vocaland.data.repository.ArtistDetailRepository
import xyz.lisfox.vocaland.data.repository.AuthRepository
import xyz.lisfox.vocaland.data.repository.BgmRepository
import xyz.lisfox.vocaland.data.repository.HomeRepository
import xyz.lisfox.vocaland.data.repository.PlaylistRepository
import xyz.lisfox.vocaland.data.repository.RankingRepository
import xyz.lisfox.vocaland.data.repository.SearchRepository
import xyz.lisfox.vocaland.data.repository.SongDetailRepository
import xyz.lisfox.vocaland.data.repository.TagDetailRepository
import xyz.lisfox.vocaland.data.repository.UserRepository
import xyz.lisfox.vocaland.data.repository.VotingRepository
import xyz.lisfox.vocaland.data.session.AuthInterceptor
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.data.session.TokenAuthenticator
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val authRetrofit = Retrofit.Builder()
        .baseUrl(AUTH_BASE_URL)
        .addConverterFactory(AppJson.asConverterFactory(JSON_MEDIA_TYPE))
        .build()

    val authApi: AuthApi = authRetrofit.create(AuthApi::class.java)

    val session = AuthSession(appContext, authApi)

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

    val v3Api: VocaLandApi = retrofit(V3_BASE_URL).create(VocaLandApi::class.java)
    val v2Api: CommunityApi = retrofit(V2_BASE_URL).create(CommunityApi::class.java)
    val playUrlApi: PlayUrlApi = retrofit(PLAYURL_BASE_URL, playUrlClient)
        .create(PlayUrlApi::class.java)

    val authRepository = AuthRepository(authApi, session)
    val bgmRepository = BgmRepository(v3Api, playUrlApi)
    val bgmPlayerController = BgmPlayerController(appContext, bgmRepository)
    val homeRepository = HomeRepository(v3Api, v2Api)
    val votingRepository = VotingRepository(v3Api)
    val rankingRepository = RankingRepository(v3Api)
    val searchRepository = SearchRepository(v3Api)
    val songDetailRepository = SongDetailRepository(v3Api, v2Api)
    val artistDetailRepository = ArtistDetailRepository(v3Api)
    val tagDetailRepository = TagDetailRepository(v3Api)
    val playlistRepository = PlaylistRepository(v3Api)
    val userRepository = UserRepository(v3Api, v2Api)
    val themePreferences = ThemePreferences(appContext)

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
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
