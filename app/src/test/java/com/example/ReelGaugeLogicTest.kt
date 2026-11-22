package com.example

import com.example.data.DailyUsageEntity
import com.example.data.ThresholdLevel
import com.example.data.UsageRepository
import com.example.data.UserPreferences
import com.example.detector.ReelScrollDetector
import com.example.detector.ReelViewportSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelGaugeLogicTest {

  @Test
  fun `detector ignores non-instagram and respects cooldown`() {
    val detector = ReelScrollDetector(cooldownMs = 1100L)

    // Non-Instagram event ignored
    assertFalse(
      detector.evaluate(
        ReelViewportSnapshot(
          packageName = "com.android.launcher",
          isReelsContext = true,
          scrollIndex = 1,
          timestampMs = 1000L,
        )
      )
    )

    // First Reels entry seeds baseline without incrementing
    assertFalse(
      detector.evaluate(
        ReelViewportSnapshot(
          packageName = ReelScrollDetector.INSTAGRAM_PACKAGE,
          isReelsContext = true,
          isScrollEvent = false,
          scrollIndex = 0,
          contentSignature = 101,
          timestampMs = 2000L,
        )
      )
    )

    // Within cooldown (500ms later) -> ignored
    assertFalse(
      detector.evaluate(
        ReelViewportSnapshot(
          packageName = ReelScrollDetector.INSTAGRAM_PACKAGE,
          isReelsContext = true,
          isScrollEvent = true,
          scrollIndex = 1,
          contentSignature = 202,
          timestampMs = 2500L,
        )
      )
    )

    // After cooldown (1500ms later) with new scrollDeltaY / signature -> counted!
    assertTrue(
      detector.evaluate(
        ReelViewportSnapshot(
          packageName = ReelScrollDetector.INSTAGRAM_PACKAGE,
          isReelsContext = true,
          isScrollEvent = true,
          scrollIndex = 2,
          scrollDeltaY = -500,
          contentSignature = 303,
          timestampMs = 4000L,
        )
      )
    )
  }

  @Test
  fun `detector counts scrollY change and content signature change after cooldown`() {
    val detector = ReelScrollDetector(cooldownMs = 1000L, minScrollDeltaPx = 140)

    // Entry
    assertFalse(
      detector.evaluate(
        ReelViewportSnapshot(
          packageName = ReelScrollDetector.INSTAGRAM_PACKAGE,
          isReelsContext = true,
          scrollY = 0,
          contentSignature = 111,
          timestampMs = 1000L,
        )
      )
    )

    // Scroll via scrollY delta
    assertTrue(
      detector.evaluate(
        ReelViewportSnapshot(
          packageName = ReelScrollDetector.INSTAGRAM_PACKAGE,
          isReelsContext = true,
          isScrollEvent = true,
          scrollY = 1200,
          contentSignature = 111,
          timestampMs = 2200L,
        )
      )
    )

    // Content-changed event with new creator signature
    assertTrue(
      detector.evaluate(
        ReelViewportSnapshot(
          packageName = ReelScrollDetector.INSTAGRAM_PACKAGE,
          isReelsContext = true,
          isScrollEvent = false,
          scrollY = 1200,
          contentSignature = 222,
          timestampMs = 3500L,
        )
      )
    )
  }

  @Test
  fun `under-limit streak counts consecutive compliant days`() {
    val records =
      listOf(
        DailyUsageEntity("2026-11-22", reelCount = 20, dailyLimitSnapshot = 30),
        DailyUsageEntity("2026-11-21", reelCount = 28, dailyLimitSnapshot = 30),
        DailyUsageEntity("2026-11-20", reelCount = 45, dailyLimitSnapshot = 30), // Over limit
        DailyUsageEntity("2026-11-19", reelCount = 10, dailyLimitSnapshot = 30),
      )

    val streak =
      UsageRepository.computeUnderLimitStreak(
        recordsDesc = records,
        todayKey = "2026-11-22",
        currentDailyLimit = 30,
      )
    assertEquals(2, streak)
  }

  @Test
  fun `threshold shifts at 75 percent and 100 percent`() {
    val prefs = UserPreferences(dailyLimit = 40)
    assertEquals(ThresholdLevel.SAFE, prefs.thresholdFor(20))
    assertEquals(ThresholdLevel.WARNING, prefs.thresholdFor(30))
    assertEquals(ThresholdLevel.OVER_LIMIT, prefs.thresholdFor(40))
  }
}
