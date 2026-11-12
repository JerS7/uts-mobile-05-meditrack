package com.example.meditrack.reminders

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.meditrack.model.DoseStatus
import kotlinx.coroutines.*

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(ReminderManager.EXTRA_REMINDER_ID) ?: return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { ReminderManager.get(context).deliver(id) }
            catch (error: Exception) { Log.e("MediTrack", "Unable to deliver reminder", error) }
            finally { result.finish() }
        }
    }
}

/** Handles the "Sudah diminum" and "Terlewat" buttons on a reminder notification. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(ReminderManager.EXTRA_REMINDER_ID) ?: return
        val status = intent.getStringExtra(ReminderManager.EXTRA_STATUS)
            ?.let { runCatching { DoseStatus.valueOf(it) }.getOrNull() } ?: return
        val firedAt = intent.getLongExtra(ReminderManager.EXTRA_FIRED_AT, System.currentTimeMillis())
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { ReminderManager.get(context).logFromNotification(id, status, firedAt) }
            catch (error: Exception) { Log.e("MediTrack", "Unable to log dose from reminder", error) }
            finally { result.finish() }
        }
    }
}

class RestoreRemindersReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { ReminderManager.get(context).reschedule() }
            catch (error: Exception) { Log.e("MediTrack", "Unable to restore reminders", error) }
            finally { result.finish() }
        }
    }
}
