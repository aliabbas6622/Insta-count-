package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ThresholdLevel
import com.example.ui.theme.LocalReelGaugeColors

@Composable
fun DailyGaugeRing(
  todayTotal: Int,
  dailyLimit: Int,
  thresholdLevel: ThresholdLevel,
  isSnoozed: Boolean,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalReelGaugeColors.current
  val targetColor =
    if (isSnoozed) MaterialTheme.colorScheme.outline
    else semantic.forThreshold(thresholdLevel)
  val animatedAccent by
    animateColorAsState(
      targetValue = targetColor,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "gauge_color",
    )

  val rawProgress =
    if (dailyLimit <= 0) 0.25f else (todayTotal.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f)
  val animatedProgress by
    animateFloatAsState(
      targetValue = rawProgress,
      animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
      label = "gauge_progress",
    )

  val trackColor = MaterialTheme.colorScheme.surfaceVariant
  val glowColor = animatedAccent.copy(alpha = 0.12f)

  Box(modifier = modifier.size(204.dp), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val strokePx = 18.dp.toPx()
      val radius = (size.minDimension - strokePx) / 2f
      val center = Offset(size.width / 2f, size.height / 2f)

      // Soft organic atmospheric glow behind ring
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(glowColor, Color.Transparent),
            center = center,
            radius = size.minDimension * 0.55f,
          ),
        radius = size.minDimension * 0.55f,
        center = center,
      )

      val arcTopLeft = Offset(center.x - radius, center.y - radius)
      val arcSize = Size(radius * 2f, radius * 2f)

      drawArc(
        color = trackColor,
        startAngle = 140f,
        sweepAngle = 260f,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
      )

      if (animatedProgress > 0f) {
        drawArc(
          color = animatedAccent,
          startAngle = 140f,
          sweepAngle = (260f * animatedProgress).coerceAtLeast(10f),
          useCenter = false,
          topLeft = arcTopLeft,
          size = arcSize,
          style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
      }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = todayTotal.toString(),
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = if (dailyLimit > 0) "of $dailyLimit daily limit" else "reels today",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (dailyLimit > 0) {
        Spacer(modifier = Modifier.height(6.dp))
        val pct = ((todayTotal.toFloat() / dailyLimit) * 100).toInt()
        Surface(
          color = animatedAccent.copy(alpha = 0.14f),
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = "$pct%",
            style = MaterialTheme.typography.labelMedium,
            color = animatedAccent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
          )
        }
      }
    }
  }
}

@Composable
fun StatusBadgePill(
  label: String,
  dotColor: Color,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    color = containerColor,
    shape = RoundedCornerShape(50),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = contentColor,
      )
    }
  }
}

@Composable
fun FloatingPillPreview(
  sessionCount: Int,
  todayTotal: Int,
  dailyLimit: Int,
  thresholdLevel: ThresholdLevel,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalReelGaugeColors.current
  val accentColor by
    animateColorAsState(
      targetValue = semantic.forThreshold(thresholdLevel),
      label = "preview_pill_accent",
    )
  val progress =
    if (dailyLimit <= 0) 0.4f else (todayTotal.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f)

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = semantic.pillSurface.copy(alpha = 0.94f),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Simulated top-center floating pill
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = semantic.pillSurface,
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.8f)),
        shadowElevation = 6.dp,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val stroke = 3.dp.toPx()
              drawArc(
                color = Color.White.copy(alpha = 0.25f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke),
              )
              drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = (progress * 360f).coerceAtLeast(18f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
              )
            }
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = null,
              tint = semantic.pillOnSurface,
              modifier = Modifier.size(13.dp),
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = sessionCount.toString(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = semantic.pillOnSurface,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Top-center Instagram overlay preview",
        style = MaterialTheme.typography.bodyMedium,
        color = semantic.pillOnSurface.copy(alpha = 0.72f),
      )
    }
  }
}
