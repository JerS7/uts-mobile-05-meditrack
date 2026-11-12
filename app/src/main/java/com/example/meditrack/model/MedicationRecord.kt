package com.example.meditrack.model

import com.example.meditrack.util.Dates
import java.util.UUID

enum class DoseStatus {
    TAKEN,
    MISSED,
}

/**
 * A single log entry. [date] is yyyy-MM-dd in local time; times use the UI's
 * 12-hour format, for example 08:30 AM.
 */
data class MedicationRecord(
    val name: String,
    val scheduledTime: String,
    val actualTime: String?,
    val note: String,
    val status: DoseStatus,
    val date: String = Dates.today(),
    val id: String = UUID.randomUUID().toString(),
)
