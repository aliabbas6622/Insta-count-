package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ThresholdLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationController(private val context: Context) {
  private val notificationManager = NotificationManagerCompat.from(context)

  init {
    ensureChannel()
  }

  fun ensureChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
          )
          .apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
          }
      val sysManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      sysManager.createNotificationChannel(channel)
    }
  }

  fun hasNotificationPermission(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
    }
    return notificationManager.areNotificationsEnabled()
  }

  fun updateOngoingNotification(
    enabled: Boolean,
    todayTotal: Int,
    sessionReels: Int,
    dailyLimit: Int,
    snoozeUntilEpochMs: Long,
    serviceConnected: Boolean,
  ) {
    if (!enabled || !hasNotificationPermission()) {
      cancelNotification()
      return
    }
    ensureChannel()

    val now = System.currentTimeMillis()
    val isSnoozed = snoozeUntilEpochMs > now

    val title =
      if (isSnoozed) {
        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        "Tracking paused until ${fmt.format(Date(snoozeUntilEpochMs))}"
      } else if (dailyLimit > 0) {
        "Today: $todayTotal / $dailyLimit reels"
      } else {
        "Today: $todayTotal reels scrolled"
      }

    val statusSuffix =
      when {
        isSnoozed -> "Today: $todayTotal reels • Tap Resume anytime"
        !serviceConnected -> "Detector off • Open ReelGauge to enable"
        else -> "Session: $sessionReels reels • Battery-optimized"
      }

    val openAppIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val openPending =
      PendingIntent.getActivity(
        context,
        REQ_OPEN_APP,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val actionIntent =
      Intent(context, NotificationActionReceiver::class.java).apply {
        action =
          if (isSnoozed) NotificationActionReceiver.ACTION_RESUME
          else NotificationActionReceiver.ACTION_PAUSE_15M
      }
    val actionPending =
      PendingIntent.getBroadcast(
        context,
        REQ_TOGGLE_SNOOZE,
        actionIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val actionLabel =
      if (isSnoozed) context.getString(R.string.action_resume)
      else context.getString(R.string.action_pause_15m)

    val threshold =
      if (dailyLimit <= 0) ThresholdLevel.SAFE
      else {
        val ratio = todayTotal.toFloat() / dailyLimit.toFloat()
        when {
          ratio >= 1.0f -> ThresholdLevel.OVER_LIMIT
          ratio >= 0.75f -> ThresholdLevel.WARNING
          else -> ThresholdLevel.SAFE
        }
      }

    val accentColor =
      when (threshold) {
        ThresholdLevel.SAFE -> 0xFF3D6B5B.toInt()
        ThresholdLevel.WARNING -> 0xFFC88A36.toInt()
        ThresholdLevel.OVER_LIMIT -> 0xFFC45B4B.toInt()
      }

    val builder =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_media_play)
        .setColor(accentColor)
        .setContentTitle(title)
        .setContentText(statusSuffix)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setContentIntent(openPending)
        .addAction(0, actionLabel, actionPending)

    if (dailyLimit > 0) {
      builder.setProgress(dailyLimit, todayTotal.coerceAtMost(dailyLimit), false)
    }

    try {
      notificationManager.notify(NOTIFICATION_ID, builder.build())
    } catch (_: SecurityException) {
      // Guarded if notification permission was revoked concurrently
    }
  }

  fun cancelNotification() {
    notificationManager.cancel(NOTIFICATION_ID)
  }

  companion object {
    const val CHANNEL_ID = "reel_usage_channel"
    const val NOTIFICATION_ID = 4102
    private const val REQ_OPEN_APP = 101
    private const val REQ_TOGGLE_SNOOZE = 102
  }
}
