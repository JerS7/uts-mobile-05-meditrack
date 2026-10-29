package com.example.meditrack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.meditrack.data.demoMedicationRecords
import com.example.meditrack.ui.MediTrackScreen
import com.example.meditrack.ui.theme.MediTrackTheme
import org.junit.Rule
import org.junit.Test

class MedicationListTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lazyListDisplaysEachDemoRecordAndCount() {
        compose.setContent { MediTrackTheme { MediTrackScreen(records = demoMedicationRecords) } }
        val list = compose.onNodeWithTag("medication-list")
        list.performScrollToKey("records-header")
        compose.onNodeWithText("3 Records").assertIsDisplayed()
        demoMedicationRecords.forEach {
            list.performScrollToKey("record-${it.id}")
            compose.onNodeWithText(it.name).assertIsDisplayed()
        }
    }

    @Test fun emptyStateStillWorksWithoutRecords() {
        compose.setContent { MediTrackTheme { MediTrackScreen() } }
        compose.onNodeWithTag("medication-list").performScrollToKey("empty-records")
        compose.onNodeWithText("No records yet").assertIsDisplayed()
    }
}
