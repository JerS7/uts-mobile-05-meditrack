package com.example.meditrack.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord

const val MAX_NAME_LENGTH = 60
const val MAX_NOTE_LENGTH = 160

/** Holds what the user is typing in the "Log Medication Record" form. */
class MedicationFormState(
    name: String = "",
    scheduledTime: String? = null,
    actualTime: String? = null,
    note: String = "",
    showErrors: Boolean = false,
) {
    var name by mutableStateOf(name)
        private set
    var scheduledTime by mutableStateOf(scheduledTime)
    var actualTime by mutableStateOf(actualTime)
    var note by mutableStateOf(note)
        private set

    // Errors only appear after the first submit attempt, not while the user is still typing.
    var showErrors by mutableStateOf(showErrors)
        private set

    fun updateName(value: String) {
        name = value.take(MAX_NAME_LENGTH)
    }

    fun updateNote(value: String) {
        note = value.take(MAX_NOTE_LENGTH)
    }

    val nameError: String?
        get() = if (name.isBlank()) "Medication name is required" else null

    val scheduledTimeError: String?
        get() = if (scheduledTime == null) "Choose a scheduled time" else null

    val actualTimeError: String?
        get() = if (actualTime == null) "Choose the time you took it" else null

    val isValid: Boolean
        get() = nameError == null && scheduledTimeError == null && actualTimeError == null

    /** Returns a new record when every required field is filled, otherwise turns on error messages. */
    fun submit(status: DoseStatus): MedicationRecord? {
        showErrors = true
        if (!isValid) return null
        return MedicationRecord(
            name = name.trim(),
            scheduledTime = scheduledTime!!,
            actualTime = actualTime,
            note = note.trim(),
            status = status,
        )
    }

    companion object {
        val Saver = listSaver<MedicationFormState, Any?>(
            save = { listOf(it.name, it.scheduledTime, it.actualTime, it.note, it.showErrors) },
            restore = {
                MedicationFormState(
                    name = it[0] as String,
                    scheduledTime = it[1] as String?,
                    actualTime = it[2] as String?,
                    note = it[3] as String,
                    showErrors = it[4] as Boolean,
                )
            },
        )
    }
}

/** Survives screen rotation; still only lives in memory like [MediTrackState]. */
@Composable
fun rememberMedicationFormState(): MedicationFormState =
    rememberSaveable(saver = MedicationFormState.Saver) { MedicationFormState() }

/** Formats a 24-hour picker value into the UI format, for example 08:30 AM. */
fun formatTime12h(hour: Int, minute: Int): String {
    require(hour in 0..23 && minute in 0..59) { "Invalid time $hour:$minute" }
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when (val h = hour % 12) { 0 -> 12; else -> h }
    return "%02d:%02d %s".format(displayHour, minute, period)
}

/** Reads "08:30 AM" back into a 24-hour pair so the picker can reopen on the saved value. */
fun parseTime12h(text: String): Pair<Int, Int>? {
    val match = Regex("""^(\d{1,2}):(\d{2}) (AM|PM)$""").matchEntire(text.trim()) ?: return null
    val (h, m, period) = match.destructured
    val hour = h.toInt()
    val minute = m.toInt()
    if (hour !in 1..12 || minute !in 0..59) return null
    val hour24 = when {
        period == "AM" && hour == 12 -> 0
        period == "PM" && hour != 12 -> hour + 12
        else -> hour
    }
    return hour24 to minute
}