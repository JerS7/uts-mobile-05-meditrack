package com.example.meditrack.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// Small local vector set: no additional icon dependency is needed.
internal object MediTrackIcons {
    private fun outline(name: String, data: String): ImageVector = ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(data).toNodes(),
        stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
    ).build()

    val Pill = outline("Pill", "M9 5 A5 5 0 0 1 19 12 L12 19 A5 5 0 0 1 5 12 Z M8 8 L16 16")
    val Person = outline("Person", "M15 7 A3 3 0 1 1 9 7 A3 3 0 1 1 15 7 M5 20 L5 18 C5 13 19 13 19 18 L19 20 Z")
    val Pulse = outline("Pulse", "M2 12 L6 12 L9 4 L14 20 L17 12 L22 12")
    val Info = outline("Info", "M22 12 A10 10 0 1 1 2 12 A10 10 0 1 1 22 12 M12 11 L12 17 M12 7 L12 7.1")
    val Calendar = outline("Calendar", "M5 5 L19 5 L19 21 L5 21 Z M8 3 L8 7 M16 3 L16 7 M5 10 L19 10")
    val Check = outline("Check", "M5 12 L10 17 L19 7")
    val Close = outline("Close", "M6 6 L18 18 M18 6 L6 18")
    val Trend = outline("Trend", "M3 17 L9 11 L13 15 L21 6 M15 6 L21 6 L21 12")
    val School = outline("School", "M2 9 L12 3 L22 9 L12 15 Z M6 12 L6 18 L12 21 L18 18 L18 12 M22 9 L22 17")
    val Add = outline("Add", "M12 4 L12 20 M4 12 L20 12")
    val Clock = outline("Clock", "M22 12 A10 10 0 1 1 2 12 A10 10 0 1 1 22 12 M12 6 L12 12 L16 15")
    val Reset = outline("Reset", "M4 8 A9 9 0 1 1 3 15 M4 3 L4 8 L9 8")
    val Eye = outline("Eye", "M2 12 C7 3 17 3 22 12 C17 21 7 21 2 12 Z M15 12 A3 3 0 1 1 9 12 A3 3 0 1 1 15 12")
    val Shield = outline("Shield", "M12 2 L20 6 L19 15 Q18 19 12 22 Q6 19 5 15 L4 6 Z M8 12 L11 15 L16 9")
}
