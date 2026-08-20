package app.foscal.notifications

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Beyond a week out, the weekday alone stops being enough to place a date. */
private const val NamedWeekdayDays = 7L

/**
 * When the event is, in one phrase.
 *
 * The clock time, the way every other calendar's reminders do it: "21:30 – 22:00" is the fact you
 * act on, and it stays true however late the alarm arrives. The lead time was tried here first and
 * read badly at exactly the moment it mattered — a reminder that says "Now" tells you nothing you
 * did not already know from your phone buzzing.
 *
 * The day is named only when the event is not today, because a reminder that fires for something
 * hours away should say which day it means; a same-day one would just be repeating itself.
 *
 * [startMillis] and [endMillis] are when the event runs as the reader understands it: local
 * midnight for an all-day event, not the UTC midnight the provider stores. [nowMillis] decides
 * only whether the day needs naming.
 */
internal fun reminderWhen(
    startMillis: Long,
    endMillis: Long,
    nowMillis: Long,
    allDay: Boolean,
    use24Hour: Boolean,
    zone: ZoneId,
    locale: Locale,
): String? {
    if (startMillis <= 0L) return null
    val start = Instant.ofEpochMilli(startMillis).atZone(zone)
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
    val day = dayLabel(start.toLocalDate(), now.toLocalDate(), locale)
    if (allDay) return day

    val format = DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", locale)
    val from = start.format(format)
    // An end the provider could not supply is left off rather than guessed at; the start alone is
    // still the answer to "when", just less of it.
    val span = if (endMillis > startMillis) {
        "$from \u2013 ${Instant.ofEpochMilli(endMillis).atZone(zone).format(format)}"
    } else {
        from
    }
    return if (start.toLocalDate() == now.toLocalDate()) span else "$day, $span"
}

private fun dayLabel(date: LocalDate, today: LocalDate, locale: Locale): String {
    val away = Duration.between(today.atStartOfDay(), date.atStartOfDay()).toDays()
    return when {
        away == 0L -> "Today"
        away == 1L -> "Tomorrow"
        away == -1L -> "Yesterday"
        away in 2L..NamedWeekdayDays ->
            date.format(DateTimeFormatter.ofPattern("EEEE", locale))
        else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))
    }
}
