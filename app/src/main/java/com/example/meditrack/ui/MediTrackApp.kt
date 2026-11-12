package com.example.meditrack.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditrack.R
import com.example.meditrack.model.MedicationRecord
import com.example.meditrack.ui.state.formatTime12h
import com.example.meditrack.ui.state.rememberMedicationFormState
import com.example.meditrack.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Calendar

/** The four pages in the bottom navigation bar. */
enum class Page(@param:StringRes val label: Int, val icon: ImageVector) {
    HOME(R.string.nav_home, MediTrackIcons.Home),
    LOG(R.string.nav_log, MediTrackIcons.Add),
    REMINDERS(R.string.nav_reminders, MediTrackIcons.Bell),
    HISTORY(R.string.nav_history, MediTrackIcons.History),
}

/** A dose to log, opened from a reminder notification. */
data class DosePrefill(val medicine: String, val scheduledTime: String)

@Composable
fun MediTrackApp(
    model: MedicationViewModel = viewModel(),
    prefill: DosePrefill? = null,
    onPrefillHandled: () -> Unit = {},
) {
    val resources = LocalResources.current
    var records by remember { mutableStateOf<List<MedicationRecord>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(model, attempt) {
        loadFailed = false
        try {
            model.records.collect { records = it }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    var page by rememberSaveable { mutableStateOf(Page.HOME) }
    // The form lives here so a draft survives switching pages and History can load a record into it.
    val form = rememberMedicationFormState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun notify(message: String, action: String? = null, onAction: suspend () -> Unit = {}) {
        scope.launch {
            if (snackbar.showSnackbar(message, action, withDismissAction = action == null) == SnackbarResult.ActionPerformed) {
                try { onAction() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { snackbar.showSnackbar(resources.getString(R.string.action_failed)) }
            }
        }
    }

    LaunchedEffect(prefill) {
        if (prefill != null) {
            val now = Calendar.getInstance()
            form.prefillFromReminder(
                prefill.medicine, prefill.scheduledTime,
                formatTime12h(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)),
            )
            page = Page.LOG
            onPrefillHandled()
        }
    }
    BackHandler(enabled = page != Page.HOME) {
        page = if (page == Page.LOG && form.isEditing) Page.HISTORY else Page.HOME
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                Page.entries.forEach { item ->
                    NavigationBarItem(
                        selected = page == item,
                        onClick = { page = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.label)) },
                        modifier = Modifier.testTag("nav-${item.name.lowercase()}"),
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Teal, selectedTextColor = Teal, indicatorColor = TealContainer),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding).imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val content = Modifier.widthIn(max = 600.dp).fillMaxSize()
            when (page) {
                Page.LOG -> LogScreen(
                    form = form, modifier = content,
                    onSave = model::save,
                    onSaved = { record, wasEditing ->
                        if (wasEditing) {
                            page = Page.HISTORY
                            notify(resources.getString(R.string.changes_saved, record.name))
                        } else {
                            notify(resources.getString(R.string.record_saved, record.name))
                        }
                    },
                    onCancelEdit = { page = Page.HISTORY },
                    searchMedicines = model::searchMedicines,
                )
                Page.REMINDERS -> ReminderScreen(modifier = content, searchMedicines = model::searchMedicines)
                Page.HOME, Page.HISTORY -> {
                    val loaded = records
                    when {
                        loadFailed -> Column(content, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(stringResource(R.string.load_failed))
                            TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
                        }
                        loaded == null -> Box(content, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        page == Page.HOME -> HomeScreen(
                            records = loaded, modifier = content,
                            onLogDose = { if (form.isEditing) form.reset(); page = Page.LOG },
                            onOpenHistory = { page = Page.HISTORY },
                            onAddDemoRecords = {
                                scope.launch {
                                    try {
                                        model.addDemoRecords()
                                        notify(resources.getString(R.string.demo_added))
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        notify(resources.getString(R.string.action_failed))
                                    }
                                }
                            },
                        )
                        else -> HistoryScreen(
                            records = loaded, modifier = content,
                            onEdit = { record -> form.load(record); page = Page.LOG },
                            onDelete = { record ->
                                scope.launch {
                                    try {
                                        model.delete(record.id)
                                        if (form.editingId == record.id) form.reset()
                                        notify(resources.getString(R.string.record_deleted, record.name), resources.getString(R.string.action_undo)) {
                                            model.save(record)
                                        }
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        notify(resources.getString(R.string.action_failed))
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
