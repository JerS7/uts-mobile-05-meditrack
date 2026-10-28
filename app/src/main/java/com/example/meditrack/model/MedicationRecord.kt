package com.example.meditrack.model

import java.util.UUID

enum class DoseStatus {
    TAKEN,
    MISSED,
}

/** A single log entry. Times use the UI's 12-hour format, for example 08:30 AM. */
data class MedicationRecord(
    val name: String,
    val scheduledTime: String,
    val actualTime: String?,
    val note: String,
    val status: DoseStatus,
    val id: String = UUID.randomUUID().toString(),
)
