package app.foscal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import app.foscal.core.ui.theme.LocalIsDarkTheme

/** The two colours an event is drawn in, derived once from its calendar's colour. */
data class EventColors(
    /** The block's fill: the event's colour, blended down into the surface behind it. */
    val container: Color,
    /** Its text, and anything else that has to be read against [container]. */
    val content: Color,
    /** The event's colour at full strength, for the stripe that identifies it. */
    val accent: Color,
)

/**
 * How an event should be painted, given its calendar's colour.
 *
 * Blended into the surface rather than drawn at an alpha, because alpha does not survive both
 * themes: ten percent of a colour over near-black is invisible, and the same ten percent over
 * white washes out to nothing. Blending keeps the hue at a usable weight either way.
 *
 * The text is taken from the event's own hue rather than plain `onSurface`, so a block reads as
 * belonging to its calendar instead of as grey text on a faint tint — and it is pushed toward
 * black or white until it actually clears 4.5:1 against the container it sits on, because a
 * calendar's colour is whatever the user or their server chose and some of those are pale yellow.
 */
@Composable
fun eventColors(eventColorArgb: Int): EventColors {
    val dark = LocalIsDarkTheme.current
    val surface = MaterialTheme.colorScheme.surface
    return remember(eventColorArgb, dark, surface) {
        val accent = Color(eventColorArgb)
        val container = lerp(surface, accent, if (dark) 0.28f else 0.20f)
        EventColors(
            container = container,
            content = readableOn(container, accent, toward = if (dark) Color.White else Color.Black),
            accent = accent,
        )
    }
}

/** [color] pushed [toward] black or white in small steps until it clears 4.5:1 against [on]. */
private fun readableOn(on: Color, color: Color, toward: Color): Color {
    var candidate = color
    var step = 0
    while (contrastRatio(on, candidate) < 4.5f && step < 20) {
        step++
        candidate = lerp(color, toward, step * 0.05f)
    }
    return candidate
}

private fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}
