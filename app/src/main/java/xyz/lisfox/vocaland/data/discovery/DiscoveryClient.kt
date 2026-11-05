package xyz.lisfox.vocaland.data.discovery

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import xyz.lisfox.vocaland.data.remote.AppJson
import xyz.lisfox.vocaland.data.session.AuthSession
import java.util.concurrent.TimeUnit

object DiscoveryAuth {
    @Volatile
    private var tokenProvider: (suspend () -> String?)? = null

    fun bindSession(session: AuthSession) {
        tokenProvider = { session.validAccessToken() }
    }

    internal suspend fun accessToken(): String? =
        runCatching { tokenProvider?.invoke() }.getOrNull()
}

object DiscoveryApiFactory {
    private const val BASE_URL = "https://api.vocabili.top/v3/"
    private val JSON_MEDIA_TYPE = "application/json".toMediaType()

    fun create(): DiscoveryApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthHeaderInterceptor())
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(AppJson.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(DiscoveryApi::class.java)
    }
}

private class AuthHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { DiscoveryAuth.accessToken() }
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
