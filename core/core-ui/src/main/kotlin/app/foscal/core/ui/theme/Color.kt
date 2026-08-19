package app.foscal.core.ui.theme

import androidx.compose.ui.graphics.Color

// Cobalt — the default accent (drives today, selection, buttons and the FAB).
// The same blue the launcher icon is built from, so the app a user opens looks like the icon they
// tapped. Every pair below clears 4.5:1 against what it is drawn on.
val FoscalBlue = Color(0xFF4355F4)
val FoscalBlueDark = Color(0xFFAAB2FA)

/**
 * Tokens for one accent preset. The rest of the UI reads only `colorScheme.primary` and the
 * primary-container pair, so these five colors fully define an accent in both light and dark.
 */
data class AccentTokens(
    val primaryLight: Color,
    val primaryContainerLight: Color,
    val onPrimaryContainerLight: Color,
    val primaryDark: Color,
    val onPrimaryDark: Color,
    val primaryContainerDark: Color,
    val onPrimaryContainerDark: Color,
)

val CobaltAccent = AccentTokens(
    primaryLight = FoscalBlue,
    primaryContainerLight = Color(0xFFE8EBFE),
    onPrimaryContainerLight = Color(0xFF19205D),
    primaryDark = FoscalBlueDark,
    onPrimaryDark = Color(0xFF080A1D),
    primaryContainerDark = Color(0xFF202975),
    onPrimaryContainerDark = Color(0xFFE1E4FD),
)

val VioletAccent = AccentTokens(
    primaryLight = Color(0xFF6E45E2),
    primaryContainerLight = Color(0xFFEDE7FF),
    onPrimaryContainerLight = Color(0xFF2C1470),
    primaryDark = Color(0xFFC3B0FF),
    onPrimaryDark = Color(0xFF17093B),
    primaryContainerDark = Color(0xFF39236E),
    onPrimaryContainerDark = Color(0xFFE7DEFF),
)

val ForestAccent = AccentTokens(
    primaryLight = Color(0xFF1E8E5A),
    primaryContainerLight = Color(0xFFD8F2E2),
    onPrimaryContainerLight = Color(0xFF06331E),
    primaryDark = Color(0xFF77D6A2),
    onPrimaryDark = Color(0xFF032013),
    primaryContainerDark = Color(0xFF145033),
    onPrimaryContainerDark = Color(0xFFC9F3D8),
)

// Weekend day-of-week labels. Muted gold in light; a lighter amber that survives the dark
// background instead of the near-black gold used before.
val WeekendGoldLight = Color(0xFFB9A24B)
val WeekendGoldDark = Color(0xFFCBB667)

val FoscalLightBackground = Color(0xFFF4F6FA)
val FoscalLightSurface = Color(0xFFFFFFFF)
val FoscalLightSurfaceVariant = Color(0xFFF0F3F7)
val FoscalLightOutline = Color(0xFFE2E6EC)

val FoscalDarkBackground = Color(0xFF101318)
val FoscalDarkSurface = Color(0xFF191C21)
val FoscalDarkSurfaceVariant = Color(0xFF22272E)
val FoscalDarkOutline = Color(0xFF343A44)
