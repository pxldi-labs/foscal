package app.foscal.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.foscal.core.model.UiColor
import app.foscal.core.ui.theme.CobaltAccent
import app.foscal.core.ui.theme.LocalIsDarkTheme

/**
 * Where the app's chrome gets its colour: the system's, Foscal's own, or one you pick.
 *
 * Three swatches rather than a palette of presets. The colours that carry meaning in a calendar
 * belong to the calendars and their events; this is only about the frame around them, and a frame
 * needs one answer, not six.
 */
@Composable
fun UiColorPicker(
    selected: UiColor,
    customColor: Int,
    onSelect: (UiColor) -> Unit,
    onPickCustom: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalIsDarkTheme.current
    var showPicker by remember { mutableStateOf(false) }
    // Material You needs a wallpaper-derived palette the platform only exposes from Android 12 on.
    // Below that the swatch would be a choice that changes nothing, so it is not offered — and a
    // preference stored on a newer phone still falls back to Foscal's blue when read here.
    val systemAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Main colour", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            if (systemAvailable) {
                Swatch(
                    color = MaterialTheme.colorScheme.primary,
                    label = "System",
                    selected = selected == UiColor.SYSTEM,
                    rainbowRing = selected != UiColor.SYSTEM,
                    onClick = { onSelect(UiColor.SYSTEM) },
                )
            }
            Swatch(
                color = if (dark) CobaltAccent.primaryDark else CobaltAccent.primaryLight,
                label = "Foscal",
                selected = selected == UiColor.FOSCAL || (!systemAvailable && selected == UiColor.SYSTEM),
                onClick = { onSelect(UiColor.FOSCAL) },
            )
            Swatch(
                color = Color(customColor),
                label = "Custom",
                selected = selected == UiColor.CUSTOM,
                rainbowRing = true,
                onClick = { showPicker = true },
            )
        }
        Text(
            when {
                selected == UiColor.SYSTEM && systemAvailable -> "Taken from your wallpaper"
                selected == UiColor.CUSTOM -> "A colour you picked"
                else -> "Foscal's own blue"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showPicker) {
        ColorPickerDialog(
            initialColor = customColor,
            onDismiss = { showPicker = false },
            onConfirm = {
                showPicker = false
                onPickCustom(it)
            },
        )
    }
}

@Composable
private fun Swatch(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    rainbowRing: Boolean = false,
) {
    val rainbow = Brush.sweepGradient(
        listOf(
            Color(0xFFE53935), Color(0xFFFFB300), Color(0xFF43A047),
            Color(0xFF00ACC1), Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFE53935),
        ),
    )
    Column(
        modifier = Modifier
            .selectable(selected = selected, onClick = onClick)
            .size(width = 60.dp, height = 78.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    when {
                        selected -> Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        rainbowRing -> Modifier.border(2.dp, rainbow, CircleShape)
                        else -> Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
        )
    }
}
