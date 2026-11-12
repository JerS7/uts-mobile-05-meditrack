package com.example.meditrack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.meditrack.data.demoMedicationRecords
import com.example.meditrack.model.MedicationRecord
import com.example.meditrack.ui.HistoryScreen
import com.example.meditrack.ui.HomeScreen
import com.example.meditrack.ui.theme.MediTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MedicationListTest {
    @get:Rule val compose = createComposeRule()
    private val today = "2026-11-12"

    @Test fun homeShowsTodaysDemoRecordsAndCount() {
        val records = demoMedicationRecords(today)
        compose.setContent { MediTrackTheme { HomeScreen(records = records, today = today) } }
        val list = compose.onNodeWithTag("home-list")
        list.performScrollToKey("records-header")
        compose.onNodeWithText("3 Catatan").assertIsDisplayed()
        records.forEach {
            list.performScrollToKey("record-${it.id}")
            compose.onNodeWithText(it.name).assertIsDisplayed()
        }
    }

    @Test fun firstLaunchShowsEmptyStateWithSampleDataButton() {
        var demoRequested = false
        compose.setContent { MediTrackTheme { HomeScreen(records = emptyList(), today = today, onAddDemoRecords = { demoRequested = true }) } }
        compose.onNodeWithTag("home-list").performScrollToKey("empty-records")
        compose.onNodeWithText("Belum ada catatan").assertIsDisplayed()
        compose.onNodeWithText("Isi data contoh").performClick()
        assertEquals(true, demoRequested)
    }

    @Test fun olderRecordsOnlyAppearInHistory() {
        val records = demoMedicationRecords("2026-11-11")
        compose.setContent { MediTrackTheme { HomeScreen(records = records, today = today) } }
        compose.onNodeWithTag("home-list").performScrollToKey("empty-today")
        compose.onNodeWithText("Belum ada catatan hari ini").assertIsDisplayed()
    }

    @Test fun historyFiltersByStatus() {
        val records: List<MedicationRecord> = demoMedicationRecords(today)
        compose.setContent { MediTrackTheme { HistoryScreen(records = records, today = today) } }
        compose.onNodeWithText("3 Catatan").assertIsDisplayed()
        compose.onNode(hasText("Terlewat") and hasClickAction()).performClick()
        compose.onNodeWithText("1 Catatan").assertIsDisplayed()
        compose.onNodeWithTag("history-list").performScrollToKey("record-uts-demo-c")
        compose.onNodeWithText("Loratadine").assertIsDisplayed()
        compose.onAllNodesWithText("Paracetamol").assertCountEquals(0)
    }
}
