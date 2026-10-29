package com.example.meditrack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.meditrack.data.MedicineCatalogDatabase
import com.example.meditrack.ui.MediTrackScreen
import com.example.meditrack.ui.theme.MediTrackTheme
import org.junit.Rule
import org.junit.Test

class MedicineSearchUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun typingFindsCatalogNameAndSelectionFillsEditableField() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "catalog-ui-test.db"
        context.deleteDatabase(databaseName)
        val catalog = MedicineCatalogDatabase.open(context, databaseName)
        try {
            compose.setContent {
                MediTrackTheme { MediTrackScreen(searchMedicines = catalog::suggestions) }
            }
            val field = compose.onNodeWithContentDescription("Medication or Supplement Name")
            field.performScrollTo().performTextInput("parasetamol")
            compose.waitUntil(5000) {
                compose.onAllNodesWithText("Paracetamol", substring = false).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Paracetamol", substring = false).performScrollTo().performClick()
            field.assertTextContains("Paracetamol")
            field.performTextClearance()
            field.performTextInput("My supplement")
            field.assertTextContains("My supplement")
        } finally {
            catalog.close()
            context.deleteDatabase(databaseName)
        }
    }
}
