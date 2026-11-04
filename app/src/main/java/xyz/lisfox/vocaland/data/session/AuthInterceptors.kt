package xyz.lisfox.vocaland.data.session

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class AuthInterceptor(
    private val session: AuthSession,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { session.validAccessToken() }
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

class TokenAuthenticator(
    private val session: AuthSession,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization").isNullOrBlank()) return null
        if (responseCount(response) >= 2) return null

        val refreshed = runBlocking { session.refresh() }
        if (!refreshed) return null

        val token = session.currentAccessToken() ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count += 1
            prior = prior.priorResponse
        }
        return count
    }
}
