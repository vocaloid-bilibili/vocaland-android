package xyz.lisfox.vocaland.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerVideoSettings(
    val frameRate: Int = PlayerVideoPreferences.FRAME_RATE_DEFAULT,
    val fadePercent: Int = PlayerVideoPreferences.FADE_PERCENT_DEFAULT,
    val videoBackgroundEnabled: Boolean = true,
    val danmakuEnabled: Boolean = true,
    val danmakuLimit: Int = PlayerVideoPreferences.DANMAKU_LIMIT_DEFAULT,
)

class PlayerVideoPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        PlayerVideoSettings(
            frameRate = prefs.getInt(KEY_FRAME_RATE, FRAME_RATE_DEFAULT)
                .coerceIn(FRAME_RATE_MIN, FRAME_RATE_MAX),
            fadePercent = prefs.getInt(KEY_FADE_PERCENT, FADE_PERCENT_DEFAULT)
                .coerceIn(FADE_PERCENT_MIN, FADE_PERCENT_MAX),
            videoBackgroundEnabled = prefs.getBoolean(KEY_VIDEO_BACKGROUND, true),
            danmakuEnabled = prefs.getBoolean(KEY_DANMAKU_ENABLED, true),
            danmakuLimit = normalizeDanmakuLimit(
                prefs.getInt(KEY_DANMAKU_LIMIT, DANMAKU_LIMIT_DEFAULT),
            ),
        ),
    )
    val settings: StateFlow<PlayerVideoSettings> = _settings.asStateFlow()

    fun setFrameRate(frameRate: Int) {
        val value = frameRate.coerceIn(FRAME_RATE_MIN, FRAME_RATE_MAX)
        if (value == _settings.value.frameRate) return
        prefs.edit().putInt(KEY_FRAME_RATE, value).apply()
        _settings.value = _settings.value.copy(frameRate = value)
    }

    fun setFadePercent(fadePercent: Int) {
        val value = fadePercent.coerceIn(FADE_PERCENT_MIN, FADE_PERCENT_MAX)
        if (value == _settings.value.fadePercent) return
        prefs.edit().putInt(KEY_FADE_PERCENT, value).apply()
        _settings.value = _settings.value.copy(fadePercent = value)
    }

    fun setVideoBackgroundEnabled(enabled: Boolean) {
        if (enabled == _settings.value.videoBackgroundEnabled) return
        prefs.edit().putBoolean(KEY_VIDEO_BACKGROUND, enabled).apply()
        _settings.value = _settings.value.copy(videoBackgroundEnabled = enabled)
    }

    fun setDanmakuEnabled(enabled: Boolean) {
        if (enabled == _settings.value.danmakuEnabled) return
        prefs.edit().putBoolean(KEY_DANMAKU_ENABLED, enabled).apply()
        _settings.value = _settings.value.copy(danmakuEnabled = enabled)
    }

    fun setDanmakuLimit(limit: Int) {
        val value = normalizeDanmakuLimit(limit)
        if (value == _settings.value.danmakuLimit) return
        prefs.edit().putInt(KEY_DANMAKU_LIMIT, value).apply()
        _settings.value = _settings.value.copy(danmakuLimit = value)
    }

    private fun normalizeDanmakuLimit(limit: Int): Int =
        if (limit <= 0) {
            DANMAKU_LIMIT_UNLIMITED
        } else {
            limit.coerceIn(DANMAKU_LIMIT_MIN, DANMAKU_LIMIT_MAX)
        }

    companion object {
        const val FRAME_RATE_MIN = 1
        const val FRAME_RATE_MAX = 10
        const val FRAME_RATE_DEFAULT = 1
        const val FADE_PERCENT_MIN = 0
        const val FADE_PERCENT_MAX = 100
        const val FADE_PERCENT_DEFAULT = 50
        const val DANMAKU_LIMIT_MIN = 50
        const val DANMAKU_LIMIT_MAX = 1000
        const val DANMAKU_LIMIT_STEP = 50
        const val DANMAKU_LIMIT_DEFAULT = 500
        const val DANMAKU_LIMIT_UNLIMITED = -1

        private const val PREFS_NAME = "vocaland_player_video"
        private const val KEY_FRAME_RATE = "frame_rate"
        private const val KEY_FADE_PERCENT = "fade_percent"
        private const val KEY_VIDEO_BACKGROUND = "video_background"
        private const val KEY_DANMAKU_ENABLED = "danmaku_enabled"
        private const val KEY_DANMAKU_LIMIT = "danmaku_limit"
    }
}
