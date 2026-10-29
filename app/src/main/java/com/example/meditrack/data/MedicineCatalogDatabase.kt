package com.example.meditrack.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Locale

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

/** Reference names only. This database is separate from the user's medication records. */
@Database(entities = [CatalogMedicine::class], version = 1, exportSchema = true)
abstract class MedicineCatalogDatabase : RoomDatabase() {
    abstract fun medicines(): MedicineCatalogDao

    suspend fun suggestions(query: String): List<String> {
        val text = query.trim().lowercase(Locale.ROOT)
        return if (text.length < 2) emptyList() else medicines().search(text)
    }

    companion object {
        fun open(context: Context, name: String = "medicine-catalog.db"): MedicineCatalogDatabase =
            Room.databaseBuilder(context.applicationContext, MedicineCatalogDatabase::class.java, name)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        catalogSeed.forEach { medicine ->
                            db.execSQL("INSERT INTO medicines (name, aliases, source) VALUES (?, ?, ?)",
                                arrayOf(medicine.name, medicine.aliases, medicine.source))
                        }
                    }
                }).build()
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
