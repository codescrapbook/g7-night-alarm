package com.g7nightalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.g7nightalarm.app.alarm.SnoozeController
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/** Fires the snooze re-alarm from AlarmManager, and checks it when the test clock moves. */
class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val force = intent.action == ACTION_FIRE
        GlobalScope.launch {
            try {
                if (force || intent.action == SnoozeController.ACTION_CHECK) {
                    SnoozeController.fire(context, force = force)
                }
            } catch (_: Throwable) {
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.g7nightalarm.SNOOZE_REARM"
    }
}
