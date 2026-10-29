package com.example.meditrack.reminders

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "reminders")
data class MedicationReminder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val medicine: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY hour, minute, medicine")
    fun observe(): Flow<List<MedicationReminder>>
    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun enabled(): List<MedicationReminder>
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun find(id: String): MedicationReminder?
    @Upsert suspend fun save(reminder: MedicationReminder)
    @Query("DELETE FROM reminders WHERE id = :id") suspend fun delete(id: String)
}

@Database(entities = [MedicationReminder::class], version = 1, exportSchema = true)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminders(): ReminderDao
    companion object {
        fun open(context: Context, name: String = "medication-reminders.db"): ReminderDatabase =
            Room.databaseBuilder(context.applicationContext, ReminderDatabase::class.java, name).build()
    }
}
