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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

class ReminderManager private constructor(private val context: Context) {
    private val dao = ReminderDatabase.open(context).reminders()
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
            putExtra("reminder_id", id)
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
        val open = PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle("Pengingat Minum Obat")
            .setContentText("Waktunya minum ${reminder.medicine}.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Waktunya minum ${reminder.medicine}. Buka MediTrack untuk mencatat dosis Anda."))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try { NotificationManagerCompat.from(context).notify(id, 0, notification) }
        catch (_: SecurityException) { /* The user can revoke notification access at any time. */ }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "Pengingat Minum Obat", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Pengingat harian untuk obat dan waktu yang Anda pilih"
                })
        }
    }

    companion object {
        const val CHANNEL = "medication_reminders"
        @Volatile private var instance: ReminderManager? = null
        fun get(context: Context): ReminderManager = instance ?: synchronized(this) {
            instance ?: ReminderManager(context.applicationContext).also { instance = it }
        }
    }
}
