package com.example.meditrack.data

import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord

/** Fictional records for the UTS demonstration, not treatment instructions. */
val demoMedicationRecords = listOf(
    MedicationRecord(
        id = "uts-demo-a", name = "Paracetamol", scheduledTime = "08:00 AM",
        actualTime = "08:05 AM", status = DoseStatus.TAKEN,
        note = "Data dummy UTS: contoh dosis diminum pada pagi hari.",
    ),
    MedicationRecord(
        id = "uts-demo-b", name = "Ascorbic acid (Vitamin C)", scheduledTime = "12:00 PM",
        actualTime = "12:10 PM", status = DoseStatus.TAKEN,
        note = "Data dummy UTS: contoh dosis diminum pada siang hari.",
    ),
    MedicationRecord(
        id = "uts-demo-c", name = "Loratadine", scheduledTime = "07:00 PM",
        actualTime = null, status = DoseStatus.MISSED,
        note = "Data dummy UTS: contoh dosis terlewat tanpa waktu aktual.",
    ),
)

internal val legacyDemoNames = mapOf(
    "uts-demo-a" to "Obat Simulasi A",
    "uts-demo-b" to "Obat Simulasi B",
    "uts-demo-c" to "Obat Simulasi C",
)
