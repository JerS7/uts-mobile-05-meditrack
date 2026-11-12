package com.example.meditrack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.meditrack.data.demoMedicationRecords
import com.example.meditrack.model.MedicationRecord
import com.example.meditrack.ui.HistoryScreen
import com.example.meditrack.ui.LogScreen
import com.example.meditrack.ui.state.MedicationFormState
import com.example.meditrack.ui.theme.MediTrackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EditDeleteUiTest {
    @get:Rule val compose = createComposeRule()
    private val today = "2026-11-12"

    @Test fun deleteAsksForConfirmationFirst() {
        val deleted = mutableListOf<MedicationRecord>()
        compose.setContent {
            MediTrackTheme { HistoryScreen(records = demoMedicationRecords(today), today = today, onDelete = { deleted += it }) }
        }
        compose.onNodeWithTag("history-list").performScrollToKey("record-uts-demo-a")
        compose.onNodeWithContentDescription("Hapus Paracetamol").performClick()
        compose.onNodeWithText("Hapus catatan?").assertIsDisplayed()
        compose.onNodeWithText("Batal").performClick()
        assertTrue(deleted.isEmpty())

        compose.onNodeWithContentDescription("Hapus Paracetamol").performClick()
        compose.onNode(hasText("Hapus") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(listOf("uts-demo-a"), deleted.map { it.id })
    }

    @Test fun editingSavesTheSameRecordWithChanges() {
        val original = demoMedicationRecords(today).first()
        val form = MedicationFormState().apply { load(original) }
        var saved: MedicationRecord? = null
        var wasEditing = false
        compose.setContent {
            MediTrackTheme {
                LogScreen(form = form, onSave = { saved = it }, onSaved = { _, editing -> wasEditing = editing })
            }
        }
        compose.onNodeWithText("Ubah Catatan").assertIsDisplayed()
        val field = compose.onNodeWithContentDescription("Nama Obat atau Suplemen")
        field.performTextClearance()
        field.performTextInput("Paracetamol 500")
        compose.onNodeWithText("Simpan Perubahan").performScrollTo().performClick()
        compose.waitUntil(5000) { saved != null }
        assertEquals(original.copy(name = "Paracetamol 500"), saved)
        assertTrue(wasEditing)
    }
}
