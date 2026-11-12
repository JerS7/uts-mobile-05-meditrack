package com.example.meditrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.meditrack.R
import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord
import com.example.meditrack.ui.state.formatTime12h
import com.example.meditrack.ui.state.parseTime12h
import com.example.meditrack.ui.theme.*
import com.example.meditrack.util.Dates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
internal fun SmallIcon(icon: ImageVector, tint: Color) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = tint)
}

@Composable
internal fun PageTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
internal fun FormField(
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

/** Medicine name field with suggestions from the local reference catalog. */
@Composable
internal fun MedicineNameField(
    value: String,
    onValueChange: (String) -> Unit,
    searchMedicines: suspend (String) -> List<String>,
    error: String? = null,
) {
    var showSuggestions by rememberSaveable { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchFailed by remember { mutableStateOf(false) }
    LaunchedEffect(value, focused, showSuggestions) {
        suggestions = emptyList()
        searchFailed = false
        searching = false
        if (focused && showSuggestions && value.trim().length >= 2) {
            searching = true
            try {
                delay(200)
                suggestions = searchMedicines(value)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                searchFailed = true
            } finally {
                searching = false
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FormField(
            stringResource(R.string.field_medicine_name), value,
            { onValueChange(it); showSuggestions = true },
            stringResource(R.string.field_medicine_name_hint), icon = MediTrackIcons.Pill,
            error = error,
            onFocusChanged = { focused = it },
        )
        if (showSuggestions && focused && value.trim().length >= 2) {
            suggestions.forEach { suggestion ->
                Text(
                    suggestion,
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                        .clickable { onValueChange(suggestion); showSuggestions = false }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            val hint = when {
                searching -> R.string.search_in_progress
                searchFailed -> R.string.search_failed
                suggestions.isEmpty() -> R.string.search_no_match
                else -> null
            }
            if (hint != null) Text(stringResource(hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** A field that opens a picker dialog; shared by the time and date fields. */
@Composable
private fun PickerField(
    label: String, text: String, hasValue: Boolean, icon: ImageVector,
    onClick: () -> Unit, modifier: Modifier = Modifier, error: String? = null, enabled: Boolean = true,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Row(
            Modifier.fillMaxWidth().background(if (enabled) Color.White else TrackBackground.copy(alpha = 0.4f))
                .clickable(enabled = enabled, onClickLabel = stringResource(R.string.pick_value, label), onClick = onClick)
                .padding(16.dp)
                .semantics { contentDescription = "$label: $text" },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SmallIcon(icon, TextSecondary)
            Text(
                text, style = MaterialTheme.typography.bodyLarge,
                color = if (enabled && hasValue) TextPrimary else TextSecondary.copy(alpha = 0.65f),
            )
        }
        if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = MissedRed)
    }
}

@Composable
internal fun TimeField(
    label: String, value: String?, onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier, error: String? = null,
    enabled: Boolean = true, disabledText: String = "--",
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    PickerField(
        label = label,
        text = if (!enabled) disabledText else value ?: stringResource(R.string.pick),
        hasValue = value != null, icon = MediTrackIcons.Clock,
        onClick = { showPicker = true }, modifier = modifier, error = error, enabled = enabled,
    )
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
            TextButton(onClick = { onConfirm(formatTime12h(pickerState.hour, pickerState.minute)) }) { Text(stringResource(R.string.ok), color = Teal) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = TextSecondary) } },
    )
}

/** Picks a day up to [latestDate] (inclusive). Dates are yyyy-MM-dd text. */
@Composable
internal fun DateField(
    label: String, value: String?, onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier, error: String? = null, emptyText: String? = null,
    latestDate: String = Dates.today(),
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    PickerField(
        label = label,
        text = value?.let { Dates.display(it) } ?: emptyText ?: stringResource(R.string.pick),
        hasValue = value != null, icon = MediTrackIcons.Calendar,
        onClick = { showPicker = true }, modifier = modifier, error = error,
    )
    if (showPicker) {
        MediDatePickerDialog(
            initialDate = value, latestDate = latestDate,
            onDismiss = { showPicker = false },
            onConfirm = { onDateSelected(it); showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediDatePickerDialog(
    initialDate: String?, latestDate: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit,
) {
    val latest = Dates.toUtcMillis(latestDate)
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.let(Dates::toUtcMillis) ?: latest,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = latest == null || utcTimeMillis <= latest
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(Dates.fromUtcMillis(it)) } },
                enabled = state.selectedDateMillis != null,
            ) { Text(stringResource(R.string.ok), color = Teal) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = TextSecondary) } },
    ) { DatePicker(state = state) }
}

/** One record. Edit and delete buttons appear only when their callbacks are given. */
@Composable
internal fun RecordCard(
    record: MedicationRecord,
    showDate: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
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
                    Text(stringResource(if (taken) R.string.status_taken else R.string.status_missed), style = MaterialTheme.typography.labelSmall, color = statusColor)
                }
            }
            if (showDate) Text(Dates.display(record.date), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.record_scheduled, record.scheduledTime), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(stringResource(R.string.record_actual, record.actualTime ?: "--"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = statusColor)
            }
            if (record.note.isNotBlank()) Text(stringResource(R.string.record_note, record.note), modifier = Modifier.fillMaxWidth().background(PanelBackground).padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            if (onEdit != null || onDelete != null) {
                val editLabel = stringResource(R.string.action_edit_named, record.name)
                val deleteLabel = stringResource(R.string.action_delete_named, record.name)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (onEdit != null) TextButton(onClick = onEdit, modifier = Modifier.semantics {
                        contentDescription = editLabel
                    }) { Text(stringResource(R.string.action_edit), color = Teal) }
                    if (onDelete != null) TextButton(onClick = onDelete, modifier = Modifier.semantics {
                        contentDescription = deleteLabel
                    }) { Text(stringResource(R.string.action_delete), color = MissedRed) }
                }
            }
        }
    }
}

@Composable
internal fun RecordCountHeader(title: String, count: Int, countText: String = stringResource(R.string.record_count, count)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, modifier = Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        Text(countText, modifier = Modifier.background(TrackBackground, CircleShape).padding(horizontal = 8.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

/** Empty message with optional buttons, for example "Catat obat" and "Isi data contoh". */
@Composable
internal fun EmptyRecords(
    title: String = stringResource(R.string.empty_title),
    message: String = stringResource(R.string.empty_message),
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SmallIcon(MediTrackIcons.Pill, Teal)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
        actions()
    }
}
