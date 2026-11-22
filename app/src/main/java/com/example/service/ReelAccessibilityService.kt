package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.example.ReelGaugeApp
import com.example.detector.InstagramNodeInspector
import com.example.detector.ReelScrollDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Battery-optimized AccessibilityService:
 * - When Instagram is closed, listens ONLY to TYPE_WINDOW_STATE_CHANGED (0 scroll/content wakeups).
 * - When Instagram is active, enables coalesced scroll/content events (250ms timeout).
 * - Immediately removes the floating overlay on Instagram exit or SCREEN_OFF.
 */
class ReelAccessibilityService : AccessibilityService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val scrollDetector = ReelScrollDetector()

  private var isInInstagramMode = false
  private var lastOverlayBumpTimeMs = 0L

  private val screenOffReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
          exitInstagramMode()
        }
      }
    }

  private val coordinator: ReelTrackerCoordinator
    get() = (applicationContext as ReelGaugeApp).coordinator

  private val overlayController: OverlayPillController
    get() = (applicationContext as ReelGaugeApp).overlayPillController

  override fun onServiceConnected() {
    super.onServiceConnected()
    setEventScope(activeInstagram = false)
    coordinator.onServiceConnected()

    registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

    // React to live preference/snooze updates while Instagram is open
    serviceScope.launch {
      coordinator.liveState.collect { state ->
        if (state.isInstagramForeground &&
          state.preferences.overlayEnabled &&
          !state.isSnoozed
        ) {
          val now = System.currentTimeMillis()
          val shouldBump = now - lastOverlayBumpTimeMs < 350L
          overlayController.showOrUpdate(
            sessionReels = state.sessionReels,
            todayTotal = state.todayTotal,
            dailyLimit = state.preferences.dailyLimit,
            thresholdLevel = state.thresholdLevel,
            animateBump = shouldBump,
          )
        } else {
          overlayController.hide()
        }
      }
    }
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return
    val pkg = event.packageName?.toString() ?: return

    if (pkg != ReelScrollDetector.INSTAGRAM_PACKAGE) {
      // Ignore transient system UI (e.g., volume slider) so overlay doesn't flicker
      if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
        pkg != SYSTEM_UI_PACKAGE
      ) {
        exitInstagramMode()
      }
      return
    }

    if (!isInInstagramMode) {
      enterInstagramMode()
    }

    val state = coordinator.liveState.value
    if (state.isSnoozed) {
      overlayController.hide()
      return
    }

    if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
      event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    ) {
      val root = try {
        rootInActiveWindow
      } catch (_: Exception) {
        null
      }
      val snapshot = InstagramNodeInspector.inspect(event, root)
      if (scrollDetector.evaluate(snapshot)) {
        lastOverlayBumpTimeMs = System.currentTimeMillis()
        coordinator.recordReelScrolled()
      }
    }
  }

  override fun onInterrupt() {
    exitInstagramMode()
  }

  override fun onDestroy() {
    super.onDestroy()
    try {
      unregisterReceiver(screenOffReceiver)
    } catch (_: Exception) {
    }
    overlayController.hide()
    coordinator.onServiceDisconnected()
    serviceScope.cancel()
  }

  private fun enterInstagramMode() {
    isInInstagramMode = true
    scrollDetector.resetSession()
    setEventScope(activeInstagram = true)
    coordinator.onInstagramEntered()

    val state = coordinator.liveState.value
    if (state.preferences.overlayEnabled && !state.isSnoozed) {
      overlayController.showOrUpdate(
        sessionReels = state.sessionReels,
        todayTotal = state.todayTotal,
        dailyLimit = state.preferences.dailyLimit,
        thresholdLevel = state.thresholdLevel,
        animateBump = false,
      )
    }
  }

  private fun exitInstagramMode() {
    if (!isInInstagramMode) return
    isInInstagramMode = false
    scrollDetector.resetSession()
    setEventScope(activeInstagram = false)
    overlayController.hide()
    coordinator.onInstagramExited()
  }

  private fun setEventScope(activeInstagram: Boolean) {
    val info = serviceInfo ?: AccessibilityServiceInfo()
    info.eventTypes =
      if (activeInstagram) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
          AccessibilityEvent.TYPE_VIEW_SCROLLED or
          AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
      } else {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
      }
    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
    info.notificationTimeout = 250L
    info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
    serviceInfo = info
  }

  companion object {
    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
  }
}
