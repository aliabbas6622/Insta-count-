package com.example.service

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import com.example.data.DailyUsageEntity
import com.example.data.DateKeyHelper
import com.example.data.PreferencesRepository
import com.example.data.ThresholdLevel
import com.example.data.UsageRepository
import com.example.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrackerLiveState(
  val dateKey: String = DateKeyHelper.dateKeyForEpochMs(),
  val todayTotal: Int = 0,
  val sessionReels: Int = 0,
  val todaySessions: Int = 0,
  val isInstagramForeground: Boolean = false,
  val isServiceConnected: Boolean = false,
  val preferences: UserPreferences = UserPreferences(),
) {
  val thresholdLevel: ThresholdLevel
    get() = preferences.thresholdFor(todayTotal)

  val isSnoozed: Boolean
    get() = preferences.isSnoozed()
}

/**
 * Single source of truth for real-time reel counts, snooze state, debounced Room flushes,
 * ongoing notification updates, and gentle milestone nudges.
 */
class ReelTrackerCoordinator(
  private val appContext: Context,
  private val usageRepository: UsageRepository,
  val preferencesRepository: PreferencesRepository,
  val notificationController: NotificationController,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val mainHandler = Handler(Looper.getMainLooper())

  private val _liveState = MutableStateFlow(TrackerLiveState())
  val liveState: StateFlow<TrackerLiveState> = _liveState.asStateFlow()

  private var flushJob: Job? = null
  private var unsavedDeltaCount = 0
  private var lastLimitAlertLevel: ThresholdLevel = ThresholdLevel.SAFE

  @Volatile private var initialized = false

  fun initialize() {
    if (initialized) return
    initialized = true
    scope.launch {
      val initialPrefs = preferencesRepository.preferences.first()
      val todayKey = DateKeyHelper.dateKeyForEpochMs()
      val existing = usageRepository.getTodayRecord(todayKey)
      _liveState.update {
        it.copy(
          dateKey = todayKey,
          todayTotal = existing?.reelCount ?: 0,
          todaySessions = existing?.sessionCount ?: 0,
          preferences = initialPrefs,
        )
      }
      refreshNotification()

      preferencesRepository.preferences.collect { prefs ->
        _liveState.update { it.copy(preferences = prefs) }
        refreshNotification()
      }
    }
  }

  fun onServiceConnected() {
    _liveState.update { it.copy(isServiceConnected = true) }
    refreshNotification()
  }

  fun onServiceDisconnected() {
    _liveState.update {
      it.copy(isServiceConnected = false, isInstagramForeground = false, sessionReels = 0)
    }
    flushNow()
    refreshNotification()
  }

  fun onInstagramEntered() {
    rolloverDayIfNeeded()
    val current = _liveState.value
    if (!current.isInstagramForeground) {
      _liveState.update {
        it.copy(
          isInstagramForeground = true,
          sessionReels = 0,
          todaySessions = it.todaySessions + 1,
        )
      }
      scheduleDebouncedFlush()
    }
  }

  fun onInstagramExited() {
    val current = _liveState.value
    if (current.isInstagramForeground) {
      _liveState.update { it.copy(isInstagramForeground = false) }
      flushNow()
      refreshNotification()
    }
  }

  fun recordReelScrolled(): TrackerLiveState {
    rolloverDayIfNeeded()
    val snapshot = _liveState.value
    if (snapshot.isSnoozed) return snapshot

    val newTotal = snapshot.todayTotal + 1
    val newSession = snapshot.sessionReels + 1
    val prefs = snapshot.preferences
    val newThreshold = prefs.thresholdFor(newTotal)

    _liveState.update {
      it.copy(todayTotal = newTotal, sessionReels = newSession)
    }

    unsavedDeltaCount++
    if (unsavedDeltaCount >= 5) {
      flushNow()
    } else {
      scheduleDebouncedFlush()
    }

    refreshNotification()
    maybeTriggerMindfulNudge(newTotal, prefs, newThreshold)
    return _liveState.value
  }

  fun snoozeForMinutes(minutes: Int) {
    val until = System.currentTimeMillis() + minutes * 60_000L
    scope.launch {
      preferencesRepository.setSnoozeUntil(until)
    }
    _liveState.update {
      it.copy(preferences = it.preferences.copy(snoozeUntilEpochMs = until))
    }
    refreshNotification()
  }

  fun resumeTracking() {
    scope.launch {
      preferencesRepository.setSnoozeUntil(0L)
    }
    _liveState.update {
      it.copy(preferences = it.preferences.copy(snoozeUntilEpochMs = 0L))
    }
    refreshNotification()
  }

  fun resetTodayCount() {
    val todayKey = DateKeyHelper.dateKeyForEpochMs()
    lastLimitAlertLevel = ThresholdLevel.SAFE
    _liveState.update {
      it.copy(dateKey = todayKey, todayTotal = 0, sessionReels = 0, todaySessions = 0)
    }
    flushNow()
    refreshNotification()
  }

  fun refreshNotification() {
    val state = _liveState.value
    notificationController.updateOngoingNotification(
      enabled = state.preferences.persistentNotificationEnabled,
      todayTotal = state.todayTotal,
      sessionReels = state.sessionReels,
      dailyLimit = state.preferences.dailyLimit,
      snoozeUntilEpochMs = state.preferences.snoozeUntilEpochMs,
      serviceConnected = state.isServiceConnected,
    )
  }

  private fun rolloverDayIfNeeded() {
    val nowKey = DateKeyHelper.dateKeyForEpochMs()
    val current = _liveState.value
    if (current.dateKey != nowKey) {
      // Flush yesterday first
      scope.launch { persistState(current) }
      lastLimitAlertLevel = ThresholdLevel.SAFE
      _liveState.update {
        it.copy(
          dateKey = nowKey,
          todayTotal = 0,
          sessionReels = 0,
          todaySessions = 0,
        )
      }
      refreshNotification()
    }
  }

  private fun scheduleDebouncedFlush() {
    flushJob?.cancel()
    flushJob =
      scope.launch {
        delay(3000L)
        persistState(_liveState.value)
        unsavedDeltaCount = 0
      }
  }

  fun flushNow() {
    flushJob?.cancel()
    val state = _liveState.value
    unsavedDeltaCount = 0
    scope.launch { persistState(state) }
  }

  private suspend fun persistState(state: TrackerLiveState) {
    usageRepository.saveDailyRecord(
      DailyUsageEntity(
        dateKey = state.dateKey,
        reelCount = state.todayTotal,
        sessionCount = state.todaySessions,
        dailyLimitSnapshot = state.preferences.dailyLimit,
        lastUpdatedEpochMs = System.currentTimeMillis(),
      )
    )
  }

  private fun maybeTriggerMindfulNudge(
    newTotal: Int,
    prefs: UserPreferences,
    newThreshold: ThresholdLevel,
  ) {
    val crossedThreshold =
      newThreshold != lastLimitAlertLevel &&
        (newThreshold == ThresholdLevel.WARNING || newThreshold == ThresholdLevel.OVER_LIMIT)
    if (crossedThreshold) {
      lastLimitAlertLevel = newThreshold
    }

    val hitMilestone = prefs.milestoneInterval > 0 && newTotal % prefs.milestoneInterval == 0
    if (!hitMilestone && !crossedThreshold) return

    if (prefs.hapticNudgeEnabled) {
      triggerGentleHaptic()
    }
    if (prefs.toastNudgeEnabled) {
      val msg =
        when {
          crossedThreshold && newThreshold == ThresholdLevel.OVER_LIMIT ->
            "Daily limit reached (${prefs.dailyLimit} reels). Take a breather?"
          crossedThreshold && newThreshold == ThresholdLevel.WARNING ->
            "Approaching daily limit ($newTotal / ${prefs.dailyLimit} reels)"
          else -> "$newTotal reels scrolled today"
        }
      mainHandler.post {
        Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun triggerGentleHaptic() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator.vibrate(
          VibrationEffect.createOneShot(28L, VibrationEffect.DEFAULT_AMPLITUDE)
        )
      } else {
        @Suppress("DEPRECATION")
        val vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(VibrationEffect.createOneShot(28L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
          @Suppress("DEPRECATION") vibrator.vibrate(28L)
        }
      }
    } catch (_: Exception) {
      // Ignore if vibrator hardware is unavailable
    }
  }
}
