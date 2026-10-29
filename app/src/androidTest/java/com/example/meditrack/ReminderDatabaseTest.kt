package com.example.meditrack

import androidx.test.platform.app.InstrumentationRegistry
import com.example.meditrack.reminders.MedicationReminder
import com.example.meditrack.reminders.ReminderDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ReminderDatabaseTest {
    @Test fun settingsPersistAndCanBeEditedDisabledAndDeleted() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "reminders-test.db"
        context.deleteDatabase(name)
        var database = ReminderDatabase.open(context, name)
        val reminder = MedicationReminder(medicine = "Test medicine", hour = 8, minute = 30)
        try {
            assertTrue(database.reminders().observe().first().isEmpty())
            database.reminders().save(reminder)
            database.close()
            database = ReminderDatabase.open(context, name)
            assertEquals(reminder, database.reminders().find(reminder.id))
            val edited = reminder.copy(hour = 10, minute = 15, enabled = false)
            database.reminders().save(edited)
            assertEquals(edited, database.reminders().find(reminder.id))
            assertTrue(database.reminders().enabled().isEmpty())
            assertEquals(1, database.reminders().observe().first().size)
            database.reminders().delete(reminder.id)
            assertNull(database.reminders().find(reminder.id))
        } finally {
            database.close(); context.deleteDatabase(name)
        }
    }
}
