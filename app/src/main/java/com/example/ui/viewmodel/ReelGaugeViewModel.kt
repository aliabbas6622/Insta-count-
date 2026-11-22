package com.example.ui.viewmodel

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ReelGaugeApp
import com.example.data.WeeklyUsageStats
import com.example.service.ReelAccessibilityService
import com.example.service.TrackerLiveState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionReadiness(
  val hasNotifications: Boolean = false,
  val hasOverlay: Boolean = false,
  val hasAccessibility: Boolean = false,
) {
  val allGranted: Boolean
    get() = hasNotifications && hasOverlay && hasAccessibility
}

data class ReelGaugeUiState(
  val live: TrackerLiveState = TrackerLiveState(),
  val weeklyStats: WeeklyUsageStats =
    WeeklyUsageStats(
      days = emptyList(),
      sevenDayAverage = 0,
      currentUnderLimitStreak = 0,
      totalWeekReels = 0,
    ),
  val permissions: PermissionReadiness = PermissionReadiness(),
  val selectedHistoryDateKey: String? = null,
)

class ReelGaugeViewModel(application: Application) : AndroidViewModel(application) {
  private val app = application as ReelGaugeApp
  private val coordinator = app.coordinator
  private val prefsRepo = app.preferencesRepository
  private val usageRepo = app.usageRepository

  private val permissionsFlow = MutableStateFlow(readPermissionStatus(application))
  private val selectedDateFlow = MutableStateFlow<String?>(null)

  val uiState: StateFlow<ReelGaugeUiState> =
    combine(
        coordinator.liveState,
        usageRepo.observeWeeklyStats(),
        permissionsFlow,
        selectedDateFlow,
      ) { live, weekly, perms, selectedDate ->
        val effectiveSelected =
          selectedDate?.takeIf { key -> weekly.days.any { it.dateKey == key } }
            ?: weekly.days.lastOrNull()?.dateKey
        ReelGaugeUiState(
          live = live,
          weeklyStats = weekly,
          permissions = perms,
          selectedHistoryDateKey = effectiveSelected,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReelGaugeUiState(permissions = permissionsFlow.value),
      )

  fun refreshPermissions() {
    permissionsFlow.value = readPermissionStatus(app)
    coordinator.refreshNotification()
  }

  fun snooze15Minutes() {
    coordinator.snoozeForMinutes(15)
  }

  fun resumeTracking() {
    coordinator.resumeTracking()
  }

  fun resetTodayCount() {
    coordinator.resetTodayCount()
  }

  fun setDailyLimit(limit: Int) {
    viewModelScope.launch { prefsRepo.setDailyLimit(limit) }
  }

  fun setMilestoneInterval(interval: Int) {
    viewModelScope.launch { prefsRepo.setMilestoneInterval(interval) }
  }

  fun setHapticNudge(enabled: Boolean) {
    viewModelScope.launch { prefsRepo.setHapticNudgeEnabled(enabled) }
  }

  fun setToastNudge(enabled: Boolean) {
    viewModelScope.launch { prefsRepo.setToastNudgeEnabled(enabled) }
  }

  fun setOverlayEnabled(enabled: Boolean) {
    viewModelScope.launch { prefsRepo.setOverlayEnabled(enabled) }
  }

  fun setPersistentNotificationEnabled(enabled: Boolean) {
    viewModelScope.launch { prefsRepo.setPersistentNotificationEnabled(enabled) }
  }

  fun selectHistoryDate(dateKey: String) {
    selectedDateFlow.value = dateKey
  }

  fun clearAllHistory() {
    viewModelScope.launch {
      usageRepo.clearAllHistory()
      coordinator.resetTodayCount()
    }
  }

  private fun readPermissionStatus(context: Context): PermissionReadiness {
    val hasNotif = app.notificationController.hasNotificationPermission()
    val hasOverlay = Settings.canDrawOverlays(context)
    val hasAccessibility = isAccessibilityServiceEnabled(context)
    return PermissionReadiness(
      hasNotifications = hasNotif,
      hasOverlay = hasOverlay,
      hasAccessibility = hasAccessibility,
    )
  }

  private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    val expectedComponent = ComponentName(context, ReelAccessibilityService::class.java)
    val enabledList =
      am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        ?: emptyList()
    if (enabledList.any { it.resolveInfo?.serviceInfo?.name == expectedComponent.className }) {
      return true
    }
    val enabledSetting =
      Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
      ) ?: return false
    return enabledSetting.contains(expectedComponent.flattenToString()) ||
      enabledSetting.contains(expectedComponent.flattenToShortString())
  }
}
