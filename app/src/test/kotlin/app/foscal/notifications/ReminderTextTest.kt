package app.foscal.notifications

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTextTest {

    private val zone: ZoneId = ZoneId.of("Europe/Berlin")
    private val locale: Locale = Locale.UK
    private val now: LocalDateTime = LocalDateTime.of(2026, 8, 18, 11, 15)

    private fun millis(at: LocalDateTime): Long =
        at.atZone(zone).toInstant().toEpochMilli()

    private fun label(
        start: LocalDateTime,
        end: LocalDateTime? = null,
        allDay: Boolean = false,
        use24Hour: Boolean = true,
    ): String? = reminderWhen(
        startMillis = millis(start),
        endMillis = end?.let { millis(it) } ?: 0L,
        nowMillis = millis(now),
        allDay = allDay,
        use24Hour = use24Hour,
        zone = zone,
        locale = locale,
    )

    @Test
    fun `an event today is just its clock time`() {
        assertEquals(
            "21:30 – 22:00",
            label(now.with(LocalTime.of(21, 30)), now.with(LocalTime.of(22, 0))),
        )
    }

    @Test
    fun `the time is the time however close the event is`() {
        // The lead time used to take over within the hour, which meant the reminder that mattered
        // most was the one that said "Now".
        assertEquals("11:15 – 12:00", label(now, now.with(LocalTime.of(12, 0))))
        assertEquals("11:30 – 12:00", label(now.plusMinutes(15), now.with(LocalTime.of(12, 0))))
    }

    @Test
    fun `another day is named before its time`() {
        assertEquals(
            "Tomorrow, 09:00 – 10:00",
            label(now.plusDays(1).with(LocalTime.of(9, 0)), now.plusDays(1).with(LocalTime.of(10, 0))),
        )
        assertEquals(
            "Friday, 09:00 – 10:00",
            label(now.plusDays(3).with(LocalTime.of(9, 0)), now.plusDays(3).with(LocalTime.of(10, 0))),
        )
    }

    @Test
    fun `beyond a week the weekday alone no longer places it`() {
        assertEquals(
            "Tue, Sept 1, 09:00 – 10:00",
            label(now.plusDays(14).with(LocalTime.of(9, 0)), now.plusDays(14).with(LocalTime.of(10, 0))),
        )
    }

    @Test
    fun `a late alarm still says when the thing was`() {
        assertEquals(
            "Yesterday, 09:00 – 10:00",
            label(now.minusDays(1).with(LocalTime.of(9, 0)), now.minusDays(1).with(LocalTime.of(10, 0))),
        )
    }

    @Test
    fun `an end the provider could not supply is left off`() {
        assertEquals("14:00", label(now.with(LocalTime.of(14, 0))))
        // An end at or before the start is no end at all.
        assertEquals("14:00", label(now.with(LocalTime.of(14, 0)), now.with(LocalTime.of(14, 0))))
    }

    @Test
    fun `an all-day event has no clock time to show`() {
        assertEquals("Today", label(now.with(LocalTime.MIDNIGHT), allDay = true))
        assertEquals("Tomorrow", label(now.plusDays(1).with(LocalTime.MIDNIGHT), allDay = true))
        assertEquals(
            "Tue, Sept 1",
            label(LocalDate.of(2026, 9, 1).atStartOfDay(), allDay = true),
        )
    }

    @Test
    fun `twelve-hour clocks get twelve-hour times`() {
        assertEquals(
            "Tomorrow, 9:00 am – 10:00 am",
            label(
                now.plusDays(1).with(LocalTime.of(9, 0)),
                now.plusDays(1).with(LocalTime.of(10, 0)),
                use24Hour = false,
            ),
        )
    }

    @Test
    fun `an event with no start has nothing to say`() {
        assertNull(reminderWhen(0L, 0L, millis(now), false, true, zone, locale))
    }
}
