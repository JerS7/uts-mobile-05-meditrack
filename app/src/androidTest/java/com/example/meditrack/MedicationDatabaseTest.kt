package com.example.meditrack

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.meditrack.data.MediTrackDatabase
import com.example.meditrack.data.demoMedicationRecords
import com.example.meditrack.data.toEntity
import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MedicationDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private suspend fun MediTrackDatabase.records() =
        medicationDao().observeRecords().first().map { it.toRecord() }

    @Test
    fun demoRecordsAreAddedOnceWithoutReplacingExistingRecords() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, MediTrackDatabase::class.java).build()
        val existing = MedicationRecord("Existing record", "08:00 AM", null, "Keep this", DoseStatus.MISSED, "2026-11-12")
        try {
            database.medicationDao().insert(existing.toEntity())
            val demo = demoMedicationRecords("2026-11-12").map { it.toEntity() }
            database.medicationDao().insertMissing(demo)
            database.medicationDao().insertMissing(demo)
            val records = database.records()
            assertEquals(4, records.size)
            assertTrue(existing in records)
            assertEquals(3, records.count { it.id.startsWith("uts-demo-") })
            assertEquals(2, records.count { it.status == DoseStatus.TAKEN })
            assertEquals(2, records.count { it.status == DoseStatus.MISSED })
        } finally { database.close() }
    }

    @Test
    fun newestDayAndNewestRecordComeFirst() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, MediTrackDatabase::class.java).build()
        try {
            val dao = database.medicationDao()
            dao.insert(MedicationRecord("Older day", "08:00 AM", null, "", DoseStatus.MISSED, "2026-11-10", "a").toEntity())
            dao.insert(MedicationRecord("Today first", "08:00 AM", null, "", DoseStatus.MISSED, "2026-11-12", "b").toEntity())
            dao.insert(MedicationRecord("Today second", "09:00 AM", null, "", DoseStatus.MISSED, "2026-11-12", "c").toEntity())
            dao.insert(MedicationRecord("Middle day", "08:00 AM", null, "", DoseStatus.MISSED, "2026-11-11", "d").toEntity())
            assertEquals(listOf("c", "b", "d", "a"), database.records().map { it.id })
        } finally { database.close() }
    }

    @Test
    fun saveUpdatesInPlaceAndDeleteRemovesOnlyThatRecord() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, MediTrackDatabase::class.java).build()
        try {
            val dao = database.medicationDao()
            val first = MedicationRecord("First", "08:00 AM", "08:05 AM", "", DoseStatus.TAKEN, "2026-11-12", "first")
            val second = MedicationRecord("Second", "09:00 AM", null, "", DoseStatus.MISSED, "2026-11-12", "second")
            dao.save(first.toEntity())
            dao.save(second.toEntity())
            val edited = first.copy(name = "First edited", status = DoseStatus.MISSED, actualTime = null)
            dao.save(edited.toEntity())
            // Editing keeps the record in its place instead of moving it to the top.
            assertEquals(listOf(second, edited), database.records())
            assertEquals(1, dao.delete("second"))
            assertEquals(0, dao.delete("second"))
            assertEquals(listOf(edited), database.records())
        } finally { database.close() }
    }

    @Test
    fun recordsSurviveClosingAndReopeningDatabase() = runBlocking {
        // A separate database keeps user records untouched.
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        var database = MediTrackDatabase.open(context, name)
        val taken = MedicationRecord("Test taken", "08:00 AM", "08:05 AM", "Test note", DoseStatus.TAKEN, "2026-11-12")
        val missed = MedicationRecord("Test missed", "09:00 AM", null, "", DoseStatus.MISSED, "2026-11-11")
        try {
            assertTrue(database.records().isEmpty())
            database.medicationDao().save(taken.toEntity())
            database.medicationDao().save(missed.toEntity())
            database.close()
            database = MediTrackDatabase.open(context, name)
            assertEquals(listOf(taken, missed), database.records())
            assertEquals(30, database.catalog().count())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
