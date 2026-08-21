package app.foscal.core.data

import app.foscal.core.model.DayTapAction
import app.foscal.core.model.EventColorStrength
import app.foscal.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.DayOfWeek

/** In-memory [Preferences] for view-model tests. */
class FakePreferences(
    onboardingDone: Boolean = true,
    hidden: Set<String> = emptySet(),
    monthHidden: Set<String> = emptySet(),
    monthMinimum: Int = 0,
    private val defaultReminder: Int? = 15,
    calendarReminders: Map<Long, Int?> = emptyMap(),
    theme: ThemeMode = ThemeMode.SYSTEM,
    use24Hour: Boolean = true,
    osmMaps: Boolean = false,
    firstDay: DayOfWeek = Preferences.DEFAULT_FIRST_DAY,
    eventMinutes: Int = Preferences.DEFAULT_EVENT_MINUTES,
) : Preferences {

    override val onboardingCompleted: Flow<Boolean> = MutableStateFlow(onboardingDone)
    override val hiddenCalendarIds: Flow<Set<String>> = MutableStateFlow(hidden)
    override val monthHiddenCalendarIds: Flow<Set<String>> = MutableStateFlow(monthHidden)
    override val monthMinimumMinutes: Flow<Int> = MutableStateFlow(monthMinimum)
    override val defaultReminderMinutes: Flow<Int?> = MutableStateFlow(defaultReminder)
    override val calendarReminderDefaults: MutableStateFlow<Map<Long, Int?>> =
        MutableStateFlow(calendarReminders)
    override val dynamicColor: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val themeMode: MutableStateFlow<ThemeMode> = MutableStateFlow(theme)
    override val use24HourClock: MutableStateFlow<Boolean> = MutableStateFlow(use24Hour)
    override val osmMapsEnabled: MutableStateFlow<Boolean> = MutableStateFlow(osmMaps)
    override val startView: MutableStateFlow<String> = MutableStateFlow("")
    override val lastUsedView: MutableStateFlow<String> = MutableStateFlow("")
    override val firstDayOfWeek: MutableStateFlow<DayOfWeek> = MutableStateFlow(firstDay)
    override val defaultEventMinutes: MutableStateFlow<Int> = MutableStateFlow(eventMinutes)
    override val showWeekNumbers: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val allDayReminderMinutes: MutableStateFlow<Int?> =
        MutableStateFlow(Preferences.DEFAULT_ALL_DAY_REMINDER_MINUTES)
    override val showDeclinedEvents: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val widgetEventLimit: MutableStateFlow<Int> =
        MutableStateFlow(Preferences.DEFAULT_WIDGET_EVENT_LIMIT)
    override val widgetDetailedRows: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val suggestEventTitles: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val dayTapAction: MutableStateFlow<DayTapAction> =
        MutableStateFlow(DayTapAction.Default)

    override val eventColorStrength: MutableStateFlow<EventColorStrength> =
        MutableStateFlow(EventColorStrength.Default)
    override val eventTextScalePercent: MutableStateFlow<Int> =
        MutableStateFlow(Preferences.DEFAULT_EVENT_TEXT_SCALE)
    override val wrapEventTitles: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override val defaultCalendarId: MutableStateFlow<Long?> = MutableStateFlow(null)

    override suspend fun setDefaultCalendarId(id: Long?) { defaultCalendarId.value = id }

    override suspend fun setStartView(view: String) { startView.value = view }
    override suspend fun setLastUsedView(view: String) { lastUsedView.value = view }
    override suspend fun setFirstDayOfWeek(day: DayOfWeek) { firstDayOfWeek.value = day }
    override suspend fun setDefaultEventMinutes(minutes: Int) { defaultEventMinutes.value = minutes }
    override suspend fun setShowWeekNumbers(enabled: Boolean) { showWeekNumbers.value = enabled }
    override suspend fun setAllDayReminder(minutes: Int?) { allDayReminderMinutes.value = minutes }
    override suspend fun setShowDeclinedEvents(enabled: Boolean) { showDeclinedEvents.value = enabled }
    override suspend fun setWidgetEventLimit(limit: Int) { widgetEventLimit.value = limit }
    override suspend fun setWidgetDetailedRows(enabled: Boolean) { widgetDetailedRows.value = enabled }
    override suspend fun setSuggestEventTitles(enabled: Boolean) { suggestEventTitles.value = enabled }
    override suspend fun setDayTapAction(action: DayTapAction) { dayTapAction.value = action }

    override suspend fun setEventColorStrength(strength: EventColorStrength) {
        eventColorStrength.value = strength
    }

    override suspend fun setEventTextScalePercent(percent: Int) {
        eventTextScalePercent.value = percent
    }

    override suspend fun setWrapEventTitles(wrap: Boolean) { wrapEventTitles.value = wrap }

    override suspend fun setOnboardingCompleted() = Unit
    override suspend fun setHiddenCalendars(ids: Set<String>) = Unit
    override suspend fun setMonthHiddenCalendars(ids: Set<String>) = Unit
    override suspend fun setMonthMinimumMinutes(minutes: Int) = Unit
    override suspend fun setDefaultReminder(minutes: Int?) = Unit

    override suspend fun setCalendarReminderDefault(calendarId: Long, minutes: Int?) {
        calendarReminderDefaults.value += (calendarId to minutes)
    }

    override suspend fun clearCalendarReminderDefault(calendarId: Long) {
        calendarReminderDefaults.value -= calendarId
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dynamicColor.value = enabled
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }

    override suspend fun setUse24HourClock(use24Hour: Boolean) {
        use24HourClock.value = use24Hour
    }

    override suspend fun setOsmMapsEnabled(enabled: Boolean) {
        osmMapsEnabled.value = enabled
    }
}
