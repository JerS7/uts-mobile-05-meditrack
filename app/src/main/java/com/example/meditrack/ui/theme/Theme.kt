package com.example.meditrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Keep the wireframe palette consistent on all devices.
private val MediTrackColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealInk,
    secondary = TealBright,
    onSecondary = Color.White,
    // Selected filter chips and the snackbar action would otherwise use Material's default purple.
    secondaryContainer = TealContainer,
    onSecondaryContainer = TealInk,
    inversePrimary = TealLight,
    // Dialogs and pickers use these containers; keep them neutral instead of lavender.
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = PanelBackground,
    background = PageBackground,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = PanelBackground,
    onSurfaceVariant = TextSecondary,
    outline = TextSecondary,
    outlineVariant = TrackBackground,
    error = MissedRed,
    onError = Color.White,
    errorContainer = MissedContainer,
)

@Composable
fun MediTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MediTrackColors,
        typography = Typography,
        content = content,
    )
}
