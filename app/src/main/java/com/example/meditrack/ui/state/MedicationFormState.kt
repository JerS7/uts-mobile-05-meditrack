package com.example.meditrack.ui.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.meditrack.R
import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord
import com.example.meditrack.util.Dates
import java.util.UUID

const val MAX_NAME_LENGTH = 60
const val MAX_NOTE_LENGTH = 160

/**
 * Holds what the user is typing in the "Catat Konsumsi Obat" form, for a new record or
 * for editing an existing one ([editingId] is then the record being changed).
 */
class MedicationFormState(
    name: String = "",
    scheduledTime: String? = null,
    actualTime: String? = null,
    note: String = "",
    status: DoseStatus = DoseStatus.TAKEN,
    showErrors: Boolean = false,
    date: String? = null,
    editingId: String? = null,
    private val today: () -> String = { Dates.today() },
) {
    var name by mutableStateOf(name)
        private set
    var scheduledTime by mutableStateOf(scheduledTime)
    var actualTime by mutableStateOf(actualTime)
        private set
    var note by mutableStateOf(note)
        private set
    var status by mutableStateOf(status)
        private set
    var date by mutableStateOf(date ?: today())
        private set
    var editingId by mutableStateOf(editingId)
        private set

    val isEditing: Boolean
        get() = editingId != null

    /** A missed dose has no actual time, so the field is locked while Missed is selected. */
    val isActualTimeEnabled: Boolean
        get() = status == DoseStatus.TAKEN

    // Errors only appear after the first submit attempt, not while the user is still typing.
    var showErrors by mutableStateOf(showErrors)
        private set

    fun updateName(value: String) {
        name = value.take(MAX_NAME_LENGTH)
    }

    fun updateNote(value: String) {
        note = value.take(MAX_NOTE_LENGTH)
    }

    fun updateActualTime(value: String) {
        if (isActualTimeEnabled) actualTime = value
    }

    fun updateDate(value: String) {
        if (Dates.isValid(value)) date = value
    }

    fun selectStatus(value: DoseStatus) {
        status = value
        if (value == DoseStatus.MISSED) actualTime = null
    }

    /** Fills the form with an existing record so it can be changed and saved again. */
    fun load(record: MedicationRecord) {
        name = record.name
        scheduledTime = record.scheduledTime
        actualTime = record.actualTime
        note = record.note
        status = record.status
        date = record.date
        editingId = record.id
        showErrors = false
    }

    /** Starts a new record from a reminder: today's date, its name and time, taken now. */
    fun prefillFromReminder(medicine: String, scheduled: String, takenAt: String?) {
        reset()
        updateName(medicine)
        scheduledTime = scheduled
        actualTime = takenAt
    }

    /** Back to a clean form: empty fields, today's date, Taken selected, no error messages. */
    fun reset() {
        name = ""
        scheduledTime = null
        actualTime = null
        note = ""
        status = DoseStatus.TAKEN
        date = today()
        editingId = null
        showErrors = false
    }

    @get:StringRes
    val nameError: Int?
        get() = if (name.isBlank()) R.string.error_name_required else null

    @get:StringRes
    val scheduledTimeError: Int?
        get() = if (scheduledTime == null) R.string.error_scheduled_time_required else null

    @get:StringRes
    val actualTimeError: Int?
        get() = if (isActualTimeEnabled && actualTime == null) R.string.error_actual_time_required else null

    /** ISO dates compare correctly as text. A dose cannot be logged for a future day. */
    @get:StringRes
    val dateError: Int?
        get() = if (date > today()) R.string.error_date_in_future else null

    val isValid: Boolean
        get() = nameError == null && scheduledTimeError == null && actualTimeError == null && dateError == null

    /**
     * Returns the record when every required field is filled. A new record gets a new ID;
     * an edited record keeps its ID. When something is missing, the form keeps its values
     * and turns on error messages.
     */
    fun submit(resetAfterSubmit: Boolean = true): MedicationRecord? {
        showErrors = true
        if (!isValid) return null
        val record = MedicationRecord(
            name = name.trim(),
            scheduledTime = scheduledTime!!,
            actualTime = if (status == DoseStatus.TAKEN) actualTime else null,
            note = note.trim(),
            status = status,
            date = date,
            id = editingId ?: UUID.randomUUID().toString(),
        )
        if (resetAfterSubmit) reset()
        return record
    }

    companion object {
        val Saver = listSaver<MedicationFormState, Any?>(
            save = {
                listOf(it.name, it.scheduledTime, it.actualTime, it.note, it.status.name, it.showErrors, it.date, it.editingId)
            },
            restore = {
                MedicationFormState(
                    name = it[0] as String,
                    scheduledTime = it[1] as String?,
                    actualTime = it[2] as String?,
                    note = it[3] as String,
                    status = DoseStatus.valueOf(it[4] as String),
                    showErrors = it[5] as Boolean,
                    date = it[6] as String,
                    editingId = it[7] as String?,
                )
            },
        )
    }
}

/** Survives screen rotation and switching pages; the draft is not written to the database. */
@Composable
fun rememberMedicationFormState(): MedicationFormState =
    rememberSaveable(saver = MedicationFormState.Saver) { MedicationFormState() }

/** Formats a 24-hour picker value into the UI format, for example 08:30 AM. */
fun formatTime12h(hour: Int, minute: Int): String {
    require(hour in 0..23 && minute in 0..59) { "Invalid time $hour:$minute" }
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when (val h = hour % 12) { 0 -> 12; else -> h }
    return "%02d:%02d %s".format(java.util.Locale.ROOT, displayHour, minute, period)
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
