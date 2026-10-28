package com.example.meditrack.ui

import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord

// Preserve the wireframe examples while the form and summary are connected in later stages.
internal val sampleMedicationRecords = listOf(
    MedicationRecord(
        id = "sample-1",
        name = "Sertraline 50mg",
        scheduledTime = "08:00 AM",
        actualTime = "08:15 AM",
        note = "Taken after breakfast in student dining hall",
        status = DoseStatus.TAKEN,
    ),
    MedicationRecord(
        id = "sample-2",
        name = "Daily Multivitamin + Iron",
        scheduledTime = "12:30 PM",
        actualTime = "12:40 PM",
        note = "With lunch before chemistry lab",
        status = DoseStatus.TAKEN,
    ),
    MedicationRecord(
        id = "sample-3",
        name = "Allergy Relief (Cetirizine)",
        scheduledTime = "07:00 PM",
        actualTime = null,
        note = "Stayed late in library study group, forgot pack",
        status = DoseStatus.MISSED,
    ),
)
