package com.example.meditrack.data

import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord
import com.example.meditrack.util.Dates

/**
 * Fictional records for the UTS demonstration, not treatment instructions.
 * They are only added when the user taps "Isi data contoh" on the empty screen.
 */
fun demoMedicationRecords(date: String = Dates.today()) = listOf(
    MedicationRecord(
        id = "uts-demo-a", name = "Paracetamol", scheduledTime = "08:00 AM",
        actualTime = "08:05 AM", status = DoseStatus.TAKEN, date = date,
        note = "Data dummy UTS: contoh dosis diminum pada pagi hari.",
    ),
    MedicationRecord(
        id = "uts-demo-b", name = "Ascorbic acid (Vitamin C)", scheduledTime = "12:00 PM",
        actualTime = "12:10 PM", status = DoseStatus.TAKEN, date = date,
        note = "Data dummy UTS: contoh dosis diminum pada siang hari.",
    ),
    MedicationRecord(
        id = "uts-demo-c", name = "Loratadine", scheduledTime = "07:00 PM",
        actualTime = null, status = DoseStatus.MISSED, date = date,
        note = "Data dummy UTS: contoh dosis terlewat tanpa waktu aktual.",
    ),
)

/** Names used by early builds; the database migration renames them. */
internal val legacyDemoNames = mapOf(
    "uts-demo-a" to ("Obat Simulasi A" to "Paracetamol"),
    "uts-demo-b" to ("Obat Simulasi B" to "Ascorbic acid (Vitamin C)"),
    "uts-demo-c" to ("Obat Simulasi C" to "Loratadine"),
)
