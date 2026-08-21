package app.foscal.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import app.foscal.core.model.UiColor

private fun lightColorsFor(accent: AccentTokens) = lightColorScheme(
    primary = accent.primaryLight,
    onPrimary = Color.White,
    primaryContainer = accent.primaryContainerLight,
    onPrimaryContainer = accent.onPrimaryContainerLight,
    secondary = accent.secondaryLight,
    onSecondary = Color.White,
    secondaryContainer = accent.secondaryContainerLight,
    onSecondaryContainer = accent.onSecondaryContainerLight,
    background = FoscalLightBackground,
    onBackground = Color(0xFF1F2328),
    surface = FoscalLightSurface,
    onSurface = Color(0xFF1F2328),
    surfaceVariant = FoscalLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF7D8793),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = FoscalLightSurfaceVariant,
    surfaceContainer = Color(0xFFEAEFF5),
    surfaceContainerHigh = Color(0xFFE5EBF2),
    outline = Color(0xFFC8CED8),
    outlineVariant = FoscalLightOutline,
    error = Color(0xFFE53935),
)

private fun darkColorsFor(accent: AccentTokens) = darkColorScheme(
    primary = accent.primaryDark,
    onPrimary = accent.onPrimaryDark,
    primaryContainer = accent.primaryContainerDark,
    onPrimaryContainer = accent.onPrimaryContainerDark,
    secondary = accent.secondaryDark,
    onSecondary = accent.onPrimaryDark,
    secondaryContainer = accent.secondaryContainerDark,
    onSecondaryContainer = accent.onSecondaryContainerDark,
    background = FoscalDarkBackground,
    onBackground = Color(0xFFE7EAEE),
    surface = FoscalDarkSurface,
    onSurface = Color(0xFFE7EAEE),
    surfaceVariant = FoscalDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA9B1BC),
    surfaceContainerLowest = Color(0xFF111419),
    surfaceContainerLow = FoscalDarkSurfaceVariant,
    surfaceContainer = Color(0xFF262B33),
    surfaceContainerHigh = Color(0xFF2E343D),
    outline = Color(0xFF59616C),
    outlineVariant = FoscalDarkOutline,
    error = Color(0xFFFF6B66),
)

/**
 * Whether the app is currently rendering dark. Reads the resolved theme — which the user can force
 * to Light or Dark in Settings — where [isSystemInDarkTheme] would only ever report the OS setting
 * and so disagree with the rest of the UI. Anything outside this file that needs to branch on
 * light/dark must read this, never [isSystemInDarkTheme].
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun FoscalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    uiColor: UiColor = UiColor.Default,
    customSeed: Color = Color(UiColor.DEFAULT_CUSTOM_COLOR),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Three answers rather than a palette: the wallpaper's, Foscal's own, or one the user picked.
    // Whatever the choice, `CobaltAccent` is the floor — a stored SYSTEM on a phone below Android
    // 12 has no dynamic scheme to read, and falling back is better than refusing to draw.
    val tokens = if (uiColor == UiColor.CUSTOM) customAccentTokens(customSeed) else CobaltAccent
    val colorScheme = when {
        uiColor == UiColor.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkColorsFor(tokens)
        else -> lightColorsFor(tokens)
    }
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FoscalTypography,
            content = content,
        )
    }
}



/**
 * Derives a full accent from a single [seed] colour by blending it toward white and black, so any
 * colour the user picks yields a coherent light/dark palette without hand-tuning each token.
 */
fun customAccentTokens(seed: Color): AccentTokens = AccentTokens(
    primaryLight = seed,
    primaryContainerLight = lerp(seed, Color.White, 0.86f),
    onPrimaryContainerLight = lerp(seed, Color.Black, 0.62f),
    primaryDark = lerp(seed, Color.White, 0.55f),
    onPrimaryDark = lerp(seed, Color.Black, 0.82f),
    primaryContainerDark = lerp(seed, Color.Black, 0.58f),
    onPrimaryContainerDark = lerp(seed, Color.White, 0.80f),
    // A step off the seed rather than a second hue: there is no way to choose a companion colour
    // on the user's behalf from a single one that would not be a guess. Lighter and quieter is the
    // safe reading of "secondary".
    secondaryLight = lerp(seed, Color.Black, 0.24f),
    secondaryContainerLight = lerp(seed, Color.White, 0.80f),
    onSecondaryContainerLight = lerp(seed, Color.Black, 0.70f),
    secondaryDark = lerp(seed, Color.White, 0.42f),
    secondaryContainerDark = lerp(seed, Color.Black, 0.66f),
    onSecondaryContainerDark = lerp(seed, Color.White, 0.72f),
)

/** Weekend day-of-week label colour: the accent, against the neutral of the other five. */
@Composable
fun weekendLabelColor(): Color = MaterialTheme.colorScheme.primary

/**
 * The filled disc marking today, and the ink on it.
 *
 * Whichever accent is in force. This was the brand amber, on the reasoning that it made the
 * launcher icon a preview of the app; that reasoning does not survive the user choosing a colour,
 * because the one day always on screen was then the one thing their choice did not reach. Today
 * and a tapped day are told apart by shape now, not hue: a filled disc against a tinted cell.
 */
@Composable
fun todayDiscColor(): Color = MaterialTheme.colorScheme.primary

@Composable
fun onTodayDiscColor(): Color = MaterialTheme.colorScheme.onPrimary
