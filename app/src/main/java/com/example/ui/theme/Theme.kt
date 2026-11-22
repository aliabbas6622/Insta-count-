package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.ThresholdLevel

@Immutable
data class ReelGaugeSemanticColors(
  val safe: Color,
  val warning: Color,
  val overLimit: Color,
  val pillSurface: Color,
  val pillOnSurface: Color,
) {
  fun forThreshold(level: ThresholdLevel): Color =
    when (level) {
      ThresholdLevel.SAFE -> safe
      ThresholdLevel.WARNING -> warning
      ThresholdLevel.OVER_LIMIT -> overLimit
    }
}

val LocalReelGaugeColors = staticCompositionLocalOf {
  ReelGaugeSemanticColors(
    safe = ThresholdSafeLight,
    warning = ThresholdWarningLight,
    overLimit = ThresholdOverLight,
    pillSurface = Color(0xFF18221F),
    pillOnSurface = Color(0xFFF4F7F5),
  )
}

private val LightColorScheme =
  lightColorScheme(
    primary = SagePrimary,
    onPrimary = SageOnPrimary,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = SageOnPrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = SlateOnSecondary,
    secondaryContainer = SlateSecondaryContainer,
    onSecondaryContainer = SlateOnSecondaryContainer,
    tertiary = EucalyptusTertiary,
    onTertiary = EucalyptusOnTertiary,
    tertiaryContainer = EucalyptusContainer,
    onTertiaryContainer = EucalyptusOnContainer,
    background = MistBackground,
    onBackground = SlateInk,
    surface = MistSurface,
    onSurface = SlateInk,
    surfaceVariant = MistSurfaceVariant,
    onSurfaceVariant = MutedSlate,
    outline = SoftOutline,
    error = ThresholdOverLight,
    onError = Color.White,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkSagePrimary,
    onPrimary = DarkSageOnPrimary,
    primaryContainer = DarkSagePrimaryContainer,
    onPrimaryContainer = DarkSageOnPrimaryContainer,
    secondary = DarkSlateSecondary,
    onSecondary = DarkSlateOnSecondary,
    secondaryContainer = DarkSlateSecondaryContainer,
    onSecondaryContainer = DarkSlateOnSecondaryContainer,
    tertiary = EucalyptusContainer,
    onTertiary = EucalyptusOnContainer,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedSlate,
    outline = DarkOutline,
    error = ThresholdOverDark,
    onError = Color(0xFF2D0A06),
  )

@Composable
fun ReelGaugeTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val semanticColors =
    if (darkTheme) {
      ReelGaugeSemanticColors(
        safe = ThresholdSafeDark,
        warning = ThresholdWarningDark,
        overLimit = ThresholdOverDark,
        pillSurface = Color(0xFF1B2723),
        pillOnSurface = Color(0xFFF4F7F5),
      )
    } else {
      ReelGaugeSemanticColors(
        safe = ThresholdSafeLight,
        warning = ThresholdWarningLight,
        overLimit = ThresholdOverLight,
        pillSurface = Color(0xFF18221F),
        pillOnSurface = Color(0xFFF4F7F5),
      )
    }

  CompositionLocalProvider(LocalReelGaugeColors provides semanticColors) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
