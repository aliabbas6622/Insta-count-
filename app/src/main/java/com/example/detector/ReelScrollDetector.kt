package com.example.detector

import kotlin.math.abs

data class ReelViewportSnapshot(
  val packageName: String,
  val isReelsContext: Boolean,
  val isExplicitNonReelsContext: Boolean = false,
  val isScrollEvent: Boolean = false,
  val scrollIndex: Int = -1,
  val scrollY: Int = -1,
  val maxIndex: Int = -1,
  val scrollDeltaY: Int = 0,
  val contentSignature: Int = 0,
  val timestampMs: Long = System.currentTimeMillis(),
)

/**
 * Pure Kotlin, debounced multi-signal detector that prevents double counting from
 * overscroll bounces, comment sheets, or layout reflows inside Instagram Reels.
 */
class ReelScrollDetector(
  private val cooldownMs: Long = 1100L,
  private val minScrollDeltaPx: Int = 140,
) {
  private var lastCountedTimestampMs: Long = 0L
  private var lastScrollIndex: Int = -1
  private var lastScrollY: Int = -1
  private var lastContentSignature: Int = 0
  private var hasInitializedContext: Boolean = false

  fun resetSession() {
    lastScrollIndex = -1
    lastScrollY = -1
    lastContentSignature = 0
    hasInitializedContext = false
  }

  /**
   * Evaluates a viewport snapshot and returns true only when a distinct new reel
   * transition occurred outside the cooldown window.
   */
  fun evaluate(snapshot: ReelViewportSnapshot): Boolean {
    if (snapshot.packageName != INSTAGRAM_PACKAGE) {
      return false
    }

    if (snapshot.isExplicitNonReelsContext) {
      resetSession()
      return false
    }

    if (!snapshot.isReelsContext) {
      return false
    }

    // Seed baseline on first entry into Reels so opening the Reels tab starts at 0.
    if (!hasInitializedContext) {
      hasInitializedContext = true
      lastScrollIndex = snapshot.scrollIndex
      lastScrollY = snapshot.scrollY
      lastContentSignature = snapshot.contentSignature
      lastCountedTimestampMs = snapshot.timestampMs

      // If the very first observed event is already an explicit vertical swipe, count it.
      val initialSwipe =
        snapshot.isScrollEvent &&
          (abs(snapshot.scrollDeltaY) >= minScrollDeltaPx || snapshot.scrollIndex > 0)
      return initialSwipe
    }

    val elapsed = snapshot.timestampMs - lastCountedTimestampMs
    if (elapsed < cooldownMs) {
      // Keep baseline tracking fresh if a scroll settled slightly later inside cooldown
      if (snapshot.scrollIndex >= 0) lastScrollIndex = snapshot.scrollIndex
      if (snapshot.scrollY >= 0) lastScrollY = snapshot.scrollY
      if (snapshot.contentSignature != 0) lastContentSignature = snapshot.contentSignature
      return false
    }

    val indexChanged =
      snapshot.scrollIndex >= 0 &&
        ((lastScrollIndex >= 0 && snapshot.scrollIndex != lastScrollIndex) ||
          (lastScrollIndex < 0 && snapshot.isScrollEvent && snapshot.scrollIndex > 0))

    val scrollYChanged =
      snapshot.scrollY >= 0 &&
        lastScrollY >= 0 &&
        abs(snapshot.scrollY - lastScrollY) >= minScrollDeltaPx

    val signatureChanged =
      snapshot.contentSignature != 0 &&
        lastContentSignature != 0 &&
        snapshot.contentSignature != lastContentSignature

    val largeVerticalScroll = snapshot.isScrollEvent && abs(snapshot.scrollDeltaY) >= minScrollDeltaPx

    // Fallback: if Instagram fires TYPE_VIEW_SCROLLED on a vertical pager without index/deltaY/signature
    val barePagerScroll = snapshot.isScrollEvent && snapshot.maxIndex > 1

    if (indexChanged || scrollYChanged || signatureChanged || largeVerticalScroll || barePagerScroll) {
      lastCountedTimestampMs = snapshot.timestampMs
      if (snapshot.scrollIndex >= 0) {
        lastScrollIndex = snapshot.scrollIndex
      }
      if (snapshot.scrollY >= 0) {
        lastScrollY = snapshot.scrollY
      }
      if (snapshot.contentSignature != 0) {
        lastContentSignature = snapshot.contentSignature
      }
      return true
    }

    return false
  }

  companion object {
    const val INSTAGRAM_PACKAGE = "com.instagram.android"
  }
}
