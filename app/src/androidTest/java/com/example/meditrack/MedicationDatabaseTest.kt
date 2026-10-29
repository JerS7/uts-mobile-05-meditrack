package com.example.meditrack

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.meditrack.data.MedicationDatabase
import com.example.meditrack.data.toEntity
import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MedicationDatabaseTest {
    @Test
    fun recordsSurviveClosingAndReopeningDatabase() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // A separate database keeps user records untouched.
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, MedicationDatabase::class.java, name).build()
        var database = open()
        val taken = MedicationRecord("Test taken", "08:00 AM", "08:05 AM", "Test note", DoseStatus.TAKEN)
        val missed = MedicationRecord("Test missed", "09:00 AM", null, "", DoseStatus.MISSED)
        try {
            assertTrue(database.medicationDao().observeRecords().first().isEmpty())
            database.medicationDao().insert(taken.toEntity())
            database.medicationDao().insert(missed.toEntity())
            assertEquals(2, database.medicationDao().observeRecords().first().size)
            database.close()
            database = open()
            val restored = database.medicationDao().observeRecords().first().map { it.toRecord() }
            assertEquals(listOf(taken, missed), restored)
            assertEquals(1, restored.count { it.status == DoseStatus.TAKEN })
            assertEquals(1, restored.count { it.status == DoseStatus.MISSED })
            assertNull(restored.last().actualTime)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
