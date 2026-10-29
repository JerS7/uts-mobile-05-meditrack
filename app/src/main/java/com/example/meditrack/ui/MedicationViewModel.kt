package com.example.meditrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditrack.data.MedicationDatabase
import com.example.meditrack.data.MedicineCatalogDatabase
import com.example.meditrack.data.toEntity
import com.example.meditrack.model.MedicationRecord
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.map

class MedicationViewModel(application: Application) : AndroidViewModel(application) {
    private val catalog = MedicineCatalogDatabase.open(application)
    private val dao = MedicationDatabase.getInstance(application).medicationDao()
    val records = dao.observeRecords().map { rows -> rows.map { it.toRecord() } }

    suspend fun searchMedicines(query: String): List<String> = catalog.suggestions(query)

    override fun onCleared() {
        catalog.close()
        super.onCleared()
    }

    // The write survives activity recreation; the UI reports success only after it commits.
    suspend fun save(record: MedicationRecord) {
        viewModelScope.async { dao.insert(record.toEntity()) }.await()
    }
}
