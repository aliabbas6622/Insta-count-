package com.example.service

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.data.ThresholdLevel

/**
 * Ultra-lightweight native Android View overlay managed by WindowManager.
 * Uses zero Compose recomposition or idle frame ticks—invalidates only when reel counts change.
 */
class OverlayPillController(private val context: Context) {
  private val windowManager =
    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

  private var pillContainer: LinearLayout? = null
  private var ringView: CompactRingGlyphView? = null
  private var countText: TextView? = null
  private var isShowing = false

  fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

  fun showOrUpdate(
    sessionReels: Int,
    todayTotal: Int,
    dailyLimit: Int,
    thresholdLevel: ThresholdLevel,
    animateBump: Boolean = false,
  ) {
    if (!canDrawOverlays()) {
      hide()
      return
    }

    ensureViewCreated()
    val container = pillContainer ?: return

    val progress =
      if (dailyLimit <= 0) 0.35f else (todayTotal.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f)
    val accent = accentColorFor(thresholdLevel)

    ringView?.updateState(progress = progress, accentColor = accent)
    countText?.text = sessionReels.toString()

    // Tint subtle border with threshold accent
    val bg = container.background as? GradientDrawable
    bg?.setStroke(dpToPx(1.5f), adjustAlpha(accent, 0.75f))

    if (!isShowing) {
      try {
        windowManager.addView(container, buildLayoutParams())
        isShowing = true
      } catch (_: Exception) {
        isShowing = false
      }
    }

    if (animateBump && isShowing) {
      playBumpAnimation(container)
    }
  }

  fun hide() {
    val container = pillContainer ?: return
    if (isShowing) {
      try {
        windowManager.removeViewImmediate(container)
      } catch (_: Exception) {
        // Ignore if window token was already detached
      } finally {
        isShowing = false
      }
    }
  }

  private fun ensureViewCreated() {
    if (pillContainer != null) return

    val pillBg =
      GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(24f).toFloat()
        setColor(0xEB18221F.toInt()) // Translucent deep slate
        setStroke(dpToPx(1.5f), 0xCC3D6B5B.toInt())
      }

    val container =
      LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = pillBg
        setPadding(dpToPx(10f), dpToPx(6f), dpToPx(12f), dpToPx(6f))
        elevation = dpToPx(6f).toFloat()
        contentDescription = "ReelGauge session counter"
        setOnClickListener {
          val intent =
            Intent(context, MainActivity::class.java).apply {
              flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
          context.startActivity(intent)
        }
      }

    val ring =
      CompactRingGlyphView(context).apply {
        val size = dpToPx(24f)
        layoutParams =
          LinearLayout.LayoutParams(size, size).apply {
            marginEnd = dpToPx(7f)
          }
      }

    val text =
      TextView(context).apply {
        setTextColor(0xFFF4F7F5.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        text = "0"
        includeFontPadding = false
      }

    container.addView(ring)
    container.addView(text)

    pillContainer = container
    ringView = ring
    countText = text
  }

  private fun buildLayoutParams(): WindowManager.LayoutParams {
    val windowType =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      } else {
        @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
      }

    val statusBarHeight = getStatusBarHeightPx()
    return WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        windowType,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
          WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
      )
      .apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = statusBarHeight + dpToPx(6f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
      }
  }

  private fun getStatusBarHeightPx(): Int {
    val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resId > 0) context.resources.getDimensionPixelSize(resId) else dpToPx(28f)
  }

  private fun playBumpAnimation(view: View) {
    val sx = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.1f, 1f)
    val sy = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.1f, 1f)
    AnimatorSet().apply {
      playTogether(sx, sy)
      duration = 200L
      interpolator = OvershootInterpolator(1.6f)
      start()
    }
  }

  private fun accentColorFor(level: ThresholdLevel): Int =
    when (level) {
      ThresholdLevel.SAFE -> 0xFF6BC2A4.toInt()
      ThresholdLevel.WARNING -> 0xFFE3A043.toInt()
      ThresholdLevel.OVER_LIMIT -> 0xFFE06C5C.toInt()
    }

  private fun adjustAlpha(color: Int, factor: Float): Int {
    val alpha = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
    return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
  }

  private fun dpToPx(dp: Float): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        context.resources.displayMetrics,
      )
      .toInt()
}

private class CompactRingGlyphView(context: Context) : View(context) {
  private var progress = 0f
  private var accentColor = 0xFF6BC2A4.toInt()

  private val trackPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      color = 0x40FFFFFF
    }

  private val arcPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      color = accentColor
    }

  private val playPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = 0xFFF4F7F5.toInt()
    }

  private val arcBounds = RectF()
  private val playPath = Path()

  fun updateState(progress: Float, accentColor: Int) {
    this.progress = progress.coerceIn(0f, 1f)
    this.accentColor = accentColor
    arcPaint.color = accentColor
    invalidate()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val stroke = width * 0.14f
    trackPaint.strokeWidth = stroke
    arcPaint.strokeWidth = stroke

    val pad = stroke / 2f + 1f
    arcBounds.set(pad, pad, width - pad, height - pad)
    canvas.drawArc(arcBounds, 0f, 360f, false, trackPaint)

    val sweep = (progress * 360f).coerceAtLeast(if (progress > 0f) 18f else 0f)
    if (sweep > 0f) {
      canvas.drawArc(arcBounds, -90f, sweep, false, arcPaint)
    }

    // Minimal centered play triangle glyph
    val cx = width / 2f
    val cy = height / 2f
    val r = width * 0.19f
    playPath.reset()
    playPath.moveTo(cx - r * 0.55f, cy - r * 0.85f)
    playPath.lineTo(cx + r * 0.95f, cy)
    playPath.lineTo(cx - r * 0.55f, cy + r * 0.85f)
    playPath.close()
    canvas.drawPath(playPath, playPaint)
  }
}
