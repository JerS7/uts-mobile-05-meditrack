package com.example.meditrack.ui.state

import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord
import org.junit.Assert.*
import org.junit.Test

class RecordStatsTest {
    private fun record(status: DoseStatus, date: String, id: String) =
        MedicationRecord("Test", "08:00 AM", if (status == DoseStatus.TAKEN) "08:05 AM" else null, "", status, date, id)

    private val records = listOf(
        record(DoseStatus.TAKEN, "2026-11-12", "a"),
        record(DoseStatus.MISSED, "2026-11-12", "b"),
        record(DoseStatus.TAKEN, "2026-11-12", "c"),
        record(DoseStatus.TAKEN, "2026-11-11", "d"),
    )

    @Test fun summaryDropsFractions() {
        val today = records.filter { it.date == "2026-11-12" }.summary()
        assertEquals(RecordSummary(total = 3, taken = 2, missed = 1), today)
        assertEquals(66, today.percent)
        assertEquals(75, records.summary().percent)
    }

    @Test fun emptySummaryIsZero() {
        val empty = emptyList<MedicationRecord>().summary()
        assertEquals(0, empty.percent)
        assertEquals(0f, empty.adherence)
    }

    @Test fun filtersByStatusAndDateAndKeepOrder() {
        assertEquals(listOf("a", "b", "c", "d"), records.filtered(StatusFilter.ALL, null).map { it.id })
        assertEquals(listOf("a", "c", "d"), records.filtered(StatusFilter.TAKEN, null).map { it.id })
        assertEquals(listOf("b"), records.filtered(StatusFilter.MISSED, "2026-11-12").map { it.id })
        assertEquals(listOf("d"), records.filtered(StatusFilter.ALL, "2026-11-11").map { it.id })
        assertTrue(records.filtered(StatusFilter.MISSED, "2026-11-11").isEmpty())
    }
}
