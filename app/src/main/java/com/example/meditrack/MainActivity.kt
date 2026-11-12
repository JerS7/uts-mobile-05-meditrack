package com.example.meditrack

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.example.meditrack.reminders.ReminderManager
import com.example.meditrack.ui.DosePrefill
import com.example.meditrack.ui.MediTrackApp
import com.example.meditrack.ui.theme.MediTrackTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** Set when the activity is opened from a reminder notification. */
    private val prefill = mutableStateOf<DosePrefill?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.BLACK,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.BLACK,
            ),
        )
        // After rotation the form already holds the prefilled values; do not overwrite edits.
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            MediTrackTheme {
                MediTrackApp(prefill = prefill.value, onPrefillHandled = { prefill.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Alarms can be dropped by the system (for example after a force stop); set them again.
        lifecycleScope.launch {
            try { ReminderManager.get(this@MainActivity).reschedule() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { Log.e("MediTrack", "Unable to reschedule reminders", error) }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action != ACTION_LOG_DOSE) return
        val medicine = intent.getStringExtra(EXTRA_MEDICINE) ?: return
        val time = intent.getStringExtra(EXTRA_SCHEDULED_TIME) ?: return
        prefill.value = DosePrefill(medicine, time)
    }

    companion object {
        const val ACTION_LOG_DOSE = "com.example.meditrack.action.LOG_DOSE"
        const val EXTRA_MEDICINE = "medicine"
        const val EXTRA_SCHEDULED_TIME = "scheduled_time"
    }
}
