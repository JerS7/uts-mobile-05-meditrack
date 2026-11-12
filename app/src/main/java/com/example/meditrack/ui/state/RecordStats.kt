package com.example.meditrack.ui.state

import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord

/** Counts for a set of records. The percentage drops fractions, so 2 of 3 is 66%. */
data class RecordSummary(val total: Int, val taken: Int, val missed: Int) {
    val adherence: Float get() = if (total == 0) 0f else taken.toFloat() / total
    val percent: Int get() = if (total == 0) 0 else taken * 100 / total
}

fun List<MedicationRecord>.summary() = RecordSummary(
    total = size,
    taken = count { it.status == DoseStatus.TAKEN },
    missed = count { it.status == DoseStatus.MISSED },
)

enum class StatusFilter { ALL, TAKEN, MISSED }

/** Keeps the input order. A null [date] means every day. */
fun List<MedicationRecord>.filtered(status: StatusFilter, date: String?): List<MedicationRecord> = filter { record ->
    (date == null || record.date == date) && when (status) {
        StatusFilter.ALL -> true
        StatusFilter.TAKEN -> record.status == DoseStatus.TAKEN
        StatusFilter.MISSED -> record.status == DoseStatus.MISSED
    }
}
