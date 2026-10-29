package com.example.meditrack.data

import android.content.Context
import androidx.room.*
import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "medication_records")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val sequence: Long = 0,
    @ColumnInfo(index = true) val id: String,
    val name: String,
    val scheduledTime: String,
    val actualTime: String?,
    val note: String,
    val status: String,
) {
    fun toRecord() = MedicationRecord(name, scheduledTime, actualTime, note, DoseStatus.valueOf(status), id)
}

fun MedicationRecord.toEntity() = MedicationEntity(
    id = id, name = name, scheduledTime = scheduledTime, actualTime = actualTime,
    note = note, status = status.name,
)

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medication_records ORDER BY sequence ASC")
    fun observeRecords(): Flow<List<MedicationEntity>>

    @Insert
    suspend fun insert(record: MedicationEntity)
}

@Database(entities = [MedicationEntity::class], version = 1, exportSchema = true)
abstract class MedicationDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile private var instance: MedicationDatabase? = null

        fun getInstance(context: Context): MedicationDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, MedicationDatabase::class.java, "meditrack.db",
            ).build().also { instance = it }
        }
    }
}
