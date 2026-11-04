package xyz.lisfox.vocaland.data.session

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import xyz.lisfox.vocaland.data.model.CachedUserDto
import xyz.lisfox.vocaland.data.model.LoginResponse
import xyz.lisfox.vocaland.data.model.LogoutRequest
import xyz.lisfox.vocaland.data.model.RefreshRequest
import xyz.lisfox.vocaland.data.remote.AppJson
import xyz.lisfox.vocaland.data.remote.AuthApi

data class SessionState(
    val user: CachedUserDto? = null,
    val ready: Boolean = false,
) {
    val isLoggedIn: Boolean get() = user != null
}

private data class StoredTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
)

class AuthSession(
    context: Context,
    private val authApi: AuthApi,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val refreshMutex = Mutex()

    @Volatile
    private var tokens: StoredTokens? = readTokens()

    private val _state = MutableStateFlow(
        SessionState(user = readUser(), ready = true),
    )
    val state: StateFlow<SessionState> = _state.asStateFlow()

    fun currentAccessToken(): String? {
        val current = tokens ?: return null
        return if (current.expiresAtMillis > System.currentTimeMillis()) {
            current.accessToken
        } else {
            null
        }
    }

    fun hasRefreshToken(): Boolean = !tokens?.refreshToken.isNullOrBlank()

    suspend fun validAccessToken(): String? {
        currentAccessToken()?.let { return it }
        val refreshed = refresh()
        return if (refreshed) currentAccessToken() else null
    }

    suspend fun refresh(): Boolean = refreshMutex.withLock {
        val current = tokens ?: return@withLock false
        if (current.refreshToken.isBlank()) return@withLock false

        var success = false
        runCatching {
            authApi.refresh(RefreshRequest(refreshToken = current.refreshToken))
        }.onSuccess { response ->
            if (response.accessToken.isNotBlank() && response.refreshToken.isNotBlank()) {
                store(response)
                success = true
            }
        }.onFailure { error ->
            val code = (error as? retrofit2.HttpException)?.code()
            if (code == 401 || code == 403) {
                clear()
            }
        }

        success
    }

    fun saveLogin(response: LoginResponse) {
        store(response)
    }

    suspend fun logout() {
        val refreshToken = tokens?.refreshToken
        clear()
        if (!refreshToken.isNullOrBlank()) {
            runCatching { authApi.logout(LogoutRequest(refreshToken = refreshToken)) }
        }
    }

    fun updateUser(user: CachedUserDto) {
        prefs.edit().putString(KEY_USER, AppJson.encodeToString(user)).apply()
        _state.value = _state.value.copy(user = user, ready = true)
    }

    fun clear() {
        tokens = null
        prefs.edit().clear().apply()
        _state.value = SessionState(user = null, ready = true)
    }

    private fun store(response: LoginResponse) {
        if (response.accessToken.isBlank() || response.refreshToken.isBlank()) return

        val expiresInMillis = if (response.expiresIn > 0) {
            response.expiresIn * 1000
        } else {
            DEFAULT_EXPIRES_MS
        }
        val stored = StoredTokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            expiresAtMillis = System.currentTimeMillis() + expiresInMillis - REFRESH_BUFFER_MS,
        )
        tokens = stored

        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, stored.accessToken)
            .putString(KEY_REFRESH_TOKEN, stored.refreshToken)
            .putLong(KEY_EXPIRES_AT, stored.expiresAtMillis)
            .apply()

        val user = response.user
        if (user != null) {
            prefs.edit().putString(KEY_USER, AppJson.encodeToString(user)).apply()
            _state.value = SessionState(user = user, ready = true)
        } else {
            _state.value = _state.value.copy(ready = true)
        }
    }

    private fun readTokens(): StoredTokens? {
        val access = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        return StoredTokens(
            accessToken = access,
            refreshToken = refresh,
            expiresAtMillis = prefs.getLong(KEY_EXPIRES_AT, 0L),
        )
    }

    private fun readUser(): CachedUserDto? {
        val raw = prefs.getString(KEY_USER, null) ?: return null
        return runCatching { AppJson.decodeFromString(CachedUserDto.serializer(), raw) }.getOrNull()
    }

    private companion object {
        const val PREFS_NAME = "vocaland_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_USER = "cached_user"

        const val REFRESH_BUFFER_MS = 60_000L
        const val DEFAULT_EXPIRES_MS = 30 * 60_000L
    }
}
