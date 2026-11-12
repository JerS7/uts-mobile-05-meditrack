package com.example.meditrack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.meditrack.data.MediTrackDatabase
import com.example.meditrack.ui.LogScreen
import com.example.meditrack.ui.state.MedicationFormState
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
        val database = MediTrackDatabase.open(context, databaseName)
        try {
            compose.setContent {
                MediTrackTheme { LogScreen(form = MedicationFormState(), searchMedicines = database::suggestions) }
            }
            val field = compose.onNodeWithContentDescription("Nama Obat atau Suplemen")
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
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
