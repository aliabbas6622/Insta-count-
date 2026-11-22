package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.reelGaugeDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "reelgauge_prefs")

class PreferencesRepository(private val context: Context) {
  private object Keys {
    val DAILY_LIMIT = intPreferencesKey("daily_limit")
    val MILESTONE_INTERVAL = intPreferencesKey("milestone_interval")
    val HAPTIC_NUDGE = booleanPreferencesKey("haptic_nudge")
    val TOAST_NUDGE = booleanPreferencesKey("toast_nudge")
    val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
    val PERSISTENT_NOTIFICATION = booleanPreferencesKey("persistent_notification")
    val SNOOZE_UNTIL = longPreferencesKey("snooze_until")
  }

  val preferences: Flow<UserPreferences> =
    context.reelGaugeDataStore.data.map { prefs ->
      UserPreferences(
        dailyLimit = prefs[Keys.DAILY_LIMIT] ?: 30,
        milestoneInterval = prefs[Keys.MILESTONE_INTERVAL] ?: 10,
        hapticNudgeEnabled = prefs[Keys.HAPTIC_NUDGE] ?: true,
        toastNudgeEnabled = prefs[Keys.TOAST_NUDGE] ?: true,
        overlayEnabled = prefs[Keys.OVERLAY_ENABLED] ?: true,
        persistentNotificationEnabled = prefs[Keys.PERSISTENT_NOTIFICATION] ?: true,
        snoozeUntilEpochMs = prefs[Keys.SNOOZE_UNTIL] ?: 0L,
      )
    }

  suspend fun setDailyLimit(limit: Int) {
    context.reelGaugeDataStore.edit { it[Keys.DAILY_LIMIT] = limit.coerceIn(0, 200) }
  }

  suspend fun setMilestoneInterval(interval: Int) {
    context.reelGaugeDataStore.edit { it[Keys.MILESTONE_INTERVAL] = interval.coerceIn(0, 50) }
  }

  suspend fun setHapticNudgeEnabled(enabled: Boolean) {
    context.reelGaugeDataStore.edit { it[Keys.HAPTIC_NUDGE] = enabled }
  }

  suspend fun setToastNudgeEnabled(enabled: Boolean) {
    context.reelGaugeDataStore.edit { it[Keys.TOAST_NUDGE] = enabled }
  }

  suspend fun setOverlayEnabled(enabled: Boolean) {
    context.reelGaugeDataStore.edit { it[Keys.OVERLAY_ENABLED] = enabled }
  }

  suspend fun setPersistentNotificationEnabled(enabled: Boolean) {
    context.reelGaugeDataStore.edit { it[Keys.PERSISTENT_NOTIFICATION] = enabled }
  }

  suspend fun setSnoozeUntil(epochMs: Long) {
    context.reelGaugeDataStore.edit { it[Keys.SNOOZE_UNTIL] = epochMs }
  }
}
