package com.example.meditrack.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.example.meditrack.model.MedicationRecord

/** In-memory records only; recreating this state starts a new session. */
class MediTrackState(initialRecords: List<MedicationRecord> = emptyList()) {
    private val mutableRecords = mutableStateListOf<MedicationRecord>().apply {
        addAll(initialRecords)
    }

    // Expose a read-only snapshot so callers cannot bypass state operations.
    val records: List<MedicationRecord>
        get() = mutableRecords.toList()

    // Form validation will happen before this operation in stage two.
    fun addRecord(record: MedicationRecord) {
        mutableRecords.add(record)
    }
}

/** Keep one state during recomposition, without saving it to disk or a database. */
@Composable
fun rememberMediTrackState(
    initialRecords: List<MedicationRecord> = emptyList(),
): MediTrackState = remember { MediTrackState(initialRecords) }
