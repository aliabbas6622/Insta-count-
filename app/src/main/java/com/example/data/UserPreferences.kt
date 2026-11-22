package com.example.data

enum class ThresholdLevel {
  SAFE,
  WARNING,
  OVER_LIMIT,
}

data class UserPreferences(
  val dailyLimit: Int = 30,
  val milestoneInterval: Int = 10,
  val hapticNudgeEnabled: Boolean = true,
  val toastNudgeEnabled: Boolean = true,
  val overlayEnabled: Boolean = true,
  val persistentNotificationEnabled: Boolean = true,
  val snoozeUntilEpochMs: Long = 0L,
) {
  fun thresholdFor(reelCount: Int): ThresholdLevel {
    if (dailyLimit <= 0) return ThresholdLevel.SAFE
    val ratio = reelCount.toFloat() / dailyLimit.toFloat()
    return when {
      ratio >= 1.0f -> ThresholdLevel.OVER_LIMIT
      ratio >= 0.75f -> ThresholdLevel.WARNING
      else -> ThresholdLevel.SAFE
    }
  }

  fun isSnoozed(nowEpochMs: Long = System.currentTimeMillis()): Boolean {
    return snoozeUntilEpochMs > nowEpochMs
  }
}
