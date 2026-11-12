package com.example.meditrack.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord
import com.example.meditrack.reminders.MedicationReminder
import com.example.meditrack.reminders.ReminderDao
import com.example.meditrack.util.Dates
import kotlinx.coroutines.flow.Flow
import java.util.Locale

@Entity(tableName = "medication_records", indices = [Index(value = ["id"], unique = true)])
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val sequence: Long = 0,
    val id: String,
    val date: String,
    val name: String,
    val scheduledTime: String,
    val actualTime: String?,
    val note: String,
    val status: String,
) {
    fun toRecord() = MedicationRecord(
        name = name, scheduledTime = scheduledTime, actualTime = actualTime, note = note,
        status = DoseStatus.valueOf(status), date = date, id = id,
    )
}

fun MedicationRecord.toEntity() = MedicationEntity(
    id = id, date = date, name = name, scheduledTime = scheduledTime, actualTime = actualTime,
    note = note, status = status.name,
)

@Dao
interface MedicationDao {
    /** Newest day first; within a day, the most recently added record first. */
    @Query("SELECT * FROM medication_records ORDER BY date DESC, sequence DESC")
    fun observeRecords(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medication_records WHERE id = :id")
    suspend fun find(id: String): MedicationEntity?

    @Insert
    suspend fun insert(record: MedicationEntity)

    @Update
    suspend fun update(record: MedicationEntity)

    @Query("DELETE FROM medication_records WHERE id = :id")
    suspend fun delete(id: String): Int

    /** Adds a new record, or replaces the record with the same [MedicationEntity.id]. */
    @Transaction
    suspend fun save(record: MedicationEntity) {
        val existing = find(record.id)
        if (existing == null) insert(record.copy(sequence = 0))
        else update(record.copy(sequence = existing.sequence))
    }

    /** Fixed IDs keep repeated taps from duplicating the sample records. */
    @Transaction
    suspend fun insertMissing(records: List<MedicationEntity>) {
        records.forEach { if (find(it.id) == null) insert(it.copy(sequence = 0)) }
    }
}

@Entity(tableName = "medicines")
data class CatalogMedicine(@PrimaryKey val name: String, val aliases: String, val source: String)

@Dao
interface MedicineCatalogDao {
    @Query("""SELECT name FROM medicines
        WHERE instr(lower(name), :query) > 0 OR instr(lower(aliases), :query) > 0
        ORDER BY CASE WHEN instr(lower(name), :query) = 1 THEN 0 ELSE 1 END, name
        LIMIT 5""")
    suspend fun search(query: String): List<String>

    @Query("SELECT COUNT(*) FROM medicines")
    suspend fun count(): Int
}

/**
 * One database for records, the reference name catalog, and reminders.
 * Version 1 held records only; reminders and the catalog lived in separate files.
 */
@Database(
    entities = [MedicationEntity::class, CatalogMedicine::class, MedicationReminder::class],
    version = 2, exportSchema = true,
)
abstract class MediTrackDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun catalog(): MedicineCatalogDao
    abstract fun reminders(): ReminderDao

    /** Catalog names are text matches only, not treatment suggestions. */
    suspend fun suggestions(query: String): List<String> {
        val text = query.trim().lowercase(Locale.ROOT)
        return if (text.length < 2) emptyList() else catalog().search(text)
    }

    companion object {
        const val NAME = "meditrack.db"
        internal const val LEGACY_REMINDERS = "medication-reminders.db"
        internal const val LEGACY_CATALOG = "medicine-catalog.db"

        @Volatile private var instance: MediTrackDatabase? = null

        fun getInstance(context: Context): MediTrackDatabase = instance ?: synchronized(this) {
            instance ?: open(context, NAME, importLegacyFiles = true).also { instance = it }
        }

        /** Tests pass their own [name]; only the app database imports the old files. */
        fun open(context: Context, name: String, importLegacyFiles: Boolean = false): MediTrackDatabase {
            val appContext = context.applicationContext
            return Room.databaseBuilder(appContext, MediTrackDatabase::class.java, name)
                .addMigrations(migration1To2())
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) = seedCatalog(db)
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        if (importLegacyFiles) importLegacyFiles(appContext, db)
                    }
                })
                .build()
        }

        /**
         * Version 2 adds a date to each record, makes record IDs unique, and moves the
         * catalog and reminder tables into this file. Existing records get the date of
         * the upgrade because version 1 never stored one.
         */
        internal fun migration1To2(today: () -> String = { Dates.today() }) = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS `index_medication_records_id`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `medication_records_new` (`sequence` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`id` TEXT NOT NULL, `date` TEXT NOT NULL, `name` TEXT NOT NULL, `scheduledTime` TEXT NOT NULL, " +
                        "`actualTime` TEXT, `note` TEXT NOT NULL, `status` TEXT NOT NULL)",
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_medication_records_id` ON `medication_records_new` (`id`)")
                db.execSQL(
                    "INSERT OR IGNORE INTO `medication_records_new` (sequence, id, date, name, scheduledTime, actualTime, note, status) " +
                        "SELECT sequence, id, ?, name, scheduledTime, actualTime, note, status FROM `medication_records` ORDER BY sequence",
                    arrayOf<Any?>(today()),
                )
                db.execSQL("DROP TABLE `medication_records`")
                db.execSQL("ALTER TABLE `medication_records_new` RENAME TO `medication_records`")
                legacyDemoNames.forEach { (id, names) ->
                    db.execSQL(
                        "UPDATE `medication_records` SET name = ? WHERE id = ? AND name = ?",
                        arrayOf<Any?>(names.second, id, names.first),
                    )
                }
                db.execSQL("CREATE TABLE IF NOT EXISTS `medicines` (`name` TEXT NOT NULL, `aliases` TEXT NOT NULL, `source` TEXT NOT NULL, PRIMARY KEY(`name`))")
                seedCatalog(db)
                db.execSQL("CREATE TABLE IF NOT EXISTS `reminders` (`id` TEXT NOT NULL, `medicine` TEXT NOT NULL, `hour` INTEGER NOT NULL, `minute` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        private fun seedCatalog(db: SupportSQLiteDatabase) {
            catalogSeed.forEach { medicine ->
                db.execSQL(
                    "INSERT OR IGNORE INTO medicines (name, aliases, source) VALUES (?, ?, ?)",
                    arrayOf<Any?>(medicine.name, medicine.aliases, medicine.source),
                )
            }
        }

        /**
         * Copies reminders from the old separate file so existing alarms keep their IDs,
         * then removes the old files. A failed copy keeps the old file for the next launch.
         */
        private fun importLegacyFiles(context: Context, db: SupportSQLiteDatabase) {
            val legacy = context.getDatabasePath(LEGACY_REMINDERS)
            if (legacy.exists()) {
                try {
                    SQLiteDatabase.openDatabase(legacy.path, null, SQLiteDatabase.OPEN_READONLY).use { old ->
                        old.rawQuery("SELECT id, medicine, hour, minute, enabled FROM reminders", null).use { rows ->
                            while (rows.moveToNext()) {
                                db.execSQL(
                                    "INSERT OR IGNORE INTO reminders (id, medicine, hour, minute, enabled) VALUES (?, ?, ?, ?, ?)",
                                    arrayOf<Any?>(rows.getString(0), rows.getString(1), rows.getInt(2), rows.getInt(3), rows.getInt(4)),
                                )
                            }
                        }
                    }
                    context.deleteDatabase(LEGACY_REMINDERS)
                } catch (error: Exception) {
                    Log.w("MediTrack", "Unable to import old reminders", error)
                }
            }
            // The catalog is rebuilt inside this database, so the old copy is no longer needed.
            context.deleteDatabase(LEGACY_CATALOG)
        }
    }
}

// Names selected from WHO reference lists. Aliases help match Indonesian spellings
// and vitamin names; they do not describe indications, doses or suitability.
private const val WHO_MEDICINES = "https://iris.who.int/bitstream/handle/10665/382242/B09475-eng.pdf?sequence=1"
private const val WHO_VITAMINS = "https://iris.who.int/bitstream/handle/10665/43887/WHO_TRS_950_eng.pdf?sequence=1"
private val catalogSeed = listOf(
    "Paracetamol" to "parasetamol acetaminophen",
    "Ibuprofen" to "ibuprofen",
    "Amoxicillin" to "amoksisilin",
    "Metformin" to "metformin",
    "Omeprazole" to "omeprazol",
    "Loratadine" to "loratadin",
    "Folic acid" to "asam folat vitamin b9",
    "Furosemide" to "furosemid",
    "Hydrochlorothiazide" to "hidroklorotiazid hct",
    "Hydrocortisone" to "hidrokortison",
    "Lactulose" to "laktulosa",
    "Levothyroxine" to "levotiroksin",
    "Metoclopramide" to "metoklopramid",
    "Metronidazole" to "metronidazol",
    "Miconazole" to "mikonazol",
    "Nystatin" to "nistatin",
    "Ondansetron" to "ondansetron",
    "Oral rehydration salts" to "oralit garam rehidrasi oral",
    "Oseltamivir" to "oseltamivir",
    "Fluconazole" to "flukonazol",
    "Mebendazole" to "mebendazol",
    "Nitrofurantoin" to "nitrofurantoin",
    "Zinc sulfate" to "seng sulfat zink zinc",
).map { (name, aliases) -> CatalogMedicine(name, aliases, WHO_MEDICINES) } + listOf(
    "Ascorbic acid" to "asam askorbat vitamin c",
    "Cholecalciferol" to "kolekalsiferol vitamin d3",
    "Pyridoxine" to "piridoksin vitamin b6",
    "Retinol" to "vitamin a",
    "Riboflavin" to "vitamin b2",
    "Thiamine" to "tiamin vitamin b1",
    "Calcium gluconate" to "kalsium glukonat",
).map { (name, aliases) -> CatalogMedicine(name, aliases, WHO_VITAMINS) }
