package com.example.meditrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.meditrack.R
import com.example.meditrack.data.demoMedicationRecords
import com.example.meditrack.model.MedicationRecord
import com.example.meditrack.ui.state.RecordSummary
import com.example.meditrack.ui.state.summary
import com.example.meditrack.ui.theme.*
import com.example.meditrack.util.Dates

/** Dashboard for today: summary, progress, and today's records. */
@Composable
fun HomeScreen(
    records: List<MedicationRecord>,
    modifier: Modifier = Modifier,
    today: String = Dates.today(),
    onLogDose: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onAddDemoRecords: () -> Unit = {},
) {
    val todayRecords = records.filter { it.date == today }
    val summary = todayRecords.summary()
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("home-list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "header") { DashboardHeader(today) }
        item(key = "summary") { SummaryGrid(summary) }
        item(key = "progress") { RegimenProgress(summary) }
        item(key = "records-header") { RecordCountHeader(stringResource(R.string.home_today_records), todayRecords.size) }
        when {
            records.isEmpty() -> item(key = "empty-records") {
                EmptyRecords {
                    Button(onClick = onLogDose, colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White)) {
                        Text(stringResource(R.string.action_log_dose))
                    }
                    TextButton(onClick = onAddDemoRecords) { Text(stringResource(R.string.action_add_demo), color = Teal) }
                }
            }
            todayRecords.isEmpty() -> item(key = "empty-today") {
                EmptyRecords(
                    title = stringResource(R.string.empty_today_title),
                    message = stringResource(R.string.empty_today_message),
                ) {
                    Button(onClick = onLogDose, colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White)) {
                        Text(stringResource(R.string.action_log_dose))
                    }
                    TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.action_open_history), color = Teal) }
                }
            }
            else -> items(todayRecords, key = { "record-${it.id}" }, contentType = { "medication-record" }) { RecordCard(it) }
        }
        item(key = "footer") { AcademicFooter() }
    }
}

@Composable
private fun DashboardHeader(today: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(32.dp).background(Teal, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(MediTrackIcons.Pill, contentDescription = stringResource(R.string.app_name), tint = Color.White, modifier = Modifier.size(21.dp))
            }
            Box(Modifier.size(32.dp).background(Teal, CircleShape), contentAlignment = Alignment.Center) {
                Icon(MediTrackIcons.Person, contentDescription = stringResource(R.string.profile_illustration), tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SmallIcon(MediTrackIcons.Pulse, Teal)
            Text(stringResource(R.string.home_eyebrow), style = MaterialTheme.typography.labelMedium, color = Teal)
        }
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.home_subtitle), color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
        Row(
            Modifier.fillMaxWidth().background(TrackBackground.copy(alpha = 0.45f)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallIcon(MediTrackIcons.Info, Teal)
            Text(stringResource(R.string.home_disclaimer), style = MaterialTheme.typography.bodySmall)
        }
        Text(stringResource(R.string.home_today_label, Dates.display(today)), style = MaterialTheme.typography.titleSmall, color = TextSecondary)
    }
}

@Composable
private fun SummaryGrid(summary: RecordSummary) {
    val total = summary.total
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(stringResource(R.string.summary_logged), "$total", stringResource(R.string.summary_logged_unit), if (total == 0) 0f else 1f, MediTrackIcons.Calendar, Teal, Modifier.weight(1f), valueColor = TextPrimary)
            SummaryCard(stringResource(R.string.summary_taken), "${summary.taken}", stringResource(R.string.summary_taken_unit), summary.adherence, MediTrackIcons.Check, TakenGreen, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(stringResource(R.string.summary_missed), "${summary.missed}", stringResource(R.string.summary_missed_unit), if (total == 0) 0f else summary.missed.toFloat() / total, MediTrackIcons.Close, MissedRed, Modifier.weight(1f))
            SummaryCard(stringResource(R.string.summary_adherence), stringResource(R.string.percent, summary.percent), stringResource(R.string.summary_adherence_unit), summary.adherence, MediTrackIcons.Trend, Color.White, Modifier.weight(1f), highlighted = true)
        }
    }
}

@Composable
private fun SummaryCard(
    title: String, value: String, unit: String, progress: Float,
    icon: ImageVector, accent: Color, modifier: Modifier = Modifier, highlighted: Boolean = false,
    valueColor: Color = accent,
) {
    val foreground = if (highlighted) Color.White else TextSecondary
    Column(
        modifier.background(if (highlighted) TealBright else PanelBackground).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = foreground)
            Box(
                Modifier.size(32.dp).background(accent.copy(alpha = if (highlighted) 0.2f else 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { SmallIcon(icon, accent) }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor)
            Text(unit, modifier = Modifier.weight(1f).padding(bottom = 3.dp), color = foreground, style = MaterialTheme.typography.bodySmall)
        }
        LinearProgressIndicator(
            progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp),
            color = accent, trackColor = if (highlighted) Color.White.copy(alpha = 0.3f) else TrackBackground,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun RegimenProgress(summary: RecordSummary) {
    Surface(shape = RoundedCornerShape(32.dp), color = Color.White) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.progress_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.progress_detail, summary.taken, summary.total), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { summary.adherence }, modifier = Modifier.fillMaxSize(), color = Teal, trackColor = TrackBackground, strokeWidth = 4.dp)
                    Text(stringResource(R.string.percent, summary.percent), color = Teal, style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(
                Modifier.fillMaxWidth().background(TealContainer).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SmallIcon(MediTrackIcons.School, Teal)
                Text(
                    stringResource(if (summary.total == 0) R.string.progress_hint_empty else R.string.progress_hint),
                    style = MaterialTheme.typography.bodyMedium, color = TealInk,
                )
            }
        }
    }
}

@Composable
private fun AcademicFooter() {
    Column(verticalArrangement = Arrangement.spacedBy(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.fillMaxWidth().background(PanelBackground).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallIcon(MediTrackIcons.Shield, Teal)
            Text(stringResource(R.string.footer_project), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Row(
            Modifier.clip(CircleShape).background(PanelBackground).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SmallIcon(MediTrackIcons.School, Teal)
            Text(stringResource(R.string.footer_disclaimer), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1600)
@Composable
private fun HomeScreenPreview() {
    MediTrackTheme { HomeScreen(records = demoMedicationRecords()) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1400)
@Composable
private fun EmptyHomeScreenPreview() {
    MediTrackTheme { HomeScreen(records = emptyList()) }
}
