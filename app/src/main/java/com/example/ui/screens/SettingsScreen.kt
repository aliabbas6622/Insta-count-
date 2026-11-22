package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ThresholdLevel
import com.example.ui.components.FloatingPillPreview
import com.example.ui.viewmodel.ReelGaugeUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  state: ReelGaugeUiState,
  onDailyLimitChange: (Int) -> Unit,
  onMilestoneChange: (Int) -> Unit,
  onToggleHaptic: (Boolean) -> Unit,
  onToggleToast: (Boolean) -> Unit,
  onToggleOverlay: (Boolean) -> Unit,
  onToggleNotification: (Boolean) -> Unit,
  onResetToday: () -> Unit,
  onClearHistory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val prefs = state.live.preferences
  var previewThreshold by remember { mutableStateOf(ThresholdLevel.SAFE) }
  var showClearDialog by remember { mutableStateOf(false) }

  val previewCount =
    when (previewThreshold) {
      ThresholdLevel.SAFE -> 8
      ThresholdLevel.WARNING -> 24
      ThresholdLevel.OVER_LIMIT -> 34
    }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column {
        Text(
          text = "Mindful Controls",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "Customize daily limits, nudges, and overlay appearance",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Live Floating Pill Preview Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Floating Pill Color-Shift Preview",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          FloatingPillPreview(
            sessionCount = previewCount,
            todayTotal = previewCount,
            dailyLimit = 30,
            thresholdLevel = previewThreshold,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ThresholdLevel.entries.forEach { level ->
              val label =
                when (level) {
                  ThresholdLevel.SAFE -> "Safe"
                  ThresholdLevel.WARNING -> "75%+"
                  ThresholdLevel.OVER_LIMIT -> "Over"
                }
              FilterChip(
                selected = previewThreshold == level,
                onClick = { previewThreshold = level },
                label = { Text(label) },
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
              )
            }
          }
        }
      }

      // Daily Limit & Milestones
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Daily Reel Limit",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = if (prefs.dailyLimit == 0) "No limit" else "${prefs.dailyLimit} reels",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          }

          Slider(
            value = prefs.dailyLimit.toFloat(),
            onValueChange = { raw ->
              val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(0, 150)
              onDailyLimitChange(snapped)
            },
            valueRange = 0f..150f,
            steps = 29,
            modifier = Modifier.fillMaxWidth().testTag("daily_limit_slider"),
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

          Text(
            text = "Milestone Nudge Interval",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            listOf(0, 5, 10, 15, 25).forEach { step ->
              FilterChip(
                selected = prefs.milestoneInterval == step,
                onClick = { onMilestoneChange(step) },
                label = { Text(if (step == 0) "Off" else "Every $step") },
                modifier = Modifier.minimumInteractiveComponentSize(),
              )
            }
          }

          SettingToggleRow(
            title = "Gentle Haptic Nudge",
            subtitle = "Short tactile pulse at milestones & limit warnings",
            checked = prefs.hapticNudgeEnabled,
            testTag = "haptic_nudge_switch",
            onCheckedChange = onToggleHaptic,
          )
          SettingToggleRow(
            title = "Toast Reminder Nudge",
            subtitle = "Brief non-blocking message at milestones",
            checked = prefs.toastNudgeEnabled,
            testTag = "toast_nudge_switch",
            onCheckedChange = onToggleToast,
          )
        }
      }

      // Overlay & Notification Toggles
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Display & Notifications",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          SettingToggleRow(
            title = "Top-Center Floating Pill",
            subtitle = "Show compact counter while Instagram is open",
            checked = prefs.overlayEnabled,
            testTag = "overlay_enabled_switch",
            onCheckedChange = onToggleOverlay,
          )
          SettingToggleRow(
            title = "Persistent Daily Notification",
            subtitle = "Silent ongoing bar with total usage & Pause 15m",
            checked = prefs.persistentNotificationEnabled,
            testTag = "persistent_notif_switch",
            onCheckedChange = onToggleNotification,
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            OutlinedButton(
              onClick = onResetToday,
              modifier =
                Modifier.weight(1f).minimumInteractiveComponentSize().testTag("reset_today_button"),
              shape = RoundedCornerShape(14.dp),
            ) {
              Text(stringResource(R.string.action_reset_today))
            }
            OutlinedButton(
              onClick = { showClearDialog = true },
              modifier =
                Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("clear_history_button"),
              shape = RoundedCornerShape(14.dp),
              colors =
                ButtonDefaults.outlinedButtonColors(
                  contentColor = MaterialTheme.colorScheme.error
                ),
            ) {
              Text("Clear History")
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
    }
  }

  if (showClearDialog) {
    AlertDialog(
      onDismissRequest = { showClearDialog = false },
      title = { Text("Clear weekly history?") },
      text = {
        Text("This deletes all locally saved daily reel counts and resets your under-limit streak.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            onClearHistory()
            showClearDialog = false
          }
        ) {
          Text("Clear", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun SettingToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  testTag: String,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
    )
  }
}
