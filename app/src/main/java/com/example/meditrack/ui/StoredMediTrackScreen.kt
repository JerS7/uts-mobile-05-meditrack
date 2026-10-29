package com.example.meditrack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditrack.model.MedicationRecord
import kotlinx.coroutines.CancellationException

@Composable
fun StoredMediTrackScreen(model: MedicationViewModel = viewModel()) {
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
    if (loadFailed || records == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (loadFailed) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Gagal memuat catatan yang tersimpan.")
                    TextButton(onClick = { attempt++ }) { Text("Coba Lagi") }
                }
            } else CircularProgressIndicator()
        }
    } else {
        MediTrackScreen(records = records!!, onAddRecord = model::save, searchMedicines = model::searchMedicines,
            remindersContent = { form ->
                ReminderPanel(medicineName = form.name, scheduledTime = form.scheduledTime,
                    onEdit = { name, time -> form.updateName(name); form.scheduledTime = time })
            })
    }
}
