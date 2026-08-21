package app.foscal.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object CalendarColors {
    /** The palette offered when the user picks a colour for a calendar they are creating. */
    val presets = listOf(
        0xFF1976D2.toInt(),
        0xFFD81B60.toInt(),
        0xFF43A047.toInt(),
        0xFFFB8C00.toInt(),
        0xFF8E24AA.toInt(),
        0xFF00897B.toInt(),
        0xFFE53935.toInt(),
        0xFF6D4C41.toInt(),
    )

    fun pick(index: Int): Int = presets[index % presets.size]

    /**
     * What each preset is called.
     *
     * A swatch you can only point at is a colour you cannot talk about, or check at a glance when
     * two of them are close. Named after the thing rather than the hue ("Poppy", not "Red 600"),
     * which is how people describe a colour to each other.
     */
    private val names = mapOf(
        presets[0] to "Cobalt",
        presets[1] to "Cerise",
        presets[2] to "Fern",
        presets[3] to "Tangerine",
        presets[4] to "Amethyst",
        presets[5] to "Teal",
        presets[6] to "Poppy",
        presets[7] to "Cocoa",
    )

    /**
     * The name of [colorArgb], or null when it is not one of ours.
     *
     * A colour synced down from a server is whatever that server chose, and inventing a name for it
     * would be a guess presented as a fact.
     */
    fun nameOf(colorArgb: Int): String? = names[colorArgb]
}

fun Int.toComposeColor(): Color = Color(this)

fun contrastColor(bg: Int): Color {
    val r = (bg shr 16) and 0xFF
    val g = (bg shr 8) and 0xFF
    val b = bg and 0xFF
    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    return if (luminance > 0.55) Color.Black else Color.White
}
