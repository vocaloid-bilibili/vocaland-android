package xyz.lisfox.vocaland.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import xyz.lisfox.vocaland.R

enum class ThemeMode(@StringRes val labelRes: Int) {
    System(R.string.theme_follow_system),
    Light(R.string.theme_light),
    Dark(R.string.theme_dark),
}

data class ThemeSettings(
    val palette: AppPalette = AppPalette.DeepPurple,
    val mode: ThemeMode = ThemeMode.System,
)

val LocalAppColorScheme = compositionLocalOf<ColorScheme> {
    AppPalette.DeepPurple.colorScheme(false)
}

@Composable
fun resolveDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

private const val THEME_TRANSITION_MS = 320

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme = target.copy(
    primary = themeColor(target.primary, "themePrimary"),
    onPrimary = themeColor(target.onPrimary, "themeOnPrimary"),
    primaryContainer = themeColor(target.primaryContainer, "themePrimaryContainer"),
    onPrimaryContainer = themeColor(target.onPrimaryContainer, "themeOnPrimaryContainer"),
    inversePrimary = themeColor(target.inversePrimary, "themeInversePrimary"),
    secondary = themeColor(target.secondary, "themeSecondary"),
    onSecondary = themeColor(target.onSecondary, "themeOnSecondary"),
    secondaryContainer = themeColor(target.secondaryContainer, "themeSecondaryContainer"),
    onSecondaryContainer = themeColor(target.onSecondaryContainer, "themeOnSecondaryContainer"),
    tertiary = themeColor(target.tertiary, "themeTertiary"),
    onTertiary = themeColor(target.onTertiary, "themeOnTertiary"),
    tertiaryContainer = themeColor(target.tertiaryContainer, "themeTertiaryContainer"),
    onTertiaryContainer = themeColor(target.onTertiaryContainer, "themeOnTertiaryContainer"),
    background = themeColor(target.background, "themeBackground"),
    onBackground = themeColor(target.onBackground, "themeOnBackground"),
    surface = themeColor(target.surface, "themeSurface"),
    onSurface = themeColor(target.onSurface, "themeOnSurface"),
    surfaceVariant = themeColor(target.surfaceVariant, "themeSurfaceVariant"),
    onSurfaceVariant = themeColor(target.onSurfaceVariant, "themeOnSurfaceVariant"),
    surfaceTint = themeColor(target.surfaceTint, "themeSurfaceTint"),
    inverseSurface = themeColor(target.inverseSurface, "themeInverseSurface"),
    inverseOnSurface = themeColor(target.inverseOnSurface, "themeInverseOnSurface"),
    error = themeColor(target.error, "themeError"),
    onError = themeColor(target.onError, "themeOnError"),
    errorContainer = themeColor(target.errorContainer, "themeErrorContainer"),
    onErrorContainer = themeColor(target.onErrorContainer, "themeOnErrorContainer"),
    outline = themeColor(target.outline, "themeOutline"),
    outlineVariant = themeColor(target.outlineVariant, "themeOutlineVariant"),
    scrim = themeColor(target.scrim, "themeScrim"),
    surfaceBright = themeColor(target.surfaceBright, "themeSurfaceBright"),
    surfaceDim = themeColor(target.surfaceDim, "themeSurfaceDim"),
    surfaceContainerLowest = themeColor(target.surfaceContainerLowest, "themeSurfaceContainerLowest"),
    surfaceContainerLow = themeColor(target.surfaceContainerLow, "themeSurfaceContainerLow"),
    surfaceContainer = themeColor(target.surfaceContainer, "themeSurfaceContainer"),
    surfaceContainerHigh = themeColor(target.surfaceContainerHigh, "themeSurfaceContainerHigh"),
    surfaceContainerHighest = themeColor(target.surfaceContainerHighest, "themeSurfaceContainerHighest"),
)

@Composable
private fun themeColor(target: Color, label: String): Color =
    animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = THEME_TRANSITION_MS),
        label = label,
    ).value

@Composable
fun VocaLandTheme(
    palette: AppPalette = AppPalette.DeepPurple,
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val darkTheme = resolveDarkTheme(themeMode)
    val targetScheme = remember(palette, darkTheme) { palette.colorScheme(darkTheme) }
    val colorScheme = animateColorScheme(targetScheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setBackgroundDrawable(ColorDrawable(colorScheme.background.toArgb()))
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        CompositionLocalProvider(
            LocalAppColorScheme provides colorScheme,
            LocalContentColor provides colorScheme.onBackground,
            content = content,
        )
    }
}
