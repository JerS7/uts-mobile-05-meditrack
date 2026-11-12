package com.example.meditrack.reminders

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Stored in the shared app database; see [com.example.meditrack.data.MediTrackDatabase]. */
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
