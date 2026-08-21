package app.foscal.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import app.foscal.core.ui.R

// Both faces ship as variable fonts, so each weight is a variation instance of the same file
// rather than a separate resource. FontVariation applies on API 26+, which matches our minSdk.
//
// Both are SIL Open Font License 1.1; the licence text travels in the APK alongside them, at
// `core-ui/src/main/assets/licenses/`, because the OFL requires it to.
@OptIn(ExperimentalTextApi::class)
private fun gabarito(weight: FontWeight) = Font(
    resId = R.font.gabarito_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: FontWeight) = Font(
    resId = R.font.manrope_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/**
 * Gabarito — the display face. Used sparingly for the app's "voice": date numerals, the month
 * header, screen titles, and the event-detail title.
 *
 * Geometric rather than characterful, which is the point: the previous display face put its
 * personality into the numerals, and the numerals are the part of a calendar you read forty
 * times a day. Gabarito's weight axis starts at Regular, so `Normal` is its lightest instance.
 */
val DisplayFamily = FontFamily(
    gabarito(FontWeight.Normal),
    gabarito(FontWeight.Medium),
    gabarito(FontWeight.SemiBold),
    gabarito(FontWeight.Bold),
    gabarito(FontWeight.ExtraBold),
)

/** Manrope — the workhorse UI/body face. */
val UiFamily = FontFamily(
    manrope(FontWeight.Normal),
    manrope(FontWeight.Medium),
    manrope(FontWeight.SemiBold),
    manrope(FontWeight.Bold),
    manrope(FontWeight.ExtraBold),
)

/**
 * Display + headline styles carry the Gabarito voice; title/body/label default to Manrope.
 * Individual composables opt into [DisplayFamily] where a numeral or title should stand out.
 */
val FoscalTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em),
        displayMedium = displayMedium.copy(fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em),
        displaySmall = displaySmall.copy(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em),
        headlineLarge = headlineLarge.copy(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em),
        headlineMedium = headlineMedium.copy(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = UiFamily, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontFamily = UiFamily),
        titleSmall = titleSmall.copy(fontFamily = UiFamily),
        bodyLarge = bodyLarge.copy(fontFamily = UiFamily),
        bodyMedium = bodyMedium.copy(fontFamily = UiFamily),
        bodySmall = bodySmall.copy(fontFamily = UiFamily),
        labelLarge = labelLarge.copy(fontFamily = UiFamily, fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontFamily = UiFamily, fontWeight = FontWeight.SemiBold),
        labelSmall = labelSmall.copy(fontFamily = UiFamily, fontWeight = FontWeight.SemiBold),
    )
}
