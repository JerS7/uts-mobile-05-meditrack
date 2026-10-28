package com.example.meditrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord
import com.example.meditrack.ui.state.MediTrackState
import com.example.meditrack.ui.state.rememberMediTrackState
import com.example.meditrack.ui.theme.*
import androidx.compose.foundation.clickable
import com.example.meditrack.ui.state.MedicationFormState
import com.example.meditrack.ui.state.formatTime12h
import com.example.meditrack.ui.state.parseTime12h
import com.example.meditrack.ui.state.rememberMedicationFormState

/** Stage one: records come from session state; summary is still a wireframe fixture. */
@Composable
fun MediTrackScreen(
    modifier: Modifier = Modifier,
    state: MediTrackState = rememberMediTrackState(initialRecords = sampleMedicationRecords),
) {
    var showEmptyView by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                DashboardHeader()
                SummaryGrid()
                RegimenProgress()
                MedicationForm(onAddRecord = state::addRecord)
                LoggedRecords(
                    records = state.records,
                    showEmptyView = showEmptyView,
                    onToggleEmpty = { showEmptyView = !showEmptyView },
                )
                AcademicFooter()
            }
        }
    }
}

@Composable
private fun DashboardHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(32.dp).background(Teal, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(MediTrackIcons.Pill, contentDescription = "MediTrack", tint = Color.White, modifier = Modifier.size(21.dp))
            }
            Box(Modifier.size(32.dp).background(Teal, CircleShape), contentAlignment = Alignment.Center) {
                Icon(MediTrackIcons.Person, contentDescription = "Profile illustration", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SmallIcon(MediTrackIcons.Pulse, Teal)
            Text("CAMPUS ROUTINE WELLNESS", style = MaterialTheme.typography.labelMedium, color = Teal)
        }
        Text("Personal Medication Adherence Logger", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text("Keep your daily course load and health regimen seamlessly aligned through stress-free tracking.", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
        Row(
            Modifier.fillMaxWidth().background(TrackBackground.copy(alpha = 0.45f)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallIcon(MediTrackIcons.Info, Teal)
            Text("Campus Health Project: Educational tracking tool only. No clinical or dosage advice provided.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SummaryGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Scheduled", "4", "doses", 1f, MediTrackIcons.Calendar, Teal, Modifier.weight(1f))
            SummaryCard("Taken", "3", "completed", 0.75f, MediTrackIcons.Check, TakenGreen, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Missed", "1", "dose", 0.25f, MediTrackIcons.Close, MissedRed, Modifier.weight(1f))
            SummaryCard("Adherence", "75%", "daily score", 0.75f, MediTrackIcons.Trend, Color.White, Modifier.weight(1f), highlighted = true)
        }
    }
}

@Composable
private fun SummaryCard(
    title: String, value: String, unit: String, progress: Float,
    icon: ImageVector, accent: Color, modifier: Modifier = Modifier, highlighted: Boolean = false,
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
            Text(value, style = MaterialTheme.typography.headlineMedium, color = if (title == "Scheduled") TextPrimary else accent)
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
private fun RegimenProgress() {
    Surface(shape = RoundedCornerShape(32.dp), color = Color.White) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Today's Regimen Progress", style = MaterialTheme.typography.titleSmall)
                    Text("3 of 4 planned items completed", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { 0.75f }, modifier = Modifier.fillMaxSize(), color = Teal, trackColor = TrackBackground, strokeWidth = 4.dp)
                    Text("75%", color = Teal, style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(
                Modifier.fillMaxWidth().background(TealContainer).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SmallIcon(MediTrackIcons.School, Teal)
                Text("Midterms Check-in: Excellent consistency today! Keep water nearby during intense library study sessions.", style = MaterialTheme.typography.bodyMedium, color = TealInk)
            }
        }
    }
}

@Composable
private fun MedicationForm(
    onAddRecord: (MedicationRecord) -> Unit,
    form: MedicationFormState = rememberMedicationFormState(),
) {
    var taken by rememberSaveable { mutableStateOf(true) }
    var savedMessage by rememberSaveable { mutableStateOf<String?>(null) }
    Surface(shape = RoundedCornerShape(32.dp), color = PanelBackground) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallIcon(MediTrackIcons.Add, Teal)
                Text("Log Medication Record", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            }
            FormField(
                "Medication or Supplement Name", form.name, { form.updateName(it); savedMessage = null }, "e.g., Multivitamin",
                icon = MediTrackIcons.Pill, error = form.nameError.takeIf { form.showErrors },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField(
                    "Scheduled Time", form.scheduledTime, { form.scheduledTime = it }, Modifier.weight(1f),
                    error = form.scheduledTimeError.takeIf { form.showErrors },
                )
                TimeField(
                    "Actual Time", form.actualTime, { form.actualTime = it }, Modifier.weight(1f),
                    error = form.actualTimeError.takeIf { form.showErrors },
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Dose Status", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = taken, onClick = { taken = true },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = Teal, activeContentColor = Color.White, inactiveContainerColor = TrackBackground.copy(alpha = 0.4f), inactiveBorderColor = Color.Transparent, activeBorderColor = Color.Transparent),
                        icon = { SmallIcon(MediTrackIcons.Check, if (taken) Color.White else TextSecondary) },
                    ) { Text("Taken") }
                    SegmentedButton(
                        selected = !taken, onClick = { taken = false },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = MissedRed, activeContentColor = Color.White, inactiveContainerColor = TrackBackground.copy(alpha = 0.4f), inactiveBorderColor = Color.Transparent, activeBorderColor = Color.Transparent),
                        icon = { SmallIcon(MediTrackIcons.Close, if (!taken) Color.White else TextSecondary) },
                    ) { Text("Missed") }
                }
            }
            FormField("Context / Campus Routine Note (optional)", form.note, { form.updateNote(it) }, "e.g., Taken with breakfast before 9 AM lecture in Hall B", multiline = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        val record = form.submit(if (taken) DoseStatus.TAKEN else DoseStatus.MISSED)
                        if (record != null) {
                            onAddRecord(record)
                            savedMessage = "${record.name} added to today's records."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White),
                ) {
                    SmallIcon(MediTrackIcons.Add, Color.White)
                    Text("Add Record")
                }
            }
            savedMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TakenGreen)
            }
        }
    }
}

@Composable
private fun TimeField(
    label: String, value: String?, onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier, error: String? = null,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Row(
            Modifier.fillMaxWidth().background(Color.White)
                .clickable(onClickLabel = "Choose $label") { showPicker = true }
                .padding(16.dp)
                .semantics { contentDescription = "$label: ${value ?: "not set"}" },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SmallIcon(MediTrackIcons.Clock, TextSecondary)
            Text(
                value ?: "Select",
                style = MaterialTheme.typography.bodyLarge,
                color = if (value != null) TextPrimary else TextSecondary.copy(alpha = 0.65f),
            )
        }
        if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = MissedRed)
    }
    if (showPicker) {
        MediTimePickerDialog(
            title = label, initialTime = value,
            onDismiss = { showPicker = false },
            onConfirm = { onTimeSelected(it); showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediTimePickerDialog(
    title: String, initialTime: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit,
) {
    val (hour, minute) = initialTime?.let(::parseTime12h) ?: (8 to 0)
    val pickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(formatTime12h(pickerState.hour, pickerState.minute)) }) { Text("OK", color = Teal) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
    )
}

@Composable
private fun FormField(
    label: String, value: String, onValueChange: (String) -> Unit, placeholder: String,
    modifier: Modifier = Modifier, icon: ImageVector? = null, multiline: Boolean = false,
    error: String? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        BasicTextField(
            value = value, onValueChange = onValueChange,
            singleLine = !multiline, minLines = if (multiline) 2 else 1,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)
                .semantics { contentDescription = label },
            decorationBox = { innerTextField ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) SmallIcon(icon, TextSecondary)
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = TextSecondary.copy(alpha = 0.65f))
                        innerTextField()
                    }
                }
            },
        )
        if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = MissedRed)
    }
}

@Composable
private fun LoggedRecords(records: List<MedicationRecord>, showEmptyView: Boolean, onToggleEmpty: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Today's Logged Records", modifier = Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            Text(if (showEmptyView) "0 Records" else "${records.size} Records",  modifier = Modifier.background(TrackBackground, CircleShape).padding(horizontal = 8.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            TextButton(onClick = onToggleEmpty, contentPadding = PaddingValues(horizontal = 4.dp)) {
                SmallIcon(MediTrackIcons.Eye, Teal)
                Spacer(Modifier.width(4.dp))
                Text(if (showEmptyView) "Show\nRecords" else "Toggle\nEmpty View", style = MaterialTheme.typography.labelSmall)
            }
        }
        if (showEmptyView || records.isEmpty()) {
            EmptyRecords()
        } else {
            records.forEach { RecordCard(it) }
        }
    }
}

@Composable
private fun RecordCard(record: MedicationRecord) {
    val taken = record.status == DoseStatus.TAKEN
    val statusColor = if (taken) TakenGreen else MissedRed
    Row(Modifier.fillMaxWidth().background(Color.White).height(IntrinsicSize.Min)) {
        Spacer(Modifier.width(4.dp).fillMaxHeight().background(statusColor))
        Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(record.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Row(
                    Modifier.background(if (taken) TakenContainer else MissedContainer, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (taken) MediTrackIcons.Check else MediTrackIcons.Close, null, Modifier.size(12.dp), tint = statusColor)
                    Text(if (taken) "Taken" else "Missed", style = MaterialTheme.typography.labelSmall, color = statusColor)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sched: ${record.scheduledTime}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("Actual: ${record.actualTime ?: "--"}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = statusColor)
            }
            Text("\"${record.note}\"", modifier = Modifier.fillMaxWidth().background(PanelBackground).padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun EmptyRecords() {
    Column(
        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SmallIcon(MediTrackIcons.Pill, Teal)
        Text("No records yet", style = MaterialTheme.typography.titleMedium)
        Text("Your daily medication records will appear here.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
            Text("MediTrack Academic Project: UTS Sementara untuk kuliah Pemograman Bergerak", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Row(
            Modifier.clip(CircleShape).background(PanelBackground).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SmallIcon(MediTrackIcons.School, Teal)
            Text("Not a true app, do not use as a need", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun SmallIcon(icon: ImageVector, tint: Color) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = tint)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1800)
@Composable
private fun MediTrackScreenPreview() {
    MediTrackTheme { MediTrackScreen() }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun EmptyRecordsPreview() {
    MediTrackTheme { EmptyRecords() }
}
