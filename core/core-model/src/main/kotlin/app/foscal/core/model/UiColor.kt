package app.foscal.core.model

/**
 * Where the app's own chrome gets its colour from.
 *
 * Three answers, not a palette: the calendars and their events are what colour *means* in a
 * calendar app, and a shelf of preset hues for the UI only competes with them. [SYSTEM] leaves the
 * question to the wallpaper, [FOSCAL] is the app's own blue, and [CUSTOM] is one colour the user
 * picked and can change.
 *
 * [key] is the stable string persisted in preferences — do not rename existing values.
 */
enum class UiColor(val key: String) {
    /** Material You, derived from the wallpaper. Needs Android 12; below that it reads as [FOSCAL]. */
    SYSTEM("system"),

    /** Foscal's own cobalt, which is also the fallback whenever [SYSTEM] has nothing to read. */
    FOSCAL("foscal"),

    /** A colour the user chose; its ARGB value is stored separately. */
    CUSTOM("custom"),
    ;

    companion object {
        val Default = SYSTEM

        /** The seed [CUSTOM] starts from until the user picks one — Foscal's own blue. */
        const val DEFAULT_CUSTOM_COLOR: Int = 0xFF4355F4.toInt()

        fun fromKey(key: String?): UiColor = entries.firstOrNull { it.key == key } ?: Default
    }
}
