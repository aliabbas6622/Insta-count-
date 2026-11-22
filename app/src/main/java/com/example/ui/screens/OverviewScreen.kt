package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ThresholdLevel
import com.example.ui.components.DailyGaugeRing
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.LocalReelGaugeColors
import com.example.ui.viewmodel.ReelGaugeUiState

@Composable
fun OverviewScreen(
  state: ReelGaugeUiState,
  onRequestNotifications: () -> Unit,
  onRequestOverlay: () -> Unit,
  onRequestAccessibility: () -> Unit,
  onSnooze15m: () -> Unit,
  onResumeTracking: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalReelGaugeColors.current
  val live = state.live
  val prefs = live.preferences
  val isSnoozed = live.isSnoozed

  val statusLabel =
    when {
      isSnoozed -> stringResource(R.string.status_paused)
      !state.permissions.allGranted -> stringResource(R.string.status_setup_needed)
      else -> stringResource(R.string.status_active)
    }

  val statusDotColor =
    when {
      isSnoozed -> semantic.warning
      !state.permissions.allGranted -> semantic.warning
      else -> semantic.safe
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
      // Header Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Mindful Instagram reel awareness",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        StatusBadgePill(
          label = statusLabel,
          dotColor = statusDotColor,
          containerColor = MaterialTheme.colorScheme.surfaceVariant,
          contentColor = MaterialTheme.colorScheme.onSurface,
        )
      }

      // Permission Readiness Card (expands when missing, collapses to clean pill when ready)
      AnimatedVisibility(visible = !state.permissions.allGranted) {
        Card(
          modifier = Modifier.fillMaxWidth().testTag("permission_readiness_card"),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = stringResource(R.string.setup_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(R.string.setup_subtitle),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PermissionActionRow(
              icon = Icons.Outlined.Notifications,
              title = stringResource(R.string.perm_notifications_title),
              subtitle = stringResource(R.string.perm_notifications_desc),
              granted = state.permissions.hasNotifications,
              buttonTestTag = "perm_notifications_btn",
              onClick = onRequestNotifications,
            )
            PermissionActionRow(
              icon = Icons.Outlined.Layers,
              title = stringResource(R.string.perm_overlay_title),
              subtitle = stringResource(R.string.perm_overlay_desc),
              granted = state.permissions.hasOverlay,
              buttonTestTag = "perm_overlay_btn",
              onClick = onRequestOverlay,
            )
            PermissionActionRow(
              icon = Icons.Outlined.AccessibilityNew,
              title = stringResource(R.string.perm_accessibility_title),
              subtitle = stringResource(R.string.perm_accessibility_desc),
              granted = state.permissions.hasAccessibility,
              buttonTestTag = "perm_accessibility_btn",
              onClick = onRequestAccessibility,
            )
          }
        }
      }

      AnimatedVisibility(visible = state.permissions.allGranted) {
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("all_systems_active_banner"),
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = "All permissions active",
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "All permissions active • Zero-polling event mode",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }

      // Hero Daily Gauge Card
      Card(
        modifier = Modifier.fillMaxWidth().testTag("daily_gauge_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(22.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          DailyGaugeRing(
            todayTotal = live.todayTotal,
            dailyLimit = prefs.dailyLimit,
            thresholdLevel = live.thresholdLevel,
            isSnoozed = isSnoozed,
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Threshold feedback message
          val thresholdMessage =
            when {
              isSnoozed -> "Counting & floating pill are paused"
              live.thresholdLevel == ThresholdLevel.OVER_LIMIT ->
                "Daily limit reached — mindful pause recommended"
              live.thresholdLevel == ThresholdLevel.WARNING ->
                "Approaching your daily reel threshold"
              else -> "Within your mindful daily pace"
            }
          Text(
            text = thresholdMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Spacer(modifier = Modifier.height(18.dp))

          // Session + Streak quick metrics row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            MetricMiniTile(
              icon = Icons.Rounded.Timer,
              label = "Last / Active Session",
              value = "${live.sessionReels} reels",
              modifier = Modifier.weight(1f),
            )
            MetricMiniTile(
              icon = Icons.Rounded.LocalFireDepartment,
              label = "Under-Limit Streak",
              value = "${state.weeklyStats.currentUnderLimitStreak} days",
              modifier = Modifier.weight(1f),
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Pause 15m / Resume primary action
          Button(
            onClick = if (isSnoozed) onResumeTracking else onSnooze15m,
            modifier =
              Modifier
                .fillMaxWidth()
                .height(50.dp)
                .minimumInteractiveComponentSize()
                .testTag("pause_resume_button"),
            shape = RoundedCornerShape(16.dp),
            colors =
              if (isSnoozed) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
              } else {
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.secondaryContainer,
                  contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
              },
          ) {
            Icon(
              imageVector = if (isSnoozed) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
              contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text =
                if (isSnoozed) stringResource(R.string.action_resume)
                else "Pause Tracking for 15 min",
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }

      // Battery & Resource Efficiency Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
          ),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.BatterySaver,
              contentDescription = "Battery efficiency",
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Battery-First Architecture",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          Text(
            text =
              "• Zero polling timers — scroll events are ignored until Instagram opens.\n" +
                "• Lightweight native Canvas overlay detaches immediately on app exit.\n" +
                "• Local storage flushes in batches on idle or every 5 reels.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun PermissionActionRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  granted: Boolean,
  buttonTestTag: String,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint =
          if (granted) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Column(modifier = Modifier.weight(1f)) {
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
      if (granted) {
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = stringResource(R.string.action_granted),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      } else {
        FilledTonalButton(
          onClick = onClick,
          modifier = Modifier.minimumInteractiveComponentSize().testTag(buttonTestTag),
          shape = RoundedCornerShape(12.dp),
        ) {
          Text(text = stringResource(R.string.action_enable))
        }
      }
    }
  }
}

@Composable
private fun MetricMiniTile(
  icon: ImageVector,
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = label,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
