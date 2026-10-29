package com.example.meditrack.ui.state

import org.junit.Assert.*
import org.junit.Test

class MedicationFormStateTest {
    @Test
    fun submissionCanKeepInputUntilDatabaseConfirmsSave() {
        val form = MedicationFormState(name = "Test", scheduledTime = "08:00 AM", actualTime = "08:05 AM")
        val record = form.submit(resetAfterSubmit = false)
        assertNotNull(record)
        assertEquals("Test", form.name)
        assertEquals("08:05 AM", form.actualTime)
        form.reset()
        assertTrue(form.name.isEmpty())
        assertNull(form.scheduledTime)
    }
}
