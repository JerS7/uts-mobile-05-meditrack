package com.example.meditrack.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Record dates are stored as ISO text (yyyy-MM-dd) in the device's local calendar.
 * Text sorts in date order and needs no java.time desugaring on API 24-25.
 */
object Dates {
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val indonesian: Locale = Locale.forLanguageTag("id-ID")

    private fun isoFormat(zone: TimeZone) =
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = zone; isLenient = false }

    fun today(now: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): String =
        isoFormat(zone).format(Date(now))

    fun isValid(date: String): Boolean = toUtcMillis(date) != null

    /** Material date pickers use midnight UTC for the selected day. */
    fun toUtcMillis(date: String): Long? {
        if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(date)) return null
        return try { isoFormat(utc).parse(date)?.time } catch (_: ParseException) { null }
    }

    fun fromUtcMillis(millis: Long): String = isoFormat(utc).format(Date(millis))

    /** The day before [date], used to label "yesterday" in the history list. */
    fun previousDay(date: String): String? = toUtcMillis(date)?.let { fromUtcMillis(it - DAY_MILLIS) }

    /** For example "12 Nov 2026". Falls back to the stored text when it cannot be parsed. */
    fun display(date: String, locale: Locale = indonesian): String {
        val millis = toUtcMillis(date) ?: return date
        return SimpleDateFormat("d MMM yyyy", locale).apply { timeZone = utc }.format(Date(millis))
    }

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
}
