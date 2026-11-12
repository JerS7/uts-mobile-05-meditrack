package com.example.meditrack.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.meditrack.R
import com.example.meditrack.reminders.MedicationReminder
import com.example.meditrack.reminders.ReminderManager
import com.example.meditrack.ui.state.MAX_NAME_LENGTH
import com.example.meditrack.ui.state.formatTime12h
import com.example.meditrack.ui.state.parseTime12h
import com.example.meditrack.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Daily reminders with their own name and time fields, separate from the record form. */
@Composable
fun ReminderScreen(
    modifier: Modifier = Modifier,
    searchMedicines: suspend (String) -> List<String> = { emptyList() },
) {
    val context = LocalContext.current
    val manager = remember(context) { ReminderManager.get(context) }
    val scope = rememberCoroutineScope()
    var reminders by remember { mutableStateOf<List<MedicationReminder>?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var notificationsAllowed by remember { mutableStateOf(manager.notificationsAllowed()) }
    var exactAllowed by remember { mutableStateOf(manager.exactAllowed()) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }

    // The reminder being edited keeps its ID and on/off state when it is saved again.
    var name by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingEnabled by rememberSaveable { mutableStateOf(true) }
    fun clearForm() { name = ""; time = null; editingId = null; editingEnabled = true }

    val failedUpdate = stringResource(R.string.reminder_update_failed)
    val missingInput = stringResource(R.string.reminder_missing_input)
    val savedText = stringResource(R.string.reminder_saved)
    val savedNoNotificationText = stringResource(R.string.reminder_saved_no_notifications)
    val editHint = stringResource(R.string.reminder_edit_hint)

    fun refreshPermissions() {
        notificationsAllowed = manager.notificationsAllowed()
        exactAllowed = manager.exactAllowed()
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshPermissions()
    }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshPermissions()
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, manager) {
        val observer = LifecycleEventObserver { _, event ->
            // MainActivity reschedules alarms on resume; here only the notices need refreshing.
            if (event == Lifecycle.Event.ON_RESUME) refreshPermissions()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(manager, attempt) {
        loadError = false
        try {
            manager.reminders.collect { reminders = it }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { loadError = true }
    }
    fun change(action: suspend () -> Unit, success: () -> Unit = {}) {
        working = true; error = null; message = null
        scope.launch {
            try { action(); success() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = failedUpdate }
            finally { working = false }
        }
    }
    if (working) AlertDialog(onDismissRequest = {}, confirmButton = {}, text = { Text(stringResource(R.string.reminder_working)) })

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("reminder-page")
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageTitle(stringResource(R.string.reminder_title), stringResource(R.string.reminder_subtitle))
        if (!notificationsAllowed || !exactAllowed) {
            Surface(shape = RoundedCornerShape(16.dp), color = MissedContainer) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!notificationsAllowed) {
                        Text(stringResource(R.string.reminder_notifications_off))
                        if (Build.VERSION.SDK_INT >= 33) TextButton(onClick = {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }) { Text(stringResource(R.string.reminder_allow_notifications)) }
                        TextButton(onClick = {
                            val intent = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                            settings.launch(intent)
                        }) { Text(stringResource(R.string.reminder_notification_settings)) }
                    }
                    if (!exactAllowed) {
                        Text(stringResource(R.string.reminder_exact_off), style = MaterialTheme.typography.bodySmall)
                        if (Build.VERSION.SDK_INT >= 31) TextButton(onClick = {
                            settings.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                        }) { Text(stringResource(R.string.reminder_allow_exact)) }
                    }
                }
            }
        }
        Surface(shape = RoundedCornerShape(32.dp), color = PanelBackground) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(if (editingId == null) R.string.reminder_form_new else R.string.reminder_form_edit),
                    style = MaterialTheme.typography.titleMedium,
                )
                MedicineNameField(
                    value = name,
                    onValueChange = { name = it.take(MAX_NAME_LENGTH); error = null; message = null },
                    searchMedicines = searchMedicines,
                )
                TimeField(stringResource(R.string.field_reminder_time), time, { time = it; error = null; message = null })
                Text(stringResource(R.string.reminder_notification_hint), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !working && reminders != null && !loadError,
                        colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White),
                        onClick = {
                            val parsed = time?.let(::parseTime12h)
                            if (name.isBlank() || parsed == null) {
                                error = missingInput
                            } else {
                                val reminder = MedicationReminder(
                                    medicine = name.trim(), hour = parsed.first, minute = parsed.second, enabled = editingEnabled,
                                ).let { r -> editingId?.let { r.copy(id = it) } ?: r }
                                change({ manager.save(reminder) }) {
                                    clearForm()
                                    message = if (notificationsAllowed) savedText else savedNoNotificationText
                                }
                            }
                        },
                    ) { Text(stringResource(if (editingId == null) R.string.reminder_save else R.string.reminder_update)) }
                    if (editingId != null) TextButton(onClick = { clearForm(); message = null; error = null }) {
                        Text(stringResource(R.string.cancel), color = TextSecondary)
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                message?.let { Text(it, color = TakenGreen) }
            }
        }
        val reminderCount = reminders?.size ?: 0
        RecordCountHeader(
            stringResource(R.string.reminder_list_title), reminderCount,
            countText = stringResource(R.string.reminder_count, reminderCount),
        )
        when {
            loadError -> {
                Text(stringResource(R.string.reminder_load_failed))
                TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
            }
            reminders == null -> CircularProgressIndicator()
            reminders!!.isEmpty() -> Text(stringResource(R.string.reminder_empty), style = MaterialTheme.typography.bodyMedium)
            else -> reminders!!.forEach { reminder ->
                val label = formatTime12h(reminder.hour, reminder.minute)
                val switchLabel = stringResource(R.string.reminder_switch_label, reminder.medicine, label)
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(reminder.medicine, style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.reminder_daily_at, label), color = TextSecondary)
                            }
                            Switch(checked = reminder.enabled, enabled = !working,
                                modifier = Modifier.semantics { contentDescription = switchLabel },
                                onCheckedChange = { enabled ->
                                    change({ manager.save(reminder.copy(enabled = enabled)) }) {
                                        if (editingId == reminder.id) editingEnabled = enabled
                                    }
                                })
                        }
                        Row {
                            TextButton(enabled = !working, onClick = {
                                editingId = reminder.id
                                editingEnabled = reminder.enabled
                                name = reminder.medicine
                                time = label
                                error = null
                                message = editHint
                            }) { Text(stringResource(R.string.action_edit), color = Teal) }
                            TextButton(enabled = !working, onClick = {
                                change({ manager.delete(reminder.id) }) { if (editingId == reminder.id) clearForm() }
                            }) { Text(stringResource(R.string.action_delete), color = MissedRed) }
                        }
                    }
                }
            }
        }
    }
}
