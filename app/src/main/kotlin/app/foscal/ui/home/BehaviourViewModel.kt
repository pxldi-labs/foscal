package app.foscal.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.foscal.core.data.Preferences
import app.foscal.core.model.DayTapAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class BehaviourState(
    /** Empty means "whatever was open last". */
    val startView: String = "",
    val lastUsedView: String = "",
    val firstDayOfWeek: DayOfWeek = Preferences.DEFAULT_FIRST_DAY,
    val defaultEventMinutes: Int = Preferences.DEFAULT_EVENT_MINUTES,
    val showWeekNumbers: Boolean = false,
    val dayTapAction: DayTapAction = DayTapAction.Default,
    /** Events shorter than this are left out of the month grid. 0 shows everything. */
    val monthMinimumMinutes: Int = 0,
    /** Null means new events land on the first visible calendar. */
    val defaultCalendarId: Long? = null,
    /** Minutes before local midnight that a new all-day event reminds; null is none. */
    val allDayReminderMinutes: Int? = Preferences.DEFAULT_ALL_DAY_REMINDER_MINUTES,
    /** Whether events the user has declined still take up space on the grid. */
    val showDeclinedEvents: Boolean = true,
    val widgetEventLimit: Int = Preferences.DEFAULT_WIDGET_EVENT_LIMIT,
    val widgetDetailedRows: Boolean = true,
    val suggestEventTitles: Boolean = true,
) {
    /**
     * The view to actually open on.
     *
     * Falls through pinned view, then last used, then Month. Names that no longer match a view —
     * from an older install, or a view we renamed — fall through the same way rather than failing,
     * so the worst outcome of a stale preference is landing on the default screen.
     */
    val resolvedStartView: CalendarView
        get() = named(startView) ?: named(lastUsedView) ?: CalendarView.Month

    private fun named(name: String): CalendarView? =
        CalendarView.entries.firstOrNull { it.name == name }
}

/** The preferences that decide how the calendar behaves, as opposed to how it looks. */
@HiltViewModel
class BehaviourViewModel @Inject constructor(
    private val prefs: Preferences,
) : ViewModel() {

    // combine() tops out at five arguments, so the rest are folded in through nested combines
    // and a holder rather than by splitting the screen's state across two view models.
    private data class Extras(
        val tap: DayTapAction,
        val monthMinimum: Int,
        val allDayReminder: Int?,
        val showDeclined: Boolean,
        val widgetLimit: Int,
        val widgetDetailed: Boolean,
        val suggestTitles: Boolean,
    )

    private val extras: Flow<Extras> = combine(
        combine(
            prefs.dayTapAction,
            prefs.monthMinimumMinutes,
            prefs.allDayReminderMinutes,
        ) { tap, monthMinimum, allDay -> Triple(tap, monthMinimum, allDay) },
        prefs.showDeclinedEvents,
        prefs.widgetEventLimit,
        prefs.widgetDetailedRows,
        prefs.suggestEventTitles,
    ) { core, declined, widgetLimit, widgetDetailed, suggestTitles ->
        Extras(
            tap = core.first,
            monthMinimum = core.second,
            allDayReminder = core.third,
            showDeclined = declined,
            widgetLimit = widgetLimit,
            widgetDetailed = widgetDetailed,
            suggestTitles = suggestTitles,
        )
    }

    val state: StateFlow<BehaviourState> = combine(
        combine(
            prefs.startView,
            prefs.lastUsedView,
            prefs.defaultCalendarId,
        ) { start, last, calendar -> Triple(start, last, calendar) },
        prefs.firstDayOfWeek,
        prefs.defaultEventMinutes,
        prefs.showWeekNumbers,
        extras,
    ) { views, firstDay, minutes, weekNumbers, rest ->
        BehaviourState(
            startView = views.first,
            lastUsedView = views.second,
            defaultCalendarId = views.third,
            firstDayOfWeek = firstDay,
            defaultEventMinutes = minutes,
            showWeekNumbers = weekNumbers,
            dayTapAction = rest.tap,
            monthMinimumMinutes = rest.monthMinimum,
            allDayReminderMinutes = rest.allDayReminder,
            showDeclinedEvents = rest.showDeclined,
            widgetEventLimit = rest.widgetLimit,
            widgetDetailedRows = rest.widgetDetailed,
            suggestEventTitles = rest.suggestTitles,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BehaviourState())

    fun setAllDayReminder(minutes: Int?) {
        viewModelScope.launch { prefs.setAllDayReminder(minutes) }
    }

    fun setShowDeclinedEvents(enabled: Boolean) {
        viewModelScope.launch { prefs.setShowDeclinedEvents(enabled) }
    }

    fun setWidgetEventLimit(limit: Int) {
        viewModelScope.launch { prefs.setWidgetEventLimit(limit) }
    }

    fun setWidgetDetailedRows(enabled: Boolean) {
        viewModelScope.launch { prefs.setWidgetDetailedRows(enabled) }
    }

    fun setSuggestEventTitles(enabled: Boolean) {
        viewModelScope.launch { prefs.setSuggestEventTitles(enabled) }
    }

    /**
     * Records the view on screen so "Last used" has something to resolve to.
     *
     * Written on every switch rather than on the way out: a calendar app is usually killed in the
     * background rather than closed, so there is no reliable later moment to save it.
     */
    fun rememberView(name: String) {
        if (name == state.value.lastUsedView) return
        viewModelScope.launch { prefs.setLastUsedView(name) }
    }

    fun setStartView(name: String) {
        viewModelScope.launch { prefs.setStartView(name) }
    }

    fun setFirstDayOfWeek(day: DayOfWeek) {
        viewModelScope.launch { prefs.setFirstDayOfWeek(day) }
    }

    fun setDefaultEventMinutes(minutes: Int) {
        viewModelScope.launch { prefs.setDefaultEventMinutes(minutes) }
    }

    fun setShowWeekNumbers(enabled: Boolean) {
        viewModelScope.launch { prefs.setShowWeekNumbers(enabled) }
    }

    fun setMonthMinimumMinutes(minutes: Int) {
        viewModelScope.launch { prefs.setMonthMinimumMinutes(minutes) }
    }

    fun setDayTapAction(action: DayTapAction) {
        viewModelScope.launch { prefs.setDayTapAction(action) }
    }

    fun setDefaultCalendarId(id: Long?) {
        viewModelScope.launch { prefs.setDefaultCalendarId(id) }
    }
}
