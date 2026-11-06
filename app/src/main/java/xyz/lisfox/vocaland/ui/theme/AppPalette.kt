package xyz.lisfox.vocaland.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import xyz.lisfox.vocaland.R

enum class AppPalette(
    @StringRes val labelRes: Int,
    val hue: Float,
    val saturation: Float,
) {
    DeepPurple(R.string.palette_deep_purple, 258f, 0.48f),
    Violet(R.string.palette_violet, 286f, 0.50f),
    Blue(R.string.palette_blue, 214f, 0.55f),
    Cyan(R.string.palette_cyan, 192f, 0.55f),
    Teal(R.string.palette_teal, 172f, 0.45f),
    Green(R.string.palette_green, 142f, 0.42f),
    Amber(R.string.palette_amber, 40f, 0.75f),
    Orange(R.string.palette_orange, 24f, 0.70f),
    Red(R.string.palette_red, 357f, 0.58f),
    Pink(R.string.palette_pink, 334f, 0.52f),
    ;

    val previewColor: Color
        get() = tone(hue, saturation, 0.52f)

    fun colorScheme(darkTheme: Boolean): ColorScheme =
        if (darkTheme) darkScheme() else lightScheme()

    private fun lightScheme(): ColorScheme {
        val secondaryHue = (hue + 58f) % 360f
        val primaryColor = tone(hue, saturation, 0.42f)
        val secondaryColor = tone(hue, saturation * 0.32f, 0.42f)
        val tertiaryColor = tone(secondaryHue, saturation * 0.40f, 0.42f)
        return lightColorScheme(
            primary = primaryColor,
            onPrimary = readableOn(primaryColor, hue, saturation),
            primaryContainer = tone(hue, saturation * 0.55f, 0.90f),
            onPrimaryContainer = tone(hue, saturation * 0.65f, 0.14f),
            inversePrimary = tone(hue, saturation * 0.65f, 0.80f),
            secondary = secondaryColor,
            onSecondary = readableOn(secondaryColor, hue, saturation * 0.32f),
            secondaryContainer = tone(hue, saturation * 0.30f, 0.90f),
            onSecondaryContainer = tone(hue, saturation * 0.35f, 0.16f),
            tertiary = tertiaryColor,
            onTertiary = readableOn(tertiaryColor, secondaryHue, saturation * 0.40f),
            tertiaryContainer = tone(secondaryHue, saturation * 0.35f, 0.90f),
            onTertiaryContainer = tone(secondaryHue, saturation * 0.42f, 0.16f),
            background = tone(hue, 0.03f, 0.985f),
            onBackground = tone(hue, 0.06f, 0.12f),
            surface = tone(hue, 0.03f, 0.985f),
            onSurface = tone(hue, 0.06f, 0.12f),
            surfaceVariant = tone(hue, 0.08f, 0.90f),
            onSurfaceVariant = tone(hue, 0.07f, 0.28f),
            surfaceTint = tone(hue, saturation, 0.42f),
            inverseSurface = tone(hue, 0.06f, 0.20f),
            inverseOnSurface = tone(hue, 0.03f, 0.95f),
            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            outline = tone(hue, 0.06f, 0.50f),
            outlineVariant = tone(hue, 0.05f, 0.80f),
            scrim = Color.Black,
            surfaceBright = tone(hue, 0.03f, 0.985f),
            surfaceDim = tone(hue, 0.03f, 0.87f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = tone(hue, 0.035f, 0.965f),
            surfaceContainer = tone(hue, 0.045f, 0.94f),
            surfaceContainerHigh = tone(hue, 0.05f, 0.92f),
            surfaceContainerHighest = tone(hue, 0.055f, 0.90f),
        )
    }

    private fun darkScheme(): ColorScheme {
        val secondaryHue = (hue + 58f) % 360f
        return darkColorScheme(
            primary = tone(hue, saturation * 0.65f, 0.80f),
            onPrimary = tone(hue, saturation * 0.65f, 0.18f),
            primaryContainer = tone(hue, saturation * 0.70f, 0.30f),
            onPrimaryContainer = tone(hue, saturation * 0.40f, 0.92f),
            inversePrimary = tone(hue, saturation, 0.42f),
            secondary = tone(hue, saturation * 0.28f, 0.80f),
            onSecondary = tone(hue, saturation * 0.30f, 0.18f),
            secondaryContainer = tone(hue, saturation * 0.30f, 0.30f),
            onSecondaryContainer = tone(hue, saturation * 0.22f, 0.92f),
            tertiary = tone(secondaryHue, saturation * 0.35f, 0.80f),
            onTertiary = tone(secondaryHue, saturation * 0.35f, 0.18f),
            tertiaryContainer = tone(secondaryHue, saturation * 0.35f, 0.30f),
            onTertiaryContainer = tone(secondaryHue, saturation * 0.30f, 0.92f),
            background = tone(hue, 0.06f, 0.07f),
            onBackground = tone(hue, 0.04f, 0.90f),
            surface = tone(hue, 0.06f, 0.07f),
            onSurface = tone(hue, 0.04f, 0.90f),
            surfaceVariant = tone(hue, 0.07f, 0.30f),
            onSurfaceVariant = tone(hue, 0.05f, 0.80f),
            surfaceTint = tone(hue, saturation * 0.65f, 0.80f),
            inverseSurface = tone(hue, 0.04f, 0.90f),
            inverseOnSurface = tone(hue, 0.06f, 0.18f),
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC),
            outline = tone(hue, 0.05f, 0.60f),
            outlineVariant = tone(hue, 0.05f, 0.30f),
            scrim = Color.Black,
            surfaceBright = tone(hue, 0.05f, 0.24f),
            surfaceDim = tone(hue, 0.06f, 0.06f),
            surfaceContainerLowest = tone(hue, 0.06f, 0.04f),
            surfaceContainerLow = tone(hue, 0.06f, 0.10f),
            surfaceContainer = tone(hue, 0.06f, 0.12f),
            surfaceContainerHigh = tone(hue, 0.06f, 0.16f),
            surfaceContainerHighest = tone(hue, 0.06f, 0.22f),
        )
    }
}

private fun readableOn(background: Color, hue: Float, saturation: Float): Color =
    if (background.luminance() > 0.18f) {
        tone(hue, saturation * 0.6f, 0.12f)
    } else {
        Color.White
    }

private fun tone(hue: Float, saturation: Float, lightness: Float): Color =
    Color.hsl(
        hue = (hue % 360f + 360f) % 360f,
        saturation = saturation.coerceIn(0f, 1f),
        lightness = lightness.coerceIn(0f, 1f),
    )
