package app.foscal.core.ui.theme

import androidx.compose.ui.graphics.Color

// Cobalt — the default accent (drives today, selection, buttons and the FAB).
// The same blue the launcher icon is built from, so the app a user opens looks like the icon they
// tapped. Every pair below clears 4.5:1 against what it is drawn on.
val FoscalBlue = Color(0xFF4355F4)
val FoscalBlueDark = Color(0xFFAAB2FA)

/** The icon's second blue. Cobalt's secondary, so the palette that made the mark runs on. */
val FoscalPeriwinkle = Color(0xFF8691F7)

/**
 * Tokens for one accent preset, in both light and dark.
 *
 * Two roles, not one. The UI reads `primary` for the things it wants you to look at — today, the
 * FAB, a selection — and `secondaryContainer` for the things that are simply there, like the view
 * switcher and the Edit button. Those used to be handed the primary pair as well, which made every
 * tinted surface in the app the same blue and left the second colour of the palette unused.
 *
 * There is deliberately no tertiary: nothing in the app reads one, and a colour nothing draws is
 * not a palette, it is a comment.
 */
data class AccentTokens(
    val primaryLight: Color,
    val primaryContainerLight: Color,
    val onPrimaryContainerLight: Color,
    val primaryDark: Color,
    val onPrimaryDark: Color,
    val primaryContainerDark: Color,
    val onPrimaryContainerDark: Color,
    val secondaryLight: Color,
    val secondaryContainerLight: Color,
    val onSecondaryContainerLight: Color,
    val secondaryDark: Color,
    val secondaryContainerDark: Color,
    val onSecondaryContainerDark: Color,
)

val CobaltAccent = AccentTokens(
    primaryLight = FoscalBlue,
    primaryContainerLight = Color(0xFFE8EBFE),
    onPrimaryContainerLight = Color(0xFF19205D),
    primaryDark = FoscalBlueDark,
    onPrimaryDark = Color(0xFF080A1D),
    primaryContainerDark = Color(0xFF202975),
    onPrimaryContainerDark = Color(0xFFE1E4FD),
    // The periwinkle, darkened until it clears 4.5:1 as text on white.
    secondaryLight = Color(0xFF666EBC),
    secondaryContainerLight = Color(0xFFE0E2FD),
    onSecondaryContainerLight = Color(0xFF262945),
    secondaryDark = FoscalPeriwinkle,
    secondaryContainerDark = Color(0xFF2E3154),
    onSecondaryContainerDark = Color(0xFFDDE0FD),
)



// The brand amber lives in the launcher icon and the badge drawable, which are the app's mark and
// not its chrome. It used to mark today and label weekends here too, which meant two pieces of the
// UI ignored the colour the user chose; both now read from the accent.

val FoscalLightBackground = Color(0xFFF4F6FA)
val FoscalLightSurface = Color(0xFFFFFFFF)
val FoscalLightSurfaceVariant = Color(0xFFF0F3F7)
val FoscalLightOutline = Color(0xFFE2E6EC)

val FoscalDarkBackground = Color(0xFF101318)
val FoscalDarkSurface = Color(0xFF191C21)
val FoscalDarkSurfaceVariant = Color(0xFF22272E)
val FoscalDarkOutline = Color(0xFF343A44)
