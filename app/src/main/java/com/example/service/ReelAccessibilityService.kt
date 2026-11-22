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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Battery-optimized AccessibilityService:
 * - When Instagram is closed, listens ONLY to TYPE_WINDOW_STATE_CHANGED (0 scroll/content wakeups).
 * - When Instagram is active, enables coalesced scroll/content events (200ms timeout).
 * - Debounces overlay hiding across transient system/IME windows so the pill never blinks.
 */
class ReelAccessibilityService : AccessibilityService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val scrollDetector = ReelScrollDetector()

  private var isInInstagramMode = false
  private var lastOverlayBumpTimeMs = 0L
  private var lastReelsConfirmedMs = 0L
  private var lastContentInspectionMs = 0L
  private var lastReelCountedMs = 0L
  private var pendingExitJob: Job? = null

  private val screenOffReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
          pendingExitJob?.cancel()
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

    serviceScope.launch {
      coordinator.liveState.collect { state ->
        if (state.isInstagramForeground &&
          state.preferences.overlayEnabled &&
          !state.isSnoozed
        ) {
          val now = System.currentTimeMillis()
          val shouldBump = lastOverlayBumpTimeMs != 0L && (now - lastOverlayBumpTimeMs < 500L)
          lastOverlayBumpTimeMs = 0L
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
      if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
        !isTransientSystemPackage(pkg)
      ) {
        // Confirm Instagram is genuinely no longer the active root window before scheduling hide
        val activeRootPkg =
          try {
            rootInActiveWindow?.packageName?.toString()
          } catch (_: Exception) {
            null
          }
        if (activeRootPkg != ReelScrollDetector.INSTAGRAM_PACKAGE) {
          scheduleExitInstagramMode()
        }
      }
      return
    }

    // Cancel any pending exit immediately when Instagram events arrive
    pendingExitJob?.cancel()
    pendingExitJob = null

    if (!isInInstagramMode) {
      enterInstagramMode()
    }

    val state = coordinator.liveState.value
    if (state.isSnoozed) {
      overlayController.hide()
      return
    }

    val now = System.currentTimeMillis()
    val eventType = event.eventType

    // Skip expensive window tree inspection during post-reel cooldown on content-changed ticks
    if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
      if (now - lastReelCountedMs < 1050L || now - lastContentInspectionMs < 350L) {
        return
      }
      lastContentInspectionMs = now
    }

    if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
      eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
      eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
    ) {
      val root =
        try {
          rootInActiveWindow
        } catch (_: Exception) {
          null
        }
      val recentlyInReels = now - lastReelsConfirmedMs < 3000L
      val snapshot = InstagramNodeInspector.inspect(event, root, recentlyInReels, now)

      if (snapshot.isExplicitNonReelsContext) {
        lastReelsConfirmedMs = 0L
      } else if (snapshot.isReelsContext) {
        lastReelsConfirmedMs = now
      }

      if (scrollDetector.evaluate(snapshot)) {
        lastReelCountedMs = now
        lastOverlayBumpTimeMs = now
        coordinator.recordReelScrolled()
      }
    }
  }

  override fun onInterrupt() {
    pendingExitJob?.cancel()
    exitInstagramMode()
  }

  override fun onDestroy() {
    super.onDestroy()
    pendingExitJob?.cancel()
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
    lastReelsConfirmedMs = 0L
    scrollDetector.resetSession()
    setEventScope(activeInstagram = true)
    coordinator.onInstagramEntered()
  }

  private fun scheduleExitInstagramMode() {
    if (!isInInstagramMode || pendingExitJob?.isActive == true) return
    pendingExitJob =
      serviceScope.launch {
        delay(500L)
        val activeRootPkg =
          try {
            rootInActiveWindow?.packageName?.toString()
          } catch (_: Exception) {
            null
          }
        if (activeRootPkg != ReelScrollDetector.INSTAGRAM_PACKAGE) {
          exitInstagramMode()
        }
      }
  }

  private fun exitInstagramMode() {
    if (!isInInstagramMode) return
    isInInstagramMode = false
    lastReelsConfirmedMs = 0L
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
    info.notificationTimeout = 200L
    info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
    serviceInfo = info
  }

  private fun isTransientSystemPackage(pkg: String): Boolean {
    return pkg == SYSTEM_UI_PACKAGE ||
      pkg == ANDROID_OS_PACKAGE ||
      pkg == applicationContext.packageName ||
      pkg.contains("inputmethod") ||
      pkg.contains("keyboard") ||
      pkg.contains("gboard")
  }

  companion object {
    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    private const val ANDROID_OS_PACKAGE = "android"
  }
}
