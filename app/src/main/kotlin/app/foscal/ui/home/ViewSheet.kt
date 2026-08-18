package app.foscal.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.foscal.core.ui.theme.Motion
import app.foscal.ui.calendars.CalendarRow
import app.foscal.ui.contrastColor
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Everything that adjusts what you are looking at, in one panel within thumb reach.
 *
 * Opened from the bottom bar rather than an edge swipe: on a phone with gesture navigation the
 * system owns every screen edge — left and right are back, the bottom is home — so there is no
 * edge drag left for an app to claim. A panel that rises from the button that opened it needs no
 * edge, and puts its contents where the hand already is instead of at the top-left corner.
 *
 * The views collapse to a single row so the calendars are the body of the sheet rather than an
 * afterthought below five stacked rows.
 */
@Composable
fun ViewSheet(
    current: CalendarView,
    calendars: List<CalendarRow>,
    onSelect: (CalendarView) -> Unit,
    onToggleCalendar: (CalendarRow) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    DragSheet(onDismiss = onDismiss) {
        ViewRow(current = current, onSelect = onSelect)
        SheetDivider()
        SectionLabel("Calendars")
        calendars.forEach { row ->
            CalendarToggle(row = row, onToggle = { onToggleCalendar(row) })
        }
        SheetDivider()
        SheetAction(label = "Settings", onClick = onOpenSettings)
    }
}

/** How much of the screen the sheet shows on opening, when it has more than that to show. */
private const val OpenFraction = 0.5f

/** Never quite the whole screen: the strip of page left visible is what says this is a panel. */
private const val MaxFraction = 0.92f

/** Dragged this far below where it opened, letting go closes it rather than leaving it there. */
private val DismissTravel = 96.dp

/**
 * A bottom sheet that stays where you put it.
 *
 * Material's sheet is anchored: it has two or three resting heights and settles to the nearest one
 * the moment you let go, so dragging it up past halfway makes it jump the rest of the way by
 * itself. That is the right behaviour for a sheet with two meaningful states and the wrong one
 * here, where the useful height is however much of the calendar list you happen to want to see.
 *
 * So there are no anchors. The sheet opens to [OpenFraction] of the screen, the drag moves it one
 * pixel per pixel, and letting go leaves it exactly there. The only settle left is downward: drag
 * it [DismissTravel] below where it opened and releasing closes it, because a sheet you can park
 * over the bottom of the screen and not get rid of would be worse than one that snaps.
 *
 * Content scrolling and sheet dragging share one gesture through a nested-scroll connection: while
 * the sheet has room to rise it takes the drag, and the contents only start scrolling once it is
 * fully open. On the way back down the contents scroll first and the sheet moves only once they
 * have run out — which is what makes a single upward flick feel like one movement rather than two.
 */
@Composable
private fun DragSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val screenPx = with(density) { maxHeight.toPx() }
            val dismissPx = with(density) { DismissTravel.toPx() }
            val scope = rememberCoroutineScope()

            // Distance the sheet is pushed below its fully-open position. Zero is fully open;
            // its own height is entirely off the bottom of the screen.
            val offset = remember { Animatable(0f) }
            var sheetPx by remember { mutableFloatStateOf(0f) }
            var openedAt by remember { mutableFloatStateOf(0f) }

            LaunchedEffect(sheetPx) {
                if (sheetPx <= 0f || openedAt > 0f) return@LaunchedEffect
                // A sheet shorter than the opening height has nothing to hold back and simply
                // arrives whole; a taller one comes up to the halfway line.
                openedAt = (sheetPx - screenPx * OpenFraction).coerceAtLeast(0f)
                offset.snapTo(sheetPx)
                offset.animateTo(openedAt, tween(Motion.DurationMedium))
            }

            val close: () -> Unit = {
                scope.launch {
                    offset.animateTo(sheetPx, tween(Motion.DurationShort))
                    onDismiss()
                }
            }

            /** Moves the sheet by [delta] px, returning how much of it was used. */
            fun drag(delta: Float): Float {
                val used = (offset.value + delta).coerceIn(0f, sheetPx) - offset.value
                // The new position is worked out again inside the coroutine rather than captured
                // here. Animatable serialises its writes, so a fast drag can queue several of
                // these, and a target computed up front would be stale by the time it ran —
                // which loses movement exactly when the finger is going fastest.
                if (used != 0f) {
                    scope.launch { offset.snapTo((offset.value + used).coerceIn(0f, sheetPx)) }
                }
                return used
            }

            val settle: () -> Unit = {
                if (offset.value > openedAt + dismissPx) close() else Unit
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = ScrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = close,
                    ),
            )

            val nested = remember {
                object : NestedScrollConnection {
                    // Upward drags lift the sheet before they scroll anything, so the gesture
                    // that opens it fully is the same one that then reads the list.
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                        if (available.y < 0f) Offset(0f, drag(available.y)) else Offset.Zero

                    // Downward drags scroll first and push the sheet only once the contents are
                    // back at the top, which is the difference between closing a sheet and
                    // scrolling one.
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset = if (available.y > 0f) Offset(0f, drag(available.y)) else Offset.Zero

                    // The end of the gesture, for drags that came through the contents rather
                    // than the handle. Without this the sheet could be pushed down by scrolling
                    // and then simply stay there, with nothing left to close it but the scrim.
                    override suspend fun onPreFling(available: Velocity): Velocity {
                        settle()
                        return Velocity.Zero
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * MaxFraction)
                    .offset { IntOffset(0, offset.value.roundToInt()) }
                    .onSizeChanged { sheetPx = it.height.toFloat() }
                    .nestedScroll(nested),
            ) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    DragHandle(onDrag = { drag(it) }, onDragStopped = settle)
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 20.dp),
                        content = content,
                    )
                }
            }
        }
    }
}

/** The grab bar. Dragging anywhere else is the contents' gesture; this one is always the sheet's. */
@Composable
private fun DragHandle(onDrag: (Float) -> Unit, onDragStopped: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { onDrag(it) },
                onDragStopped = { onDragStopped() },
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
        )
    }
}

private const val ScrimAlpha = 0.32f

@Composable
private fun ViewRow(current: CalendarView, onSelect: (CalendarView) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CalendarView.entries.forEach { view ->
            val selected = view == current
            val tint = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        },
                    )
                    .clickable(role = Role.Tab) { onSelect(view) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ViewGlyph(view = view, tint = tint, modifier = Modifier.size(22.dp))
                Text(
                    view.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = tint,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * A checkbox in the calendar's own colour, which is why it replaces the switch this used to be in
 * Settings: it toggles the calendar and tells you what colour that calendar draws in, from one
 * control instead of two.
 */
@Composable
private fun CalendarToggle(row: CalendarRow, onToggle: () -> Unit) {
    val color = Color(row.calendar.color)
    val visible = !row.isHidden
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (visible) color else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (visible) color else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(6.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (visible) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = contrastColor(row.calendar.color),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            row.calendar.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = if (visible) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}

@Composable
private fun SheetAction(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            Icons.Outlined.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun SheetDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(vertical = 6.dp),
    )
}
