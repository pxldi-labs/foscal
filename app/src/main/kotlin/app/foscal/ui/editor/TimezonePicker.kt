package app.foscal.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/**
 * A zone as the row shows it: "Europe/Berlin" plus the offset it is at right now.
 *
 * The offset is resolved against the current instant rather than printed from the zone's raw rules,
 * because half the world is on summer time half the year and "GMT+1" for Berlin in August is simply
 * wrong. It is the number the reader will check the times against.
 */
internal fun zoneLabel(zone: ZoneId, at: Instant = Instant.now()): String {
    val offset = zone.rules.getOffset(at)
    return "${zone.id.replace('_', ' ')} (${offsetLabel(offset)})"
}

/** "GMT+2", "GMT+5:30", "GMT" — the half-hour zones are why the minutes cannot just be dropped. */
internal fun offsetLabel(offset: ZoneOffset): String {
    val seconds = offset.totalSeconds
    if (seconds == 0) return "GMT"
    val sign = if (seconds < 0) "-" else "+"
    val hours = abs(seconds) / 3600
    val minutes = (abs(seconds) % 3600) / 60
    return if (minutes == 0) "GMT$sign$hours" else "GMT$sign$hours:%02d".format(minutes)
}

/**
 * Everything `ZoneId` knows, ordered the way someone hunting for their own zone reads it.
 *
 * Sorted by offset rather than alphabetically: a person looking for "somewhere in central Europe"
 * knows the offset long before they remember whether their zone is filed under Berlin, Paris or
 * Amsterdam, and the alphabetical list buries Europe under Africa and America.
 */
private fun allZones(at: Instant): List<ZoneId> =
    ZoneId.getAvailableZoneIds()
        .asSequence()
        .filter { '/' in it }
        .map(ZoneId::of)
        .sortedWith(compareBy({ it.rules.getOffset(at).totalSeconds }, { it.id }))
        .toList()

/**
 * Picks the zone an event's times are written in.
 *
 * The device's own zone is pinned to the top, because "put it back to where I am" is the single
 * most likely reason this dialog is opened a second time.
 */
@Composable
internal fun TimezonePickerDialog(
    selected: ZoneId,
    onSelect: (ZoneId) -> Unit,
    onDismiss: () -> Unit,
) {
    val now = remember { Instant.now() }
    val device = remember { ZoneId.systemDefault() }
    val zones = remember(now) { allZones(now) }
    var query by remember { mutableStateOf("") }
    val matches = remember(query, zones) {
        // Underscores are how the database spells a space and not how anyone types one, so both
        // sides of the comparison lose them.
        val q = query.trim().lowercase().replace('_', ' ')
        val found = if (q.isEmpty()) zones else zones.filter { zone ->
            zone.id.lowercase().replace('_', ' ').contains(q) ||
                zone.getDisplayName(TextStyle.FULL, Locale.getDefault()).lowercase().contains(q) ||
                offsetLabel(zone.rules.getOffset(now)).lowercase().contains(q)
        }
        (listOf(device) + found.filter { it != device }).distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Time zone") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("Search") },
                    placeholder = { Text("City, region or offset") },
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(matches, key = { it.id }) { zone ->
                        ZoneRow(
                            zone = zone,
                            now = now,
                            isDevice = zone == device,
                            selected = zone == selected,
                            onClick = { onSelect(zone) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun ZoneRow(
    zone: ZoneId,
    now: Instant,
    isDevice: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (selected) Icons.Filled.Check else Icons.Filled.Public,
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                zone.id.replace('_', ' '),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (isDevice) {
                Text(
                    "Where this phone is",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            offsetLabel(zone.rules.getOffset(now)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
