package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ReelGauge", appName)
  }

  @Test
  fun `navigate across overview history and settings tabs`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("permission_readiness_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("daily_gauge_card").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_history").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("weekly_chart_card").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("daily_limit_slider").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_overview").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("pause_resume_button").performScrollTo().assertIsDisplayed()
  }
}
