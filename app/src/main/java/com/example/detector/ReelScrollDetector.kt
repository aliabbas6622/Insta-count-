package com.example.detector

import kotlin.math.abs

data class ReelViewportSnapshot(
  val packageName: String,
  val isReelsContext: Boolean,
  val scrollIndex: Int = -1,
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
  private val cooldownMs: Long = 1200L,
  private val minScrollDeltaPx: Int = 180,
) {
  private var lastCountedTimestampMs: Long = 0L
  private var lastScrollIndex: Int = -1
  private var lastContentSignature: Int = 0
  private var hasInitializedContext: Boolean = false

  fun resetSession() {
    lastScrollIndex = -1
    lastContentSignature = 0
    hasInitializedContext = false
  }

  /**
   * Evaluates a viewport snapshot and returns true only when a distinct new reel
   * transition occurred outside the cooldown window.
   */
  fun evaluate(snapshot: ReelViewportSnapshot): Boolean {
    if (snapshot.packageName != INSTAGRAM_PACKAGE || !snapshot.isReelsContext) {
      return false
    }

    // Seed baseline on first entry into Reels so opening the Reels tab starts at 0
    // (or counts the first deliberate swipe rather than initial layout inflation).
    if (!hasInitializedContext) {
      hasInitializedContext = true
      lastScrollIndex = snapshot.scrollIndex
      lastContentSignature = snapshot.contentSignature
      lastCountedTimestampMs = snapshot.timestampMs
      return false
    }

    val elapsed = snapshot.timestampMs - lastCountedTimestampMs
    if (elapsed < cooldownMs) {
      return false
    }

    val indexChanged =
      snapshot.scrollIndex >= 0 &&
        lastScrollIndex >= 0 &&
        snapshot.scrollIndex != lastScrollIndex

    val signatureChanged =
      snapshot.contentSignature != 0 &&
        lastContentSignature != 0 &&
        snapshot.contentSignature != lastContentSignature

    val largeVerticalScroll = abs(snapshot.scrollDeltaY) >= minScrollDeltaPx

    if (indexChanged || signatureChanged || largeVerticalScroll) {
      lastCountedTimestampMs = snapshot.timestampMs
      if (snapshot.scrollIndex >= 0) {
        lastScrollIndex = snapshot.scrollIndex
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
