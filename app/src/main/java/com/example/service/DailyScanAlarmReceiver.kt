package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyScanAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_DAILY_SCAN = "com.example.action.TRIGGER_DAILY_SCAN"
        private const val TAG = "DailyScanAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast action: $action")

        when (action) {
            ACTION_TRIGGER_DAILY_SCAN -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val settings = DailyScanScheduler.loadSettings(context)
                        if (settings.isEnabled) {
                            DailyScanScheduler.executeScheduledScan(context)
                        } else {
                            DailyScanScheduler.cancelAlarm(context)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during scheduled scan run", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val settings = DailyScanScheduler.loadSettings(context)
                if (settings.isEnabled) {
                    DailyScanScheduler.scheduleAlarm(context, settings)
                }
            }
        }
    }
}
