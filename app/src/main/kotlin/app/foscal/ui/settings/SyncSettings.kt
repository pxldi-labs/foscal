package app.foscal.ui.settings

import android.accounts.Account
import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.foscal.ui.calendars.CalendarRow

/**
 * What Foscal can and cannot do about syncing.
 *
 * Foscal has no network code at all: DAVx⁵ (or whichever app owns the account) fetches the
 * calendars and decides how often and how far back. This page exists because that is not obvious
 * from inside a calendar app — the honest answer to "how do I change my sync interval" is "not
 * here", and saying so beats offering a knob that quietly does nothing.
 */
@Composable
fun SyncSettings(
    calendars: List<CalendarRow>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var asked by remember { mutableStateOf(false) }

    // One entry per account behind a calendar, minus the phone's own local one, which nothing
    // syncs and which would only ever report failure.
    val accounts = remember(calendars) {
        calendars
            .map { it.calendar }
            .filterNot { it.isLocal }
            .map { Account(it.accountName, it.accountType) }
            .distinct()
    }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Your sync app is in charge",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Foscal never goes online. DAVx⁵, or whichever app owns your account, " +
                        "decides how often your calendars are fetched and how far back they go. " +
                        "Foscal reads what it finds on the phone, and can show only what has " +
                        "already been fetched.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Foscal itself has no limit: scroll to 2011 and it will draw 2011, if your " +
                        "sync app brought it down.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (accounts.isEmpty()) {
            Text(
                "Nothing here syncs. Every calendar on this phone is a local one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Button(
                onClick = {
                    accounts.forEach { account ->
                        // MANUAL and EXPEDITED together are what "the user is standing here
                        // waiting" means to the sync framework: run now, and run even if the
                        // account is set to sync on its own schedule.
                        val extras = Bundle().apply {
                            putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                            putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                        }
                        runCatching {
                            ContentResolver.requestSync(
                                account,
                                CalendarContract.AUTHORITY,
                                extras,
                            )
                        }
                    }
                    asked = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sync now")
            }
            Text(
                if (asked) {
                    // Deliberately not "Synced": the request is handed to another app, and
                    // claiming it finished would be a guess dressed up as a result.
                    "Asked ${accounts.size} account${if (accounts.size == 1) "" else "s"} to " +
                        "sync. New events appear as they arrive."
                } else {
                    "Asks every account behind your calendars to fetch now."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_SYNC_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Android's account settings")
        }
        OutlinedButton(
            onClick = {
                // The app itself if it is installed, its F-Droid page if it is not. Both are
                // wrapped: a phone with neither should show nothing rather than crash.
                val launch = context.packageManager.getLaunchIntentForPackage(DAVX_PACKAGE)
                val intent = launch ?: Intent(Intent.ACTION_VIEW, DAVX_FDROID.toUri())
                runCatching {
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open DAVx⁵")
        }
    }
}

private const val DAVX_PACKAGE = "at.bitfire.davdroid"
private const val DAVX_FDROID = "https://f-droid.org/packages/at.bitfire.davdroid/"
