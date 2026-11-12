package com.example.meditrack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.meditrack.R
import com.example.meditrack.data.demoMedicationRecords
import com.example.meditrack.model.MedicationRecord
import com.example.meditrack.ui.state.StatusFilter
import com.example.meditrack.ui.state.filtered
import com.example.meditrack.ui.state.summary
import com.example.meditrack.ui.theme.*
import com.example.meditrack.util.Dates

/** Every record grouped by day, with status/date filters, edit, and delete. */
@Composable
fun HistoryScreen(
    records: List<MedicationRecord>,
    modifier: Modifier = Modifier,
    today: String = Dates.today(),
    onEdit: (MedicationRecord) -> Unit = {},
    onDelete: (MedicationRecord) -> Unit = {},
) {
    var status by rememberSaveable { mutableStateOf(StatusFilter.ALL) }
    var date by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<MedicationRecord?>(null) }
    val shown = records.filtered(status, date)
    val summary = shown.summary()

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_message, record.name, Dates.display(record.date))) },
            confirmButton = {
                TextButton(onClick = { pendingDelete = null; onDelete(record) }) {
                    Text(stringResource(R.string.action_delete), color = MissedRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel), color = TextSecondary) }
            },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("history-list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "title") { PageTitle(stringResource(R.string.history_title), stringResource(R.string.history_subtitle)) }
        item(key = "filters") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusFilter.entries.forEach { option ->
                        FilterChip(
                            selected = status == option,
                            onClick = { status = option },
                            label = {
                                Text(stringResource(when (option) {
                                    StatusFilter.ALL -> R.string.filter_all
                                    StatusFilter.TAKEN -> R.string.status_taken
                                    StatusFilter.MISSED -> R.string.status_missed
                                }))
                            },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField(
                        stringResource(R.string.filter_date), date, { date = it }, Modifier.weight(1f),
                        emptyText = stringResource(R.string.filter_all_dates), latestDate = today,
                    )
                    if (date != null) TextButton(onClick = { date = null }) { Text(stringResource(R.string.filter_clear_date), color = Teal) }
                }
            }
        }
        item(key = "records-header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RecordCountHeader(stringResource(R.string.history_records), shown.size)
                if (shown.isNotEmpty()) {
                    Text(stringResource(R.string.history_summary, summary.taken, summary.missed, summary.percent), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
        if (shown.isEmpty()) {
            item(key = "empty-records") {
                if (records.isEmpty()) EmptyRecords()
                else EmptyRecords(stringResource(R.string.empty_filter_title), stringResource(R.string.empty_filter_message))
            }
        } else {
            shown.groupBy { it.date }.forEach { (day, dayRecords) ->
                item(key = "day-$day", contentType = "day-header") {
                    Text(
                        dayLabel(day, today),
                        style = MaterialTheme.typography.titleSmall, color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
                    )
                }
                items(dayRecords, key = { "record-${it.id}" }, contentType = { "medication-record" }) { record ->
                    RecordCard(record, onEdit = { onEdit(record) }, onDelete = { pendingDelete = record })
                }
            }
        }
    }
}

@Composable
private fun dayLabel(day: String, today: String): String = when (day) {
    today -> stringResource(R.string.day_today, Dates.display(day))
    Dates.previousDay(today) -> stringResource(R.string.day_yesterday, Dates.display(day))
    else -> Dates.display(day)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1200)
@Composable
private fun HistoryScreenPreview() {
    MediTrackTheme { HistoryScreen(records = demoMedicationRecords()) }
}
