package com.example.meditrack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.meditrack.R
import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord
import com.example.meditrack.ui.state.MedicationFormState
import com.example.meditrack.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Form page for logging a new dose or editing an existing record. [onSaved] runs only
 * after [onSave] has committed; on failure the input stays in the form.
 */
@Composable
fun LogScreen(
    form: MedicationFormState,
    modifier: Modifier = Modifier,
    onSave: suspend (MedicationRecord) -> Unit = {},
    onSaved: (record: MedicationRecord, wasEditing: Boolean) -> Unit = { _, _ -> },
    onCancelEdit: () -> Unit = {},
    searchMedicines: suspend (String) -> List<String> = { emptyList() },
) {
    val taken = form.status == DoseStatus.TAKEN
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    if (saving) {
        AlertDialog(onDismissRequest = {}, confirmButton = {}, text = { Text(stringResource(R.string.saving_record)) })
    }
    fun error(id: Int?) = id.takeIf { form.showErrors }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("log-form")
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageTitle(
            stringResource(if (form.isEditing) R.string.log_title_edit else R.string.log_title),
            stringResource(if (form.isEditing) R.string.log_subtitle_edit else R.string.log_subtitle),
        )
        Surface(shape = RoundedCornerShape(32.dp), color = PanelBackground) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallIcon(MediTrackIcons.Add, Teal)
                    Text(stringResource(R.string.log_form_heading), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                }
                MedicineNameField(
                    value = form.name,
                    onValueChange = { form.updateName(it); saveError = false },
                    searchMedicines = searchMedicines,
                    error = error(form.nameError)?.let { stringResource(it) },
                )
                DateField(
                    stringResource(R.string.field_date), form.date, { form.updateDate(it); saveError = false },
                    error = error(form.dateError)?.let { stringResource(it) },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeField(
                        stringResource(R.string.field_scheduled_time), form.scheduledTime, { form.scheduledTime = it }, Modifier.weight(1f),
                        error = error(form.scheduledTimeError)?.let { stringResource(it) },
                    )
                    TimeField(
                        stringResource(R.string.field_actual_time), form.actualTime, form::updateActualTime, Modifier.weight(1f),
                        error = error(form.actualTimeError)?.let { stringResource(it) },
                        enabled = form.isActualTimeEnabled, disabledText = stringResource(R.string.field_actual_time_disabled),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.field_status), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = taken, onClick = { form.selectStatus(DoseStatus.TAKEN) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            colors = SegmentedButtonDefaults.colors(activeContainerColor = Teal, activeContentColor = Color.White, inactiveContainerColor = TrackBackground.copy(alpha = 0.4f), inactiveBorderColor = Color.Transparent, activeBorderColor = Color.Transparent),
                            icon = { SmallIcon(MediTrackIcons.Check, if (taken) Color.White else TextSecondary) },
                        ) { Text(stringResource(R.string.status_taken)) }
                        SegmentedButton(
                            selected = !taken, onClick = { form.selectStatus(DoseStatus.MISSED) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            colors = SegmentedButtonDefaults.colors(activeContainerColor = MissedRed, activeContentColor = Color.White, inactiveContainerColor = TrackBackground.copy(alpha = 0.4f), inactiveBorderColor = Color.Transparent, activeBorderColor = Color.Transparent),
                            icon = { SmallIcon(MediTrackIcons.Close, if (!taken) Color.White else TextSecondary) },
                        ) { Text(stringResource(R.string.status_missed)) }
                    }
                    if (!taken) {
                        Text(stringResource(R.string.missed_hint), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                FormField(
                    stringResource(R.string.field_note), form.note, { form.updateNote(it); saveError = false },
                    stringResource(R.string.field_note_hint), multiline = true,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (form.isEditing) {
                        TextButton(onClick = { form.reset(); saveError = false; onCancelEdit() }) {
                            Text(stringResource(R.string.cancel), color = TextSecondary)
                        }
                    } else {
                        TextButton(onClick = { form.reset(); saveError = false }) {
                            SmallIcon(MediTrackIcons.Reset, TextSecondary)
                            Text(stringResource(R.string.action_reset), color = TextSecondary)
                        }
                    }
                    Button(
                        onClick = {
                            val wasEditing = form.isEditing
                            val record = form.submit(resetAfterSubmit = false)
                            if (record != null) {
                                saving = true
                                saveError = false
                                scope.launch {
                                    try {
                                        onSave(record)
                                        form.reset()
                                        onSaved(record, wasEditing)
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
                        SmallIcon(if (form.isEditing) MediTrackIcons.Check else MediTrackIcons.Add, Color.White)
                        Text(stringResource(if (form.isEditing) R.string.action_save_changes else R.string.action_add_record))
                    }
                }
                if (saveError) Text(stringResource(R.string.save_failed), color = MissedRed)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun LogScreenPreview() {
    MediTrackTheme { LogScreen(form = MedicationFormState()) }
}
