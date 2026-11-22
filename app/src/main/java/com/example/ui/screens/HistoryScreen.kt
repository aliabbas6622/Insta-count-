package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.DayUsageSummary
import com.example.data.ThresholdLevel
import com.example.ui.theme.LocalReelGaugeColors
import com.example.ui.viewmodel.ReelGaugeUiState
import kotlin.math.max

@Composable
fun HistoryScreen(
  state: ReelGaugeUiState,
  onSelectDate: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val weekly = state.weeklyStats
  val dailyLimit = state.live.preferences.dailyLimit
  val semantic = LocalReelGaugeColors.current
  val selectedDay =
    weekly.days.firstOrNull { it.dateKey == state.selectedHistoryDateKey }
      ?: weekly.days.lastOrNull()

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
          text = "Weekly Insights",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "7-day reel rhythm & mindful streaks",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Streak + 7-Day Average Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        SummaryStatCard(
          icon = Icons.Rounded.LocalFireDepartment,
          label = "Under-Limit Streak",
          value = "${weekly.currentUnderLimitStreak} days",
          modifier = Modifier.weight(1f).testTag("streak_stat_card"),
        )
        SummaryStatCard(
          icon = Icons.Rounded.Timeline,
          label = "7-Day Average",
          value = "${weekly.sevenDayAverage} / day",
          modifier = Modifier.weight(1f).testTag("average_stat_card"),
        )
      }

      // 7-Day Interactive Bar Chart Card
      Card(
        modifier = Modifier.fillMaxWidth().testTag("weekly_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Last 7 Days",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            if (dailyLimit > 0) {
              Text(
                text = "Limit: $dailyLimit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          if (weekly.totalWeekReels == 0) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
              )
              Text(
                text = "No reels recorded yet this week.\nOpen Instagram Reels once setup is active.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
            }
          } else {
            WeeklyBarChart(
              days = weekly.days,
              dailyLimit = dailyLimit,
              selectedDateKey = selectedDay?.dateKey,
              onSelectDate = onSelectDate,
            )
          }

          if (selectedDay != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    text = selectedDay.displayLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = "${selectedDay.sessionCount} Instagram sessions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = "${selectedDay.reelCount} reels",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = semantic.forThreshold(selectedDay.thresholdLevel),
                )
              }
            }
          }
        }
      }

      // Daily Log List
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Daily Breakdown",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          weekly.days.reversed().forEach { day ->
            val accent = semantic.forThreshold(day.thresholdLevel)
            val statusText =
              when {
                day.dailyLimitSnapshot <= 0 -> "Tracked"
                day.thresholdLevel == ThresholdLevel.OVER_LIMIT -> "Over limit"
                day.thresholdLevel == ThresholdLevel.WARNING -> "Near limit"
                else -> "Under limit"
              }
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { onSelectDate(day.dateKey) }
                  .padding(horizontal = 10.dp, vertical = 10.dp)
                  .minimumInteractiveComponentSize(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = day.displayLabel,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "${day.sessionCount} sessions",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(50),
                  color = accent.copy(alpha = 0.14f),
                ) {
                  Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
                Text(
                  text = day.reelCount.toString(),
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WeeklyBarChart(
  days: List<DayUsageSummary>,
  dailyLimit: Int,
  selectedDateKey: String?,
  onSelectDate: (String) -> Unit,
) {
  val semantic = LocalReelGaugeColors.current
  val maxData = days.maxOfOrNull { it.reelCount } ?: 0
  val chartMax = max(max(maxData, dailyLimit), 10).toFloat()
  val outlineColor = MaterialTheme.colorScheme.outline

  Column {
    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
      // Dashed daily limit line behind bars
      if (dailyLimit > 0) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val ratio = (dailyLimit / chartMax).coerceIn(0f, 1f)
          val y = size.height * (1f - ratio)
          drawLine(
            color = outlineColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
      ) {
        days.forEach { day ->
          val barColor = semantic.forThreshold(day.thresholdLevel)
          val isSelected = day.dateKey == selectedDateKey
          val heightFraction = (day.reelCount / chartMax).coerceIn(0.04f, 1f)

          Box(
            modifier =
              Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onSelectDate(day.dateKey) }
                .testTag("history_bar_${day.dateKey}"),
            contentAlignment = Alignment.BottomCenter,
          ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
              val barHeight = size.height * heightFraction
              val topY = size.height - barHeight
              drawRoundRect(
                color = if (isSelected) barColor else barColor.copy(alpha = 0.55f),
                topLeft = Offset(0f, topY),
                size = Size(size.width, barHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      days.forEach { day ->
        val isSelected = day.dateKey == selectedDateKey
        Text(
          text = day.shortLabel,
          style =
            MaterialTheme.typography.labelMedium.copy(
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
          color =
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun SummaryStatCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
