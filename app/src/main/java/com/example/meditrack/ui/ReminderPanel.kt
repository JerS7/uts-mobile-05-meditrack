package com.example.meditrack.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.meditrack.reminders.MedicationReminder
import com.example.meditrack.reminders.ReminderManager
import com.example.meditrack.ui.state.formatTime12h
import com.example.meditrack.ui.state.parseTime12h
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ReminderPanel(
    medicineName: String,
    scheduledTime: String?,
    onEdit: (name: String, scheduledTime: String) -> Unit,
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
    val time = scheduledTime?.let(::parseTime12h)
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(medicineName, scheduledTime) {
        error = null
        message = null
        if (medicineName.isEmpty() && scheduledTime == null) editingId = null
    }

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
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
                scope.launch {
                    try { manager.reschedule() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { error = "Unable to schedule reminders. Please reopen this screen to retry." }
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(manager, attempt) {
        loadError = false
        try {
            manager.reschedule()
            manager.reminders.collect { reminders = it }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { loadError = true }
    }
    fun change(action: suspend () -> Unit, success: () -> Unit = {}) {
        working = true; error = null; message = null
        scope.launch {
            try { action(); success() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Unable to update the reminder. Please try again." }
            finally { working = false }
        }
    }
    if (working) AlertDialog(onDismissRequest = {}, confirmButton = {}, text = { Text("Updating reminder…") })

    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Daily reminder (optional)", style = MaterialTheme.typography.titleSmall)
            Text("Uses the medication name and Scheduled Time above. Save a reminder now; use Add Record to log a taken or missed dose.", style = MaterialTheme.typography.bodySmall)
            if (medicineName.isNotBlank() && time != null) {
                Text("${medicineName.trim()} • Every day at $scheduledTime", style = MaterialTheme.typography.bodyMedium)
            }
            if (!notificationsAllowed) {
                Text("Notifications are off. Enable them to receive medication reminders.")
                if (Build.VERSION.SDK_INT >= 33) TextButton(onClick = {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("Allow notifications") }
                TextButton(onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    settings.launch(intent)
                }) { Text("Notification settings") }
            }
            if (!exactAllowed) {
                Text("Without precise alarm access, Android may delay reminders.", style = MaterialTheme.typography.bodySmall)
                if (Build.VERSION.SDK_INT >= 31) TextButton(onClick = {
                    settings.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }) { Text("Allow precise reminders") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !working && reminders != null && !loadError, onClick = {
                    if (medicineName.isBlank() || time == null) {
                        error = "Enter the medication name and Scheduled Time above."
                    } else {
                        val reminder = MedicationReminder(medicine = medicineName.trim(), hour = time.first, minute = time.second)
                            .let { if (editingId == null) it else it.copy(id = editingId!!) }
                        change({ manager.save(reminder) }) {
                            editingId = reminder.id
                            message = if (notificationsAllowed) "Daily reminder saved." else "Reminder saved. Turn on notifications to receive it."
                        }
                    }
                }) { Text(if (editingId == null) "Save reminder" else "Update reminder") }
                if (editingId != null) TextButton(onClick = { editingId = null; message = null }) { Text("New reminder") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            message?.let { Text(it) }
            if (loadError) {
                Text("Unable to load reminders.")
                TextButton(onClick = { attempt++ }) { Text("Retry") }
            } else if (reminders == null) {
                CircularProgressIndicator()
            } else if (reminders!!.isEmpty()) {
                Text("No reminders yet.", style = MaterialTheme.typography.bodyMedium)
            } else reminders!!.forEach { reminder ->
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(reminder.medicine, style = MaterialTheme.typography.titleMedium)
                        Text("Every day • ${formatTime12h(reminder.hour, reminder.minute)}")
                    }
                    Switch(checked = reminder.enabled, enabled = !working,
                        modifier = Modifier.semantics { contentDescription = "Enable reminder for ${reminder.medicine} at ${formatTime12h(reminder.hour, reminder.minute)}" },
                        onCheckedChange = { enabled -> change({ manager.save(reminder.copy(enabled = enabled)) }) })
                }
                Row {
                    TextButton(enabled = !working, onClick = {
                        editingId = reminder.id
                        onEdit(reminder.medicine, formatTime12h(reminder.hour, reminder.minute))
                        message = "Edit the medication name or Scheduled Time above, then update the reminder."
                    }) { Text("Edit") }
                    TextButton(enabled = !working, onClick = {
                        change({ manager.delete(reminder.id) }) { if (editingId == reminder.id) editingId = null }
                    }) { Text("Delete") }
                }
            }
        }
    }
}
