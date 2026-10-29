package com.example.meditrack.reminders

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class ReminderTimeTest {
    private fun time(zone: TimeZone, day: Int, hour: Int, minute: Int, month: Int = Calendar.OCTOBER): Long =
        Calendar.getInstance(zone).apply {
            clear(); set(2026, month, day, hour, minute, 0)
        }.timeInMillis

    @Test fun futureTimeUsesTodayAndPastOrEqualTimeUsesTomorrow() {
        val zone = TimeZone.getTimeZone("Asia/Jakarta")
        val now = time(zone, 29, 8, 30)
        assertEquals(time(zone, 29, 9, 0), nextReminderTime(9, 0, now, zone))
        assertEquals(time(zone, 30, 8, 0), nextReminderTime(8, 0, now, zone))
        assertEquals(time(zone, 30, 8, 30), nextReminderTime(8, 30, now, zone))
    }

    @Test fun midnightAndTimezoneChangesUseLocalWallTime() {
        val jakarta = TimeZone.getTimeZone("Asia/Jakarta")
        val bali = TimeZone.getTimeZone("Asia/Makassar")
        val now = time(jakarta, 29, 23, 59)
        assertEquals(time(jakarta, 30, 0, 0), nextReminderTime(0, 0, now, jakarta))
        assertEquals(time(bali, 30, 8, 0), nextReminderTime(8, 0, now, bali))
    }

    @Test fun dailyTimeSurvivesDaylightSavingTransition() {
        val zone = TimeZone.getTimeZone("America/New_York")
        val now = time(zone, 31, 9, 0)
        val expected = time(zone, 1, 8, 0, Calendar.NOVEMBER)
        assertEquals(expected, nextReminderTime(8, 0, now, zone))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidTimeIsRejected() { nextReminderTime(24, 0) }
}
