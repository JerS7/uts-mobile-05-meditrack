package com.example.meditrack

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.example.meditrack.data.MediTrackDatabase
import com.example.meditrack.data.toEntity
import com.example.meditrack.model.DoseStatus
import com.example.meditrack.util.Dates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Opens a version 1 file (records only, no dates) with the version 2 database. */
class MigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun version1RecordsKeepTheirDataAndGetADate() = runBlocking {
        val name = "migration-test.db"
        context.deleteDatabase(name)
        context.getDatabasePath(name).parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `medication_records` (`sequence` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id` TEXT NOT NULL, `name` TEXT NOT NULL, `scheduledTime` TEXT NOT NULL, `actualTime` TEXT, `note` TEXT NOT NULL, `status` TEXT NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_records_id` ON `medication_records` (`id`)")
            db.execSQL("INSERT INTO medication_records (id, name, scheduledTime, actualTime, note, status) VALUES ('mine', 'My record', '08:00 AM', '08:10 AM', 'note', 'TAKEN')")
            db.execSQL("INSERT INTO medication_records (id, name, scheduledTime, actualTime, note, status) VALUES ('uts-demo-c', 'Obat Simulasi C', '07:00 PM', NULL, '', 'MISSED')")
            db.version = 1
        }
        val database = MediTrackDatabase.open(context, name)
        try {
            val records = database.medicationDao().observeRecords().first().map { it.toRecord() }
            assertEquals(2, records.size)
            assertTrue(records.all { it.date == Dates.today() })
            val mine = records.single { it.id == "mine" }
            assertEquals("My record", mine.name)
            assertEquals("08:10 AM", mine.actualTime)
            assertEquals(DoseStatus.TAKEN, mine.status)
            assertEquals("Loratadine", records.single { it.id == "uts-demo-c" }.name)
            assertEquals(30, database.catalog().count())
            assertTrue(database.reminders().observe().first().isEmpty())
            // New records still get sequence numbers after the copied ones.
            database.medicationDao().save(mine.copy(id = "new", name = "New").toEntity())
            assertEquals("new", database.medicationDao().observeRecords().first().first().id)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
