package xyz.lisfox.vocaland.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.lisfox.vocaland.ui.theme.AppPalette
import xyz.lisfox.vocaland.ui.theme.ThemeMode
import xyz.lisfox.vocaland.ui.theme.ThemeSettings

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        ThemeSettings(
            palette = readPalette(),
            mode = readMode(),
        ),
    )
    val settings: StateFlow<ThemeSettings> = _settings.asStateFlow()

    fun setPalette(palette: AppPalette) {
        prefs.edit().putString(KEY_PALETTE, palette.name).apply()
        _settings.value = _settings.value.copy(palette = palette)
    }

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(mode = mode)
    }

    private fun readPalette(): AppPalette {
        val name = prefs.getString(KEY_PALETTE, null) ?: return AppPalette.DeepPurple
        return runCatching { AppPalette.valueOf(name) }.getOrDefault(AppPalette.DeepPurple)
    }

    private fun readMode(): ThemeMode {
        val name = prefs.getString(KEY_MODE, null) ?: return ThemeMode.System
        return runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.System)
    }

    private companion object {
        const val PREFS_NAME = "vocaland_appearance"
        const val KEY_PALETTE = "palette"
        const val KEY_MODE = "theme_mode"
    }
}
