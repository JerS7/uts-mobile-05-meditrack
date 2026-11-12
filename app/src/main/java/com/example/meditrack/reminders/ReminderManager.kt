package com.example.meditrack.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.meditrack.MainActivity
import com.example.meditrack.R
import com.example.meditrack.data.MediTrackDatabase
import com.example.meditrack.data.toEntity
import com.example.meditrack.model.DoseStatus
import com.example.meditrack.model.MedicationRecord
import com.example.meditrack.ui.state.formatTime12h
import com.example.meditrack.util.Dates
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.util.Calendar

class ReminderManager private constructor(private val context: Context) {
    private val database = MediTrackDatabase.getInstance(context)
    private val dao = database.reminders()
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val mutex = Mutex()
    private val workScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private suspend fun <T> update(block: suspend () -> T): T =
        workScope.async { mutex.withLock { block() } }.await()
    val reminders = dao.observe()

    fun exactAllowed() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
    fun notificationsAllowed(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val channel = if (Build.VERSION.SDK_INT >= 26) context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL) else null
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun pending(id: String): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderReceiver::class.java).apply {
            data = Uri.parse("meditrack://reminder/$id")
            putExtra(EXTRA_REMINDER_ID, id)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun schedule(reminder: MedicationReminder) {
        val time = nextReminderTime(reminder.hour, reminder.minute)
        val intent = pending(reminder.id)
        if (exactAllowed()) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, intent)
                return
            } catch (_: SecurityException) { /* Permission may have just changed. */ }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, intent)
    }

    suspend fun save(reminder: MedicationReminder) = update {
        require(reminder.medicine.isNotBlank() && reminder.medicine.length <= 60)
        require(reminder.hour in 0..23 && reminder.minute in 0..59)
        val previous = dao.find(reminder.id)
        dao.save(reminder.copy(medicine = reminder.medicine.trim()))
        try {
            if (reminder.enabled) schedule(reminder) else cancel(reminder.id)
        } catch (error: Exception) {
            if (previous == null) dao.delete(reminder.id) else dao.save(previous)
            throw error
        }
    }

    suspend fun delete(id: String) = update {
        dao.delete(id)
        cancel(id)
    }

    private fun cancel(id: String) {
        alarms.cancel(pending(id))
        NotificationManagerCompat.from(context).cancel(id, 0)
    }

    suspend fun reschedule() = update {
        dao.enabled().forEach { schedule(it) }
    }

    suspend fun deliver(id: String) = update {
        val reminder = dao.find(id)?.takeIf { it.enabled } ?: return@update
        // Schedule tomorrow even when notifications have been disabled.
        schedule(reminder)
        createChannel()
        if (!notificationsAllowed()) return@update
        val firedAt = System.currentTimeMillis()
        val scheduledTime = formatTime12h(reminder.hour, reminder.minute)
        // Tapping the notification opens the form with the name and time already filled in.
        val open = PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_LOG_DOSE
                data = Uri.parse("meditrack://log/$id")
                putExtra(MainActivity.EXTRA_MEDICINE, reminder.medicine)
                putExtra(MainActivity.EXTRA_SCHEDULED_TIME, scheduledTime)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val text = context.getString(R.string.notification_text, reminder.medicine)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notification_big_text, reminder.medicine)))
            .setContentIntent(open)
            .addAction(R.drawable.ic_reminder, context.getString(R.string.notification_action_taken),
                actionIntent(id, DoseStatus.TAKEN, firedAt))
            .addAction(R.drawable.ic_reminder, context.getString(R.string.notification_action_missed),
                actionIntent(id, DoseStatus.MISSED, firedAt))
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try { NotificationManagerCompat.from(context).notify(id, 0, notification) }
        catch (_: SecurityException) { /* The user can revoke notification access at any time. */ }
    }

    private fun actionIntent(id: String, status: DoseStatus, firedAt: Long): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderActionReceiver::class.java).apply {
            data = Uri.parse("meditrack://reminder/$id/${status.name.lowercase()}")
            putExtra(EXTRA_REMINDER_ID, id)
            putExtra(EXTRA_STATUS, status.name)
            putExtra(EXTRA_FIRED_AT, firedAt)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /**
     * Saves a Taken or Missed record from a notification button. The record ID is fixed per
     * reminder and day, so pressing a button twice (or changing Taken to Missed) updates one record.
     */
    suspend fun logFromNotification(id: String, status: DoseStatus, firedAt: Long, now: Long = System.currentTimeMillis()) = update {
        NotificationManagerCompat.from(context).cancel(id, 0)
        val reminder = dao.find(id) ?: return@update
        val date = Dates.today(firedAt)
        val actual = Calendar.getInstance().apply { timeInMillis = now }
        database.medicationDao().save(
            MedicationRecord(
                id = notificationRecordId(id, date),
                name = reminder.medicine,
                scheduledTime = formatTime12h(reminder.hour, reminder.minute),
                actualTime = if (status == DoseStatus.TAKEN) formatTime12h(actual.get(Calendar.HOUR_OF_DAY), actual.get(Calendar.MINUTE)) else null,
                note = context.getString(R.string.notification_record_note),
                status = status,
                date = date,
            ).toEntity(),
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.notification_channel_description)
                })
        }
    }

    companion object {
        const val CHANNEL = "medication_reminders"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_STATUS = "status"
        const val EXTRA_FIRED_AT = "fired_at"
        fun notificationRecordId(reminderId: String, date: String) = "reminder-$reminderId-$date"

        @Volatile private var instance: ReminderManager? = null
        fun get(context: Context): ReminderManager = instance ?: synchronized(this) {
            instance ?: ReminderManager(context.applicationContext).also { instance = it }
        }
    }
}
