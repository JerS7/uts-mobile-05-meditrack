package com.example.meditrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord
import com.example.meditrack.ui.state.MedicationFormState
import com.example.meditrack.ui.state.formatTime12h
import com.example.meditrack.ui.state.parseTime12h
import com.example.meditrack.ui.state.rememberMedicationFormState
import com.example.meditrack.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MediTrackScreen(
    modifier: Modifier = Modifier,
    records: List<MedicationRecord> = emptyList(),
    onAddRecord: suspend (MedicationRecord) -> Unit = {},
    searchMedicines: suspend (String) -> List<String> = { emptyList() },
    remindersContent: @Composable (MedicationFormState) -> Unit = {},
) {
    val total = records.size
    val taken = records.count { it.status == DoseStatus.TAKEN }
    val missed = records.count { it.status == DoseStatus.MISSED }
    val adherence = if (total == 0) 0f else taken.toFloat() / total
    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().testTag("medication-list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item(key = "header") { DashboardHeader() }
                item(key = "summary") { SummaryGrid(total, taken, missed, adherence) }
                item(key = "progress") { RegimenProgress(total, taken, adherence) }
                item(key = "form") {
                    MedicationForm(onAddRecord = onAddRecord, searchMedicines = searchMedicines, remindersContent = remindersContent)
                }
                item(key = "records-header") { LoggedRecordsHeader(records.size) }
                if (records.isEmpty()) {
                    item(key = "empty-records") { EmptyRecords() }
                } else {
                    items(records, key = { "record-${it.id}" }, contentType = { "medication-record" }) { record ->
                        RecordCard(record)
                    }
                }
                item(key = "footer") { AcademicFooter() }
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
                Icon(MediTrackIcons.Person, contentDescription = "Ilustrasi Profil", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SmallIcon(MediTrackIcons.Pulse, Teal)
            Text("KESEHATAN RUTINITAS KAMPUS", style = MaterialTheme.typography.labelMedium, color = Teal)
        }
        Text("Pencatat Kepatuhan Minum Obat Pribadi", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text("Jaga agar beban kuliah harian dan resep kesehatan Anda selaras tanpa stres.", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
        Row(
            Modifier.fillMaxWidth().background(TrackBackground.copy(alpha = 0.45f)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallIcon(MediTrackIcons.Info, Teal)
            Text("(! Hanyalah  test, jangan gunakan serius)", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SummaryGrid(total: Int, taken: Int, missed: Int, adherence: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Terdaftar", "$total", "dosis dicatat", if (total == 0) 0f else 1f, MediTrackIcons.Calendar, Teal, Modifier.weight(1f))
            SummaryCard("Diminum", "$taken", "selesai", adherence, MediTrackIcons.Check, TakenGreen, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Terlewat", "$missed", "dosis", if (total == 0) 0f else missed.toFloat() / total, MediTrackIcons.Close, MissedRed, Modifier.weight(1f))
            SummaryCard("Kepatuhan", "${(adherence * 100).toInt()}%", "dari dosis dicatat", adherence, MediTrackIcons.Trend, Color.White, Modifier.weight(1f), highlighted = true)
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
            Text(value, style = MaterialTheme.typography.headlineMedium, color = if (title == "Terdaftar") TextPrimary else accent)
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
private fun RegimenProgress(total: Int, taken: Int, adherence: Float) {
    Surface(shape = RoundedCornerShape(32.dp), color = Color.White) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Progres Konsumsi Obat", style = MaterialTheme.typography.titleSmall)
                    Text("$taken dari $total dosis terdaftar telah diminum", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { adherence }, modifier = Modifier.fillMaxSize(), color = Teal, trackColor = TrackBackground, strokeWidth = 4.dp)
                    Text("${(adherence * 100).toInt()}%", color = Teal, style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(
                Modifier.fillMaxWidth().background(TealContainer).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SmallIcon(MediTrackIcons.School, Teal)
                Text(if (total == 0) "Tambahkan catatan obat untuk melihat progres Anda." else "Progres didasarkan pada dosis yang Anda catat.", style = MaterialTheme.typography.bodyMedium, color = TealInk)
            }
        }
    }
}

@Composable
private fun MedicationForm(
    onAddRecord: suspend (MedicationRecord) -> Unit,
    searchMedicines: suspend (String) -> List<String>,
    remindersContent: @Composable (MedicationFormState) -> Unit,
    form: MedicationFormState = rememberMedicationFormState(),
) {
    val taken = form.status == DoseStatus.TAKEN
    var savedMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showNameSuggestions by rememberSaveable { mutableStateOf(false) }
    var nameFocused by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchFailed by remember { mutableStateOf(false) }
    LaunchedEffect(form.name, nameFocused, showNameSuggestions) {
        suggestions = emptyList()
        searchFailed = false
        searching = false
        if (nameFocused && showNameSuggestions && form.name.trim().length >= 2) {
            searching = true
            try {
                delay(200)
                suggestions = searchMedicines(form.name)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                searchFailed = true
            } finally {
                searching = false
            }
        }
    }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    if (saving) {
        AlertDialog(onDismissRequest = {}, confirmButton = {}, text = { Text("Menyimpan catatan…") })
    }
    Surface(shape = RoundedCornerShape(32.dp), color = PanelBackground) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallIcon(MediTrackIcons.Add, Teal)
                Text("Catat Konsumsi Obat", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FormField(
                    "Nama Obat atau Suplemen", form.name,
                    { form.updateName(it); showNameSuggestions = true; savedMessage = null },
                    "contoh: Parasetamol", icon = MediTrackIcons.Pill,
                    error = form.nameError.takeIf { form.showErrors },
                    onFocusChanged = { nameFocused = it; if (it) showNameSuggestions = true },
                )
                if (showNameSuggestions && nameFocused && form.name.trim().length >= 2) {
                    suggestions.forEach { suggestion ->
                        Text(
                            suggestion,
                            modifier = Modifier.fillMaxWidth().background(Color.White)
                                .clickable { form.updateName(suggestion); showNameSuggestions = false }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    if (searching) Text("Mencari nama obat…", style = MaterialTheme.typography.bodySmall)
                    else if (searchFailed) Text("Pencarian nama tidak tersedia. Anda tetap dapat memasukkan nama secara manual.", style = MaterialTheme.typography.bodySmall)
                    else if (suggestions.isEmpty()) Text("Tidak ada nama yang cocok. Anda tetap dapat memasukkan nama sendiri.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField(
                    "Waktu Jadwal", form.scheduledTime, { form.scheduledTime = it }, Modifier.weight(1f),
                    error = form.scheduledTimeError.takeIf { form.showErrors },
                )
                TimeField(
                    "Waktu Aktual", form.actualTime, form::updateActualTime, Modifier.weight(1f),
                    error = form.actualTimeError.takeIf { form.showErrors },
                    enabled = form.isActualTimeEnabled, disabledText = "Belum diminum",
                )
            }
            remindersContent(form)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Status Dosis", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = taken, onClick = { form.selectStatus(DoseStatus.TAKEN); savedMessage = null },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = Teal, activeContentColor = Color.White, inactiveContainerColor = TrackBackground.copy(alpha = 0.4f), inactiveBorderColor = Color.Transparent, activeBorderColor = Color.Transparent),
                        icon = { SmallIcon(MediTrackIcons.Check, if (taken) Color.White else TextSecondary) },
                    ) { Text("Diminum") }
                    SegmentedButton(
                        selected = !taken, onClick = { form.selectStatus(DoseStatus.MISSED); savedMessage = null },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = MissedRed, activeContentColor = Color.White, inactiveContainerColor = TrackBackground.copy(alpha = 0.4f), inactiveBorderColor = Color.Transparent, activeBorderColor = Color.Transparent),
                        icon = { SmallIcon(MediTrackIcons.Close, if (!taken) Color.White else TextSecondary) },
                    ) { Text("Terlewat") }
                }
                if (!taken) {
                    Text("Dosis yang terlewat disimpan tanpa waktu aktual.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            FormField("Catatan / Rutinitas Kampus (opsional)", form.note, { form.updateNote(it) }, "contoh: Diminum saat sarapan sebelum kuliah jam 9 pagi di Ruang B", multiline = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { form.reset(); showNameSuggestions = false; savedMessage = null }) {
                    SmallIcon(MediTrackIcons.Reset, TextSecondary)
                    Text("Reset", color = TextSecondary)
                }
                Button(
                    onClick = {
                        val record = form.submit(resetAfterSubmit = false)
                        if (record != null) {
                            saving = true
                            saveError = false
                            savedMessage = null
                            scope.launch {
                                try {
                                    onAddRecord(record)
                                    form.reset()
                                    showNameSuggestions = false
                                    savedMessage = "${record.name} berhasil disimpan di perangkat ini."
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    saveError = true
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    },
                    enabled = !saving,
                    colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White),
                ) {
                    SmallIcon(MediTrackIcons.Add, Color.White)
                    Text("Tambah Catatan")
                }
            }
            savedMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TakenGreen)
            }
            if (saveError) Text("Gagal menyimpan. Data input Anda masih ada. Silakan coba lagi.", color = MissedRed)
        }
    }
}

@Composable
private fun TimeField(
    label: String, value: String?, onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier, error: String? = null,
    enabled: Boolean = true, disabledText: String = "--",
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Row(
            Modifier.fillMaxWidth().background(if (enabled) Color.White else TrackBackground.copy(alpha = 0.4f))
                .clickable(enabled = enabled, onClickLabel = "Pilih $label") { showPicker = true }
                .padding(16.dp)
                .semantics { contentDescription = "$label: ${if (enabled) value ?: "belum diatur" else disabledText}" },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SmallIcon(MediTrackIcons.Clock, TextSecondary)
            Text(
                if (!enabled) disabledText else value ?: "Pilih",
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled && value != null) TextPrimary else TextSecondary.copy(alpha = 0.65f),
            )
        }
        if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = MissedRed)
    }
    if (showPicker && enabled) {
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = TextSecondary) } },
    )
}

@Composable
private fun FormField(
    label: String, value: String, onValueChange: (String) -> Unit, placeholder: String,
    modifier: Modifier = Modifier, icon: ImageVector? = null, multiline: Boolean = false,
    error: String? = null,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        BasicTextField(
            value = value, onValueChange = onValueChange,
            singleLine = !multiline, minLines = if (multiline) 2 else 1,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            modifier = Modifier.onFocusChanged { onFocusChanged(it.isFocused) }.fillMaxWidth().background(Color.White).padding(16.dp)
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
private fun LoggedRecordsHeader(count: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Catatan Riwayat", modifier = Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        Text("$count Catatan", modifier = Modifier.background(TrackBackground, CircleShape).padding(horizontal = 8.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
                    Text(if (taken) "Diminum" else "Terlewat", style = MaterialTheme.typography.labelSmall, color = statusColor)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Jadwal: ${record.scheduledTime}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("Aktual: ${record.actualTime ?: "--"}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = statusColor)
            }
            if (record.note.isNotBlank()) Text("\"${record.note}\"", modifier = Modifier.fillMaxWidth().background(PanelBackground).padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
        Text("Belum ada catatan", style = MaterialTheme.typography.titleMedium)
        Text("Catatan riwayat obat harian Anda akan muncul di sini.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
            Text("Proyek Akademik MediTrack: UTS Sementara untuk kuliah Pemrograman Bergerak", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Row(
            Modifier.clip(CircleShape).background(PanelBackground).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SmallIcon(MediTrackIcons.School, Teal)
            Text("Bukan aplikasi resmi, jangan gunakan untuk kebutuhan medis", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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
