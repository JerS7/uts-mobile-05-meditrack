package com.example.meditrack

import androidx.test.platform.app.InstrumentationRegistry
import com.example.meditrack.data.MedicineCatalogDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MedicineCatalogTest {
    @Test
    fun catalogIsSeededSearchableAndPersistsWithoutDuplicates() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "catalog-test.db"
        context.deleteDatabase(name)
        var database = MedicineCatalogDatabase.open(context, name)
        try {
            assertEquals(30, database.medicines().count())
            assertEquals(listOf("Paracetamol"), database.suggestions("  PARA  "))
            assertEquals(listOf("Paracetamol"), database.suggestions("parasetamol"))
            assertEquals(listOf("Ascorbic acid"), database.suggestions("vitamin c"))
            assertTrue(database.suggestions("a").isEmpty())
            assertTrue(database.suggestions("   ").isEmpty())
            assertTrue(database.suggestions("%").isEmpty())
            assertTrue(database.suggestions("unlikelyname").isEmpty())
            assertTrue(database.suggestions("in").size <= 5)
            database.close()
            database = MedicineCatalogDatabase.open(context, name)
            assertEquals(30, database.medicines().count())
            assertEquals(listOf("Amoxicillin"), database.suggestions("amoksi"))
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
