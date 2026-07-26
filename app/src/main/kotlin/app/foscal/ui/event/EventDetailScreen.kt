package app.foscal.ui.event

import android.content.Context
import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.foscal.core.model.Attendee
import app.foscal.core.model.AttendeeStatus
import app.foscal.core.model.Event
import app.foscal.core.model.MeetingLinks
import app.foscal.core.ui.theme.BricolageFamily
import app.foscal.core.ui.theme.Motion
import app.foscal.location.openInMaps
import app.foscal.ui.util.LocalUse24HourClock
import app.foscal.ui.util.currentLocale
import app.foscal.ui.util.timeFormatter
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Long,
    instanceStartMillis: Long = 0L,
    onBack: () -> Unit,
    onEdit: (eventId: Long, instanceStartMillis: Long) -> Unit,
    onOpenLocationMap: (location: String) -> Unit = {},
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Reload on every resume so returning from the editor reflects edits; show the spinner
    // only for the first fetch, refresh silently afterwards.
    LifecycleResumeEffect(eventId, instanceStartMillis) {
        viewModel.load(eventId, instanceStartMillis, showLoading = state.event == null)
        onPauseOrDispose {}
    }

    val event = state.event

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {},
                actions = {
                    if (event != null) {
                        IconButton(onClick = { onEdit(eventId, event.start.toEpochMilli()) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                        }
                    }
                },
            )
        },
    ) { padding ->
        val phase = when {
            state.loading -> "loading"
            event == null -> "missing"
            else -> "content"
        }
        Crossfade(
            targetState = phase,
            animationSpec = tween(Motion.DurationMedium),
            label = "detailCrossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) { p ->
            when (p) {
                "loading" -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                "missing" -> Box(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Event not found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    // Crossfade keeps the old slot alive during a content→missing transition, so
                    // `phase` may be stale while `state.event` has already cleared. Re-check rather
                    // than `!!` to avoid an NPE mid-animation.
                    val current = state.event
                    if (current != null) {
                        DetailContent(
                            event = current,
                            calendarName = state.calendar?.displayName ?: "Calendar",
                            calendarColor = current.color,
                            attendees = state.attendees,
                            mapsEnabled = state.mapsEnabled,
                            onOpenLocationMap = onOpenLocationMap,
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    event: Event,
    calendarName: String,
    calendarColor: Int,
    attendees: List<Attendee>,
    mapsEnabled: Boolean,
    onOpenLocationMap: (location: String) -> Unit,
) {
    val context = LocalContext.current
    // The link is looked for in the location first: an invite whose location *is* the call means it
    // literally, while a description often quotes a dial-in or recording link further down too.
    val meetingUrl = remember(event.location, event.description) {
        MeetingLinks.find(event.location, event.description)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(calendarColor).copy(alpha = 0.14f))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LabelPill(calendarName, calendarColor)
            Text(
                event.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BricolageFamily),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        InfoCard(Icons.Outlined.AccessTime, "When", formatWhen(event, LocalUse24HourClock.current, currentLocale()))
        meetingUrl?.let { url ->
            InfoCard(
                icon = Icons.Outlined.Videocam,
                label = MeetingLinks.providerName(url)?.let { "Join $it" } ?: "Join video call",
                value = url,
                trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                onClick = { openLink(context, url) },
            )
        }
        event.rrule?.takeIf { it.isNotBlank() }?.let {
            InfoCard(Icons.Outlined.Repeat, "Repeats", describeRecurrence(it))
        }
        // A location that is nothing but the call link is already the Join card above, and handing
        // a URL to a `geo:` intent searches a map for it — so the location card is suppressed
        // entirely in that case. A location that merely *contains* a link ("Room B — https://…")
        // still carries a real place and keeps its card.
        event.location
            ?.takeIf { it.isNotBlank() && it.trim() != meetingUrl }
            ?.let { location ->
                InfoCard(
                    icon = Icons.Outlined.LocationOn,
                    label = "Location",
                    value = location,
                    trailingIcon = Icons.Outlined.Map,
                    // With the opt-in map on, show the place on an in-app OpenStreetMap; otherwise
                    // hand the text to the device's maps app via a geo: intent so we stay offline.
                    onClick = {
                        if (mapsEnabled) onOpenLocationMap(location) else openInMaps(context, location)
                    },
                )
            }
        event.description?.takeIf { it.isNotBlank() }?.let {
            InfoCard(Icons.Outlined.Description, "Notes", it)
        }
        if (attendees.isNotEmpty()) {
            GuestsCard(attendees = attendees, onEmail = { openMail(context, it) })
        }
    }
}

/**
 * The guest list, organizer first, with each person's answer.
 *
 * Tapping a row opens a mail composer — Foscal has no way to send or answer an invitation itself
 * (that is the sync adapter's and the server's job), so passing the address to whatever mail app the
 * user already has is the honest affordance rather than a Yes/No pair that would go nowhere.
 */
@Composable
private fun GuestsCard(attendees: List<Attendee>, onEmail: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (attendees.size == 1) "1 guest" else "${attendees.size} guests",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            attendees.forEach { attendee ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onEmail(attendee.email) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            attendee.initial(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            attendee.label,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            attendee.subtitle(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val statusIcon = when (attendee.status) {
                        AttendeeStatus.ACCEPTED -> Icons.Outlined.CheckCircle
                        AttendeeStatus.DECLINED -> Icons.Outlined.Cancel
                        AttendeeStatus.TENTATIVE -> Icons.AutoMirrored.Outlined.HelpOutline
                        AttendeeStatus.INVITED -> Icons.Outlined.Schedule
                    }
                    Icon(
                        statusIcon,
                        contentDescription = attendee.statusLabel(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** First letter of the display name, or of the address when there is none. */
private fun Attendee.initial(): String =
    label.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"

/** The answer, plus the address when the name is what's already on the row above. */
private fun Attendee.subtitle(): String {
    val role = if (isOrganizer) "Organizer • " else ""
    val optionalMark = if (optional && !isOrganizer) " (optional)" else ""
    return if (label == email) {
        "$role${statusLabel()}$optionalMark"
    } else {
        "$role$email • ${statusLabel()}$optionalMark"
    }
}

private fun Attendee.statusLabel(): String = when (status) {
    AttendeeStatus.ACCEPTED -> "Going"
    AttendeeStatus.DECLINED -> "Not going"
    AttendeeStatus.TENTATIVE -> "Maybe"
    AttendeeStatus.INVITED -> "Awaiting reply"
}

/**
 * Hands [url] to whatever the user browses with.
 *
 * `resolveActivity` is deliberately not consulted first — package visibility on API 30+ hides
 * browsers this app has no `<queries>` entry for, so the check reports "nothing can open this" for
 * links that in fact open fine. Catching the failure covers the genuinely empty case.
 */
private fun openLink(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}

private fun openMail(context: Context, email: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_SENDTO, "mailto:$email".toUri()))
    }
}

@Composable
private fun LabelPill(calendarName: String, calendarColor: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(calendarColor)),
        )
        Text(
            calendarName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    trailingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(2.dp))
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
            if (trailingIcon != null) {
                Icon(
                    trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private fun describeRecurrence(rrule: String): String {
    val freq = rrule.split(';')
        .firstOrNull { it.startsWith("FREQ=") }
        ?.substringAfter('=')
        ?.uppercase()
    return when (freq) {
        "DAILY" -> "Every day"
        "WEEKLY" -> "Every week"
        "MONTHLY" -> "Every month"
        "YEARLY" -> "Every year"
        else -> "Repeats"
    }
}

private fun formatWhen(event: Event, is24Hour: Boolean, locale: Locale): String {
    val zone = ZoneId.systemDefault()
    val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", locale)
    val timeFmt = timeFormatter(is24Hour, locale)
    val start = event.start.atZone(zone)
    val end = event.end.atZone(zone)
    return if (event.allDay) {
        if (start.toLocalDate() == end.toLocalDate().minusDays(1)) {
            "All day • ${start.toLocalDate().format(dateFmt)}"
        } else {
            "All day • ${start.toLocalDate().format(dateFmt)} – ${end.toLocalDate().minusDays(1).format(dateFmt)}"
        }
    } else {
        if (start.toLocalDate() == end.toLocalDate()) {
            "${start.format(dateFmt)}\n${start.format(timeFmt)} – ${end.format(timeFmt)}"
        } else {
            "${start.format(dateFmt)} ${start.format(timeFmt)}\n– ${end.format(dateFmt)} ${end.format(timeFmt)}"
        }
    }
}
