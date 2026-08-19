package app.foscal.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.foscal.core.model.Event
import app.foscal.core.ui.theme.LocalIsDarkTheme
import app.foscal.ui.eventColors
import app.foscal.ui.util.LocalUse24HourClock
import app.foscal.ui.util.currentLocale
import app.foscal.ui.util.timeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor

/**
 * Width of the hour-label gutter and the inset at the far edge of the grid. Any header rendered
 * above a [TimelineLayout] must use the same two values, or its weekday columns drift out of
 * alignment with the grid columns underneath — the drift accumulates across the week and is most
 * visible on the last day.
 */
val TimelineGutterWidth = 54.dp

/**
 * Taken off the bottom of every block so touching events keep a visible seam.
 *
 * Off the drawn height rather than out of the layout: the block still owns its slot, so the grid
 * and the times stay honest and only the paint stops short.
 */
private val BlockGap = 3.dp
val TimelineEndInset = 4.dp

/** Grid hour to open on when no timed event and no "now" marker gives a better anchor. */
private const val DEFAULT_ANCHOR_HOUR = 8

/** Context kept above the anchor so the marker or first event isn't flush against the top edge. */
private const val ANCHOR_LEAD_IN_HOURS = 1

/**
 * Hour the grid should be scrolled to for [days]: the current hour when today is on screen,
 * otherwise the first timed event of the shown days, otherwise [DEFAULT_ANCHOR_HOUR].
 *
 * [days] must already be filtered to timed events — all-day events live in their own header and
 * carry no meaningful hour.
 */
internal fun anchorHour(
    days: List<TimelineDay>,
    today: LocalDate,
    now: Instant,
    zone: ZoneId,
): Int {
    val focus = if (days.any { it.date == today }) {
        now.atZone(zone).hour
    } else {
        val shownDates = days.map { it.date }.toSet()
        days.asSequence()
            .flatMap { it.events }
            .map { it.start.atZone(zone) }
            // A multi-day event starting before this view begins would otherwise drag the anchor
            // back to its original start hour on an unrelated day.
            .filter { it.toLocalDate() in shownDates }
            .minOfOrNull { it.hour }
            ?: DEFAULT_ANCHOR_HOUR
    }
    return (focus - ANCHOR_LEAD_IN_HOURS).coerceIn(0, 23)
}

data class TimelineDay(
    val date: LocalDate,
    val events: List<Event>,
)

/**
 * Hour-grid timeline with positioned event blocks.
 *
 * @param compact When true (week view), event blocks use only the title and tight
 *   padding. When false (day view), blocks show title + time + optional location with
 *   relaxed padding and an accent stripe.
 */
@Composable
fun TimelineLayout(
    days: List<TimelineDay>,
    onEventClick: (eventId: Long, instanceStartMillis: Long) -> Unit,
    modifier: Modifier = Modifier,
    hourHeight: Dp = 60.dp,
    compact: Boolean = false,
    // Remembered, not a bare Instant.now(): as a default argument it would be re-evaluated on
    // every recomposition, restarting the marker ticker below before its delay ever elapsed.
    now: Instant = remember { Instant.now() },
    zone: ZoneId = ZoneId.systemDefault(),
    blockCornerRadius: Dp = if (compact) 5.dp else 7.dp,
    accentStripe: Boolean = !compact,
    onTimeRangeSelected: ((startMillis: Long, endMillis: Long) -> Unit)? = null,
    onEventMove: ((event: Event, newStartMillis: Long, newEndMillis: Long) -> Unit)? = null,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val totalHeight = hourHeight * 24
    // Advance the current-time marker while the view stays open instead of freezing it at the
    // instant this composable first ran.
    val liveNowState = remember(now) { mutableStateOf(now) }
    val liveNow by liveNowState
    LaunchedEffect(now) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            liveNowState.value = Instant.now()
        }
    }
    val today = LocalDate.now(zone)
    val nowZ = liveNow.atZone(zone)
    val nowFractionalHour = nowZ.hour + nowZ.minute / 60f
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    // The hour lines are a ruler, not a border: they only have to be findable when you look for
    // them. A solid outline colour turns the grid into the loudest thing on an empty day. Ink in
    // light, white in dark — a dark grey line on a dark background reads as dirt.
    val hourLineColor = if (LocalIsDarkTheme.current) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    val nowColor = MaterialTheme.colorScheme.error
    val todayTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    val allDayEvents = days.flatMap { day -> day.events.filter { it.allDay } }
    val timedDays = days.map { day -> day.copy(events = day.events.filter { !it.allDay }) }
    // Only worth tinting a whole column when several are shown side by side (week view); in day
    // view the single column fills the screen so a tint just muddies the background.
    val highlightTodayColumn = days.size > 1
    val showNowLabel = timedDays.any { it.date == today }
    val is24Hour = LocalUse24HourClock.current
    val locale = currentLocale()
    val nowLabelFmt = remember(is24Hour, locale) { timeFormatter(is24Hour, locale) }
    var selection by remember { mutableStateOf<TimeSelection?>(null) }
    var eventDrag by remember { mutableStateOf<EventDrag?>(null) }

    // Open on the part of the day the user cares about. A fixed early-morning offset means that
    // opening the app in the afternoon shows an empty grid with the next event scrolled off below.
    val anchorHour = anchorHour(timedDays, today, now, zone)
    // Which anchor has already been applied, saved rather than merely remembered. Opening an event
    // takes the grid out of composition, and a plain `remember` would forget on the way back and
    // re-anchor — throwing away the position the user had scrolled to, which `scrollState` itself
    // restores perfectly well.
    var anchoredAt by rememberSaveable { mutableIntStateOf(Int.MIN_VALUE) }
    LaunchedEffect(anchorHour) {
        if (anchoredAt == anchorHour) return@LaunchedEffect
        anchoredAt = anchorHour
        val targetPx = with(density) { (hourHeight * anchorHour).toPx() }.toInt()
        scrollState.scrollTo(targetPx.coerceAtLeast(0))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (allDayEvents.isNotEmpty()) {
            AllDayHeader(days = days, onEventClick = onEventClick)
            HorizontalDivider(
                modifier = Modifier.padding(start = TimelineGutterWidth, end = TimelineEndInset),
                color = gridColor,
                thickness = 0.5.dp,
            )
            Spacer(Modifier.height(4.dp))
        }

        Column(modifier = Modifier.verticalScroll(scrollState)) {
            Box {
                Row(
                    modifier = Modifier
                        .height(totalHeight)
                        .padding(end = TimelineEndInset),
                ) {
                    Column(Modifier.width(TimelineGutterWidth)) {
                        for (h in 0..23) {
                            Box(
                                Modifier
                                    .height(hourHeight)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                Text(
                                    // The whole time, not just the hour. "05" beside a grid line
                                    // is a label you have to decode; "05:00" is one you read.
                                    "${"%02d".format(h)}:00",
                                    modifier = Modifier.padding(end = 10.dp),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    timedDays.forEachIndexed { dayIndex, day ->
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(
                                    if (onTimeRangeSelected != null) {
                                        Modifier.pointerInput(day.date, hourHeight, onTimeRangeSelected) {
                                            fun minuteAt(y: Float): Int {
                                                val raw = (y / hourHeight.toPx() * 60f).toInt()
                                                return raw.roundToStep(15).coerceIn(0, 24 * 60)
                                            }

                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { offset ->
                                                    val minute = minuteAt(offset.y)
                                                    selection = TimeSelection(day.date, minute, minute)
                                                },
                                                onDrag = { change, _ ->
                                                    change.consume()
                                                    val start = selection ?: return@detectDragGesturesAfterLongPress
                                                    selection = start.copy(endMinute = minuteAt(change.position.y))
                                                },
                                                onDragCancel = { selection = null },
                                                onDragEnd = {
                                                    val finalSelection = selection
                                                    selection = null
                                                    if (finalSelection != null) {
                                                        val range = finalSelection.normalized()
                                                        val startMinute = range.first
                                                        val endMinute = when {
                                                            range.second > range.first -> range.second
                                                            range.first <= 23 * 60 -> range.first + 60
                                                            else -> 24 * 60
                                                        }
                                                        val start = finalSelection.date.atStartOfDay(zone)
                                                            .plusMinutes(startMinute.toLong())
                                                        val end = finalSelection.date.atStartOfDay(zone)
                                                            .plusMinutes(endMinute.toLong())
                                                        onTimeRangeSelected(
                                                            start.toInstant().toEpochMilli(),
                                                            end.toInstant().toEpochMilli(),
                                                        )
                                                    }
                                                },
                                            )
                                        }
                                    } else {
                                        Modifier
                                    },
                                )
                                .then(
                                    if (highlightTodayColumn && day.date == today) {
                                        Modifier.background(todayTint)
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            val colWidth = maxWidth
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val hourPx = hourHeight.toPx()
                                for (h in 1..23) {
                                    drawLine(
                                        color = hourLineColor,
                                        start = Offset(0f, h * hourPx),
                                        end = Offset(size.width, h * hourPx),
                                        strokeWidth = 0.5f,
                                    )
                                }
                            }
                            selection
                                ?.takeIf { it.date == day.date }
                                ?.let { current ->
                                    val range = current.normalized()
                                    val top = hourHeight * (range.first / 60f)
                                    val height = (hourHeight * ((range.second - range.first).coerceAtLeast(15) / 60f))
                                        .coerceAtLeast(18.dp)
                                    Box(
                                        modifier = Modifier
                                            .offset(y = top)
                                            .fillMaxWidth()
                                            .height(height)
                                            .padding(horizontal = 3.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                                    )
                                }
                            val positioned = remember(day.events, hourHeight, zone) {
                                layoutTimed(day.events, hourHeight, zone)
                            }
                            positioned.forEach { pe ->
                                // Fractions of the day column, not a column index: a nested
                                // event keeps its container's right edge and only gives up ground
                                // on the left.
                                val blockLeft = colWidth * pe.leftFraction + 1.dp
                                val blockWidth = colWidth * pe.widthFraction - 2.dp
                                val drag = eventDrag?.takeIf {
                                    it.eventId == pe.event.id &&
                                        it.instanceStartMillis == pe.event.start.toEpochMilli()
                                }
                                EventBlock(
                                    event = pe.event,
                                    zone = zone,
                                    heightDp = pe.heightDp,
                                    compact = compact,
                                    accentStripe = accentStripe,
                                    cornerRadius = blockCornerRadius,
                                    nested = pe.depth > 0,
                                    modifier = Modifier
                                        .offset(
                                            x = blockLeft + colWidth * (drag?.deltaDays ?: 0),
                                            y = pe.topDp + hourHeight * ((drag?.deltaMinutes ?: 0) / 60f),
                                        )
                                        .width(blockWidth)
                                        // A block is drawn slightly shorter than its slot, so two
                                        // events that touch in time still have a seam between them.
                                        // Without it 08:30-12:00 and 12:00-13:00 render as one
                                        // long shape and the boundary has to be inferred from the
                                        // titles.
                                        .height((pe.heightDp - BlockGap).coerceAtLeast(12.dp)),
                                    onClick = { onEventClick(pe.event.id, pe.event.start.toEpochMilli()) },
                                    onMove = onEventMove?.let { move ->
                                        { deltaDays, deltaMinutes ->
                                            val start = pe.event.start.atZone(zone)
                                                .plusDays(deltaDays.toLong())
                                                .plusMinutes(deltaMinutes.toLong())
                                            val end = pe.event.end.atZone(zone)
                                                .plusDays(deltaDays.toLong())
                                                .plusMinutes(deltaMinutes.toLong())
                                            move(
                                                pe.event,
                                                start.toInstant().toEpochMilli(),
                                                end.toInstant().toEpochMilli(),
                                            )
                                        }
                                    },
                                    onMovePreview = if (onEventMove != null) {
                                        { deltaDays, deltaMinutes ->
                                            eventDrag = EventDrag(
                                                eventId = pe.event.id,
                                                instanceStartMillis = pe.event.start.toEpochMilli(),
                                                deltaDays = deltaDays,
                                                deltaMinutes = deltaMinutes,
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    onMovePreviewEnd = { eventDrag = null },
                                    dayIndex = dayIndex,
                                    visibleDayCount = timedDays.size,
                                    eventLeftInDay = blockLeft,
                                    dayWidth = colWidth,
                                    hourHeight = hourHeight,
                                )
                            }
                            // Last, so it crosses the blocks instead of hiding behind them. The
                            // whole point of the line is to say where you are in a day that is
                            // mostly full of events; underneath them it only shows in the gaps.
                            if (day.date == today) {
                                val nowY = nowFractionalHour * with(density) { hourHeight.toPx() }
                                Canvas(Modifier.fillMaxSize()) {
                                    drawLine(
                                        color = nowColor,
                                        start = Offset(0f, nowY),
                                        end = Offset(size.width, nowY),
                                        strokeWidth = 1.5f,
                                    )
                                    drawCircle(
                                        color = nowColor,
                                        radius = 4.5f,
                                        center = Offset(0f, nowY),
                                    )
                                }
                            }
                        }
                    }
                }
                // Current-time label pinned to the left gutter, aligned with the now line drawn in
                // the day columns. Overlaid on the Row so it lines up across the shared scale.
                if (showNowLabel) {
                    Box(
                        Modifier
                            .width(54.dp)
                            .offset(y = hourHeight * nowFractionalHour - 8.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        // Coloured text, not a filled chip. The chip was the loudest thing on a
                        // screen whose whole job is the events, and it was shouting the one fact
                        // the user can also read off the clock in their status bar.
                        Text(
                            nowZ.format(nowLabelFmt),
                            modifier = Modifier.padding(end = 10.dp),
                            color = nowColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

private data class TimeSelection(
    val date: LocalDate,
    val startMinute: Int,
    val endMinute: Int,
) {
    fun normalized(): IntRangeLike =
        if (startMinute <= endMinute) {
            IntRangeLike(startMinute, endMinute)
        } else {
            IntRangeLike(endMinute, startMinute)
        }
}

private data class IntRangeLike(val first: Int, val second: Int)

private data class EventDrag(
    val eventId: Long,
    val instanceStartMillis: Long,
    val deltaDays: Int,
    val deltaMinutes: Int,
)

private fun Int.roundToStep(step: Int): Int {
    val half = step / 2
    return ((this + half) / step) * step
}

/** An all-day event and the inclusive range of visible day columns it covers. */
internal data class AllDaySpan(val event: Event, val firstCol: Int, val lastCol: Int)

/**
 * All-day / multi-day header. Each event is drawn as a single bar spanning every visible day
 * column it covers, instead of a repeated (and truncated) chip per day. Overlapping events are
 * packed into stacked lanes the way a week grid does.
 */
@Composable
private fun AllDayHeader(
    days: List<TimelineDay>,
    onEventClick: (eventId: Long, instanceStartMillis: Long) -> Unit,
) {
    val lanes = remember(days) { assignAllDayLanes(computeAllDaySpans(days)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp, end = TimelineEndInset),
    ) {
        Spacer(Modifier.width(TimelineGutterWidth))
        BoxWithConstraints(Modifier.weight(1f)) {
            val colWidth = maxWidth / days.size.coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                lanes.forEach { lane ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(22.dp),
                    ) {
                        lane.forEach { span ->
                            AllDayBar(
                                event = span.event,
                                modifier = Modifier
                                    .offset(x = colWidth * span.firstCol)
                                    .width(colWidth * (span.lastCol - span.firstCol + 1))
                                    .fillMaxHeight()
                                    .padding(horizontal = 2.dp),
                                onClick = {
                                    onEventClick(span.event.id, span.event.start.toEpochMilli())
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Collapse the per-day event lists back into one span per event. Keyed by (id, instance start) so a
 * genuine multi-day event (same instance repeated across days) becomes one wide bar, while a daily
 * recurring all-day event (distinct instances sharing an id) stays one bar per day.
 */
internal fun computeAllDaySpans(days: List<TimelineDay>): List<AllDaySpan> {
    val first = LinkedHashMap<Pair<Long, Long>, Int>()
    val last = HashMap<Pair<Long, Long>, Int>()
    val event = HashMap<Pair<Long, Long>, Event>()
    days.forEachIndexed { idx, day ->
        day.events.filter { it.allDay }.forEach { e ->
            val key = e.id to e.start.toEpochMilli()
            first.putIfAbsent(key, idx)
            last[key] = idx
            event[key] = e
        }
    }
    return first.keys
        .map { key -> AllDaySpan(event.getValue(key), first.getValue(key), last.getValue(key)) }
        .sortedWith(compareBy({ it.firstCol }, { -(it.lastCol - it.firstCol) }))
}

/** Greedy interval partitioning: place each span in the first lane where it doesn't overlap. */
internal fun assignAllDayLanes(spans: List<AllDaySpan>): List<List<AllDaySpan>> {
    val lanes = mutableListOf<MutableList<AllDaySpan>>()
    for (span in spans) {
        val lane = lanes.firstOrNull { existing ->
            existing.none { it.firstCol <= span.lastCol && span.firstCol <= it.lastCol }
        }
        if (lane != null) lane.add(span) else lanes.add(mutableListOf(span))
    }
    return lanes
}

@Composable
private fun AllDayBar(event: Event, modifier: Modifier, onClick: () -> Unit) {
    // The same solid fill a timed block gets, from the same helper, so the two cannot drift apart
    // again — which is exactly what had happened: a solid slab up here and a ten-percent wash down
    // there, for two things that are the same kind of object.
    val colors = eventColors(event.color)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(colors.container)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            event.title,
            style = MaterialTheme.typography.labelSmall,
            color = colors.content,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EventBlock(
    event: Event,
    zone: ZoneId,
    heightDp: Dp,
    compact: Boolean,
    accentStripe: Boolean,
    cornerRadius: Dp,
    nested: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onMove: ((deltaDays: Int, deltaMinutes: Int) -> Unit)? = null,
    onMovePreview: ((deltaDays: Int, deltaMinutes: Int) -> Unit)? = null,
    onMovePreviewEnd: () -> Unit = {},
    dayIndex: Int = 0,
    visibleDayCount: Int = 1,
    eventLeftInDay: Dp = 0.dp,
    dayWidth: Dp = 0.dp,
    hourHeight: Dp = 60.dp,
) {
    val colors = eventColors(event.color)
    val textColor = colors.content
    // The same ink, stepped back, so the time reads as secondary without falling off the fill.
    val mutedTextColor = colors.content.copy(alpha = 0.78f)
    // Too short to stack a title and a time; one line, vertically centred.
    val slim = heightDp < 30.dp
    val start = event.start.atZone(zone)
    val end = event.end.atZone(zone)
    val is24Hour = LocalUse24HourClock.current
    val locale = currentLocale()
    val timeFmt = remember(is24Hour, locale) { timeFormatter(is24Hour, locale) }
    val showTime = !compact && heightDp >= 40.dp
    val textPadding = when {
        slim -> Modifier.fillMaxSize().padding(horizontal = if (compact) 3.dp else 8.dp)
        // Week columns are only ~48dp wide on a phone. Every dp of padding here costs a character,
        // and once a word no longer fits the line the layout breaks it mid-word ("plannin/g").
        compact -> Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 2.dp)
        else -> Modifier.fillMaxSize().padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 5.dp)
    }
    val titleScale = if (compact) 10.sp else 13.sp
    val detailScale = if (compact) 10.sp else 11.sp
    val maxTitleLines = if (compact) 3 else 2

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(colors.container)
            // A block sitting inside another one is the same colour as the thing underneath it, so
            // without an outline the pair reads as a single shape with a caption halfway down.
            .then(
                if (nested) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(cornerRadius),
                    )
                } else {
                    Modifier
                },
            )
            .then(
                if (onMove != null) {
                    Modifier.pointerInput(event.id, event.start, eventLeftInDay, dayWidth, hourHeight) {
                        var totalDrag = Offset.Zero
                        fun deltas(): Pair<Int, Int> {
                            val rawMinutes = (totalDrag.y / hourHeight.toPx() * 60f).toInt()
                            val deltaMinutes = rawMinutes.roundToStep(15)
                            val widthPx = dayWidth.toPx().takeIf { it > 0f } ?: return 0 to deltaMinutes
                            val centerInWeek = eventLeftInDay.toPx() + size.width / 2f + totalDrag.x
                            val relativeDay = floor(centerInWeek / widthPx).toInt()
                            val targetDay = (dayIndex + relativeDay).coerceIn(0, visibleDayCount - 1)
                            return (targetDay - dayIndex) to deltaMinutes
                        }

                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                totalDrag = Offset.Zero
                                onMovePreview?.invoke(0, 0)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDrag += dragAmount
                                val (deltaDays, deltaMinutes) = deltas()
                                onMovePreview?.invoke(deltaDays, deltaMinutes)
                            },
                            onDragCancel = {
                                totalDrag = Offset.Zero
                                onMovePreviewEnd()
                            },
                            onDragEnd = {
                                val (deltaDays, deltaMinutes) = deltas()
                                totalDrag = Offset.Zero
                                onMovePreviewEnd()
                                if (deltaDays != 0 || deltaMinutes != 0) {
                                    onMove(deltaDays, deltaMinutes)
                                }
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
    ) {
        // No stripe: the fill is the colour now, and a stripe of the same colour on top of it is
        // just a seam. It existed to give a 10%-tinted block something to be identified by.
        if (slim) {
            // A quarter-hour slot is about fifteen dp. There is room for one line of the title and
            // nothing else, and trying to fit more is what used to make these overlap the event
            // below them.
            Box(textPadding, contentAlignment = Alignment.CenterStart) {
                Text(
                    event.title,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            // Title only. The block already says when it is — that is what its position and its
            // length on the grid are for — so printing the times inside it is the same fact twice,
            // taking the room the title wanted.
            Column(textPadding) {
                Text(
                    event.title,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    fontSize = titleScale,
                    maxLines = maxTitleLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * One event's box inside a day column, expressed as fractions of that column's width.
 *
 * Fractions rather than a column index because an event nested inside another does not get a
 * column of its own: it keeps its container's right edge and only gives up ground on the left,
 * drawn on top. An index-based layout has no way to say that — it can only halve the column, which
 * is what turned a full-width "Arbeit" into a 24dp ribbon reading "Ar / bei / t" as soon as a
 * 45-minute break was booked inside it.
 */
internal data class PositionedEvent(
    val event: Event,
    val leftFraction: Float,
    val widthFraction: Float,
    /** 0 for an event nothing contains. Deeper events are drawn later, so they land on top. */
    val depth: Int,
    val topDp: Dp,
    val heightDp: Dp,
)

/** How much of its container's width a nested event gives up on the left. */
private const val NestIndent = 0.12f

internal fun layoutTimed(
    events: List<Event>,
    hourHeight: Dp,
    zone: ZoneId,
): List<PositionedEvent> {
    if (events.isEmpty()) return emptyList()
    // Longest first among events that start together, so a container is always seen before the
    // things inside it and the containment stack below never has to look backwards.
    val sorted = events.sortedWith(compareBy<Event> { it.start }.thenByDescending { it.end })
    val children = nestingOf(sorted)
    val out = mutableListOf<PositionedEvent>()
    place(children[sorted.size], sorted, children, 0f, 1f, 0, hourHeight, zone, out)
    // Painter's order: a nested block has to be drawn after the block it sits inside.
    return out.sortedBy { it.depth }
}

/**
 * Indices of the events directly inside each event; roots live at index `sorted.size`.
 *
 * Works as a stack because [sorted] is in start order: once the innermost open container no longer
 * encloses the event we are placing, nothing deeper can either.
 */
private fun nestingOf(sorted: List<Event>): List<MutableList<Int>> {
    val children = List(sorted.size + 1) { mutableListOf<Int>() }
    val open = ArrayDeque<Int>()
    for (i in sorted.indices) {
        while (open.isNotEmpty() && !encloses(sorted[open.last()], sorted[i])) open.removeLast()
        children[open.lastOrNull() ?: sorted.size].add(i)
        open.addLast(i)
    }
    return children
}

/**
 * Whether [outer] wholly contains [inner] *and* is strictly larger.
 *
 * Two events on exactly the same slot enclose each other by the loose reading, which would make one
 * of them a child of the other for no reason. They are peers, and peers go side by side.
 */
private fun encloses(outer: Event, inner: Event): Boolean =
    outer.start <= inner.start && outer.end >= inner.end &&
        (outer.start < inner.start || outer.end > inner.end)

/**
 * Lay a set of sibling events out across the band `[left, right)` and recurse into what they hold.
 *
 * Siblings that genuinely overlap in time still go side by side — there is no other honest way to
 * show two half-overlapping meetings — but one that ends before the next begins gets its column
 * back, so a day of back-to-back events stays full width.
 */
@Suppress("LongParameterList")
private fun place(
    group: List<Int>,
    sorted: List<Event>,
    children: List<List<Int>>,
    left: Float,
    right: Float,
    depth: Int,
    hourHeight: Dp,
    zone: ZoneId,
    out: MutableList<PositionedEvent>,
) {
    if (group.isEmpty()) return
    val columnEnds = mutableListOf<Long>()
    val columnOf = mutableMapOf<Int, Int>()
    for (i in group) {
        val e = sorted[i]
        val free = columnEnds.indices.firstOrNull { columnEnds[it] <= e.start.toEpochMilli() }
        if (free != null) {
            columnEnds[free] = e.end.toEpochMilli()
            columnOf[i] = free
        } else {
            columnEnds.add(e.end.toEpochMilli())
            columnOf[i] = columnEnds.size - 1
        }
    }
    val slot = (right - left) / columnEnds.size
    for (i in group) {
        val e = sorted[i]
        val blockLeft = left + slot * columnOf.getValue(i)
        val blockRight = blockLeft + slot
        val startZ = e.start.atZone(zone)
        val endZ = e.end.atZone(zone)
        val startFrac = (startZ.hour + startZ.minute / 60f + startZ.second / 3600f)
            .coerceIn(0f, 24f)
        // Keep a minimum visible slice, but never let the lower bound exceed 24h — an event
        // starting after 23:45 would otherwise make coerceIn's range empty and crash.
        val endFrac = (endZ.hour + endZ.minute / 60f + endZ.second / 3600f)
            .coerceIn((startFrac + 0.25f).coerceAtMost(24f), 24f)
        // Enough for one line of small text and no more. It used to be 32dp, which is over half an
        // hour of grid: a quarter-hour event was inflated to twice its length and drawn straight
        // over whatever started when it ended.
        val height = (hourHeight * (endFrac - startFrac)).coerceAtLeast(16.dp)
        out.add(
            PositionedEvent(
                event = e,
                leftFraction = blockLeft,
                widthFraction = blockRight - blockLeft,
                depth = depth,
                topDp = hourHeight * startFrac,
                heightDp = height,
            ),
        )
        place(
            group = children[i],
            sorted = sorted,
            children = children,
            left = blockLeft + (blockRight - blockLeft) * NestIndent,
            right = blockRight,
            depth = depth + 1,
            hourHeight = hourHeight,
            zone = zone,
            out = out,
        )
    }
}
