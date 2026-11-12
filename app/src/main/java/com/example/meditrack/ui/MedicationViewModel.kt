package com.example.meditrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditrack.data.MediTrackDatabase
import com.example.meditrack.data.demoMedicationRecords
import com.example.meditrack.data.toEntity
import com.example.meditrack.model.MedicationRecord
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.map

class MedicationViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MediTrackDatabase.getInstance(application)
    private val dao = database.medicationDao()

    /** Newest day first, then most recently added. */
    val records = dao.observeRecords().map { rows -> rows.map { it.toRecord() } }

    suspend fun searchMedicines(query: String): List<String> = database.suggestions(query)

    // Writes survive activity recreation; the UI reports success only after they commit.

    /** Adds a new record or replaces the record with the same ID. */
    suspend fun save(record: MedicationRecord) {
        viewModelScope.async { dao.save(record.toEntity()) }.await()
    }

    suspend fun delete(id: String) {
        viewModelScope.async { dao.delete(id) }.await()
    }

    /** Adds the three UTS sample records for today. Repeated taps do not duplicate them. */
    suspend fun addDemoRecords() {
        viewModelScope.async { dao.insertMissing(demoMedicationRecords().map { it.toEntity() }) }.await()
    }
}
