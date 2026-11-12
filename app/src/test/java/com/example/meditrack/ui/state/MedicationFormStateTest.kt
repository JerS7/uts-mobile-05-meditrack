package com.example.meditrack.ui.state

import com.example.meditrack.R
import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord
import org.junit.Assert.*
import org.junit.Test

class MedicationFormStateTest {
    private val today = "2026-11-12"
    private fun form(
        name: String = "Test", scheduledTime: String? = "08:00 AM", actualTime: String? = "08:05 AM",
    ) = MedicationFormState(name = name, scheduledTime = scheduledTime, actualTime = actualTime, today = { today })

    @Test
    fun submissionCanKeepInputUntilDatabaseConfirmsSave() {
        val form = form()
        val record = form.submit(resetAfterSubmit = false)
        assertNotNull(record)
        assertEquals("Test", form.name)
        assertEquals("08:05 AM", form.actualTime)
        form.reset()
        assertTrue(form.name.isEmpty())
        assertNull(form.scheduledTime)
    }

    @Test
    fun newRecordUsesTodayByDefaultAndChosenDateWhenChanged() {
        val form = form()
        assertEquals(today, form.submit(resetAfterSubmit = false)!!.date)
        form.updateDate("2026-11-10")
        assertEquals("2026-11-10", form.submit()!!.date)
        assertEquals(today, form.date)
    }

    @Test
    fun missingFieldsAndFutureDateAreRejected() {
        val blank = form(name = "   ", scheduledTime = null, actualTime = null)
        assertNull(blank.submit())
        assertTrue(blank.showErrors)
        assertEquals(R.string.error_name_required, blank.nameError)
        assertEquals(R.string.error_scheduled_time_required, blank.scheduledTimeError)
        assertEquals(R.string.error_actual_time_required, blank.actualTimeError)

        val future = form()
        future.updateDate("2026-11-13")
        assertEquals(R.string.error_date_in_future, future.dateError)
        assertNull(future.submit())
    }

    @Test
    fun invalidDateTextIsIgnored() {
        val form = form()
        form.updateDate("12/11/2026")
        assertEquals(today, form.date)
    }

    @Test
    fun missedDoseClearsAndDoesNotNeedActualTime() {
        val form = form()
        form.selectStatus(DoseStatus.MISSED)
        assertNull(form.actualTime)
        assertNull(form.actualTimeError)
        val record = form.submit()!!
        assertEquals(DoseStatus.MISSED, record.status)
        assertNull(record.actualTime)
    }

    @Test
    fun editingKeepsTheRecordIdAndResetEndsEditing() {
        val original = MedicationRecord("Paracetamol", "08:00 AM", "08:05 AM", "note", DoseStatus.TAKEN, "2026-11-11", "record-1")
        val form = form(name = "", scheduledTime = null, actualTime = null)
        form.load(original)
        assertTrue(form.isEditing)
        form.updateName("Paracetamol 500")
        val edited = form.submit(resetAfterSubmit = false)!!
        assertEquals(original.copy(name = "Paracetamol 500"), edited)
        form.reset()
        assertFalse(form.isEditing)
        assertNull(form.editingId)
    }

    @Test
    fun newRecordsGetDifferentIds() {
        val first = form().submit()!!
        val second = form().submit()!!
        assertNotEquals(first.id, second.id)
    }

    @Test
    fun reminderPrefillStartsANewTakenRecordForToday() {
        val form = form()
        form.load(MedicationRecord("Old", "07:00 AM", null, "", DoseStatus.MISSED, "2026-11-01", "old"))
        form.prefillFromReminder("Loratadine", "09:00 PM", "09:03 PM")
        assertFalse(form.isEditing)
        assertEquals("Loratadine", form.name)
        assertEquals("09:00 PM", form.scheduledTime)
        assertEquals("09:03 PM", form.actualTime)
        assertEquals(DoseStatus.TAKEN, form.status)
        assertEquals(today, form.date)
    }

    @Test
    fun timesRoundTripBetween12And24HourFormats() {
        assertEquals("12:00 AM", formatTime12h(0, 0))
        assertEquals("12:30 PM", formatTime12h(12, 30))
        assertEquals("11:59 PM", formatTime12h(23, 59))
        assertEquals(0 to 0, parseTime12h("12:00 AM"))
        assertEquals(12 to 30, parseTime12h("12:30 PM"))
        assertEquals(23 to 59, parseTime12h("11:59 PM"))
        assertNull(parseTime12h("13:00 PM"))
    }
}
