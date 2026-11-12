package com.example.meditrack.util

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class DatesTest {
    @Test fun todayUsesTheLocalCalendarDay() {
        val jakarta = TimeZone.getTimeZone("Asia/Jakarta")
        val lateEvening = Calendar.getInstance(jakarta).apply { clear(); set(2026, Calendar.NOVEMBER, 12, 23, 30) }.timeInMillis
        assertEquals("2026-11-12", Dates.today(lateEvening, jakarta))
        // The same moment is already the next day further east.
        assertEquals("2026-11-13", Dates.today(lateEvening, TimeZone.getTimeZone("Asia/Jayapura")))
    }

    @Test fun pickerMillisRoundTrip() {
        val millis = Dates.toUtcMillis("2026-02-28")!!
        assertEquals("2026-02-28", Dates.fromUtcMillis(millis))
        assertEquals("2026-03-01", Dates.fromUtcMillis(millis + 24 * 60 * 60 * 1000))
    }

    @Test fun invalidTextIsRejected() {
        assertFalse(Dates.isValid("2026-02-30"))
        assertFalse(Dates.isValid("12/11/2026"))
        assertFalse(Dates.isValid(""))
        assertTrue(Dates.isValid("2024-02-29"))
    }

    @Test fun previousDayCrossesMonthAndYear() {
        assertEquals("2025-12-31", Dates.previousDay("2026-01-01"))
        assertNull(Dates.previousDay("bad"))
    }

    @Test fun displayIsShortAndLocalised() {
        assertEquals("12 Nov 2026", Dates.display("2026-11-12", Locale.ENGLISH))
        assertEquals("not-a-date", Dates.display("not-a-date"))
    }
}
