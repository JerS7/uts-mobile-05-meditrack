package com.example.meditrack.ui.state

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediTrackStateTest {
    @Test
    fun newSessionStartsEmpty() {
        assertTrue(MediTrackState().records.isEmpty())
    }

    @Test
    fun addedRecordsKeepTheirValuesAndInsertionOrder() {
        val state = MediTrackState()
        val taken = record(DoseStatus.TAKEN)
        val missed = record(DoseStatus.MISSED)

        state.addRecord(taken)
        state.addRecord(missed)

        assertEquals(listOf(taken, missed), state.records)
        assertEquals("08:45 AM", state.records.first().actualTime)
        assertEquals(null, state.records.last().actualTime)
        assertNotEquals(taken.id, missed.id)
    }

    @Test
    fun stateOwnsItsDataAndPreviouslyReadSnapshotsStayUnchanged() {
        val initial = mutableListOf(record(DoseStatus.TAKEN))
        val state = MediTrackState(initial)
        val snapshot = state.records
        initial.clear()

        state.addRecord(record(DoseStatus.MISSED))

        assertEquals(1, snapshot.size)
        assertEquals(2, state.records.size)
    }

    @Test
    fun recordsAreNotSharedOrPersistedAcrossSessions() {
        val firstSession = MediTrackState()
        firstSession.addRecord(record(DoseStatus.TAKEN))

        assertTrue(MediTrackState().records.isEmpty())
        assertEquals(1, firstSession.records.size)
    }

    @Test
    fun addingRecordNotifiesReadersSoComposeCanUpdateTheScreen() {
        val state = MediTrackState()
        var changed = false
        val observer = SnapshotStateObserver { callback -> callback() }
        observer.start()
        try {
            observer.observeReads(scope = Unit, onValueChangedForScope = { changed = true }) {
                state.records
            }

            state.addRecord(record(DoseStatus.TAKEN))
            Snapshot.sendApplyNotifications()

            assertTrue(changed)
        } finally {
            observer.stop()
            observer.clear()
        }
    }

    private fun record(status: DoseStatus) = MedicationRecord(
        name = "Multivitamin",
        scheduledTime = "08:30 AM",
        actualTime = if (status == DoseStatus.TAKEN) "08:45 AM" else null,
        note = "After breakfast",
        status = status,
    )
}
