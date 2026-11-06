package xyz.lisfox.vocaland.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerVideoSettings(
    val frameRate: Int = PlayerVideoPreferences.FRAME_RATE_DEFAULT,
    val fadePercent: Int = PlayerVideoPreferences.FADE_PERCENT_DEFAULT,
)

class PlayerVideoPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        PlayerVideoSettings(
            frameRate = prefs.getInt(KEY_FRAME_RATE, FRAME_RATE_DEFAULT)
                .coerceIn(FRAME_RATE_MIN, FRAME_RATE_MAX),
            fadePercent = prefs.getInt(KEY_FADE_PERCENT, FADE_PERCENT_DEFAULT)
                .coerceIn(FADE_PERCENT_MIN, FADE_PERCENT_MAX),
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

    companion object {
        const val FRAME_RATE_MIN = 1
        const val FRAME_RATE_MAX = 10
        const val FRAME_RATE_DEFAULT = 1
        const val FADE_PERCENT_MIN = 0
        const val FADE_PERCENT_MAX = 100
        const val FADE_PERCENT_DEFAULT = 50

        private const val PREFS_NAME = "vocaland_player_video"
        private const val KEY_FRAME_RATE = "frame_rate"
        private const val KEY_FADE_PERCENT = "fade_percent"
    }
}
