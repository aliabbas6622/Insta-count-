package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ReelGaugeApp

class NotificationActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val app = context.applicationContext as? ReelGaugeApp ?: return
    val coordinator = app.coordinator
    when (intent.action) {
      ACTION_PAUSE_15M -> {
        coordinator.snoozeForMinutes(15)
        app.overlayPillController.hide()
      }
      ACTION_RESUME -> {
        coordinator.resumeTracking()
      }
    }
  }

  companion object {
    const val ACTION_PAUSE_15M = "com.example.action.PAUSE_15M"
    const val ACTION_RESUME = "com.example.action.RESUME"
  }
}
