package com.example.detector

import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Lightweight, depth-bounded AccessibilityNodeInfo inspector designed for minimal CPU
 * overhead. Stops traversal as soon as Instagram Reels markers are confirmed.
 */
object InstagramNodeInspector {
  private const val MAX_TRAVERSAL_DEPTH = 7
  private const val MAX_VISITED_NODES = 48

  fun inspect(
    event: AccessibilityEvent,
    rootNode: AccessibilityNodeInfo?,
    nowMs: Long = System.currentTimeMillis(),
  ): ReelViewportSnapshot {
    val pkg = event.packageName?.toString().orEmpty()
    if (pkg != ReelScrollDetector.INSTAGRAM_PACKAGE) {
      return ReelViewportSnapshot(packageName = pkg, isReelsContext = false, timestampMs = nowMs)
    }

    val deltaY =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        event.scrollDeltaY
      } else {
        0
      }

    var isReels = false
    var signatureAcc = 17

    // Fast path: check event source view ID or class before touching the window tree
    val sourceNode = event.source
    if (sourceNode != null) {
      val sourceId = sourceNode.viewIdResourceName.orEmpty().lowercase()
      if (looksLikeReelsId(sourceId)) {
        isReels = true
      }
    }

    if (rootNode != null) {
      var visited = 0
      val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
      queue.add(rootNode to 0)

      while (queue.isNotEmpty() && visited < MAX_VISITED_NODES) {
        val (node, depth) = queue.removeFirst()
        visited++

        val viewId = node.viewIdResourceName.orEmpty().lowercase()
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        val text = node.text?.toString()?.trim().orEmpty()

        if (!isReels && (looksLikeReelsId(viewId) || looksLikeReelsDesc(desc, node.isSelected))) {
          isReels = true
        }

        if (isReels && text.length in 2..40 && (viewId.contains("clips") || viewId.contains("caption") || viewId.contains("username"))) {
          signatureAcc = 31 * signatureAcc + text.hashCode()
        }

        if (depth < MAX_TRAVERSAL_DEPTH) {
          val childCount = node.childCount.coerceAtMost(12)
          for (i in 0 until childCount) {
            node.getChild(i)?.let { queue.add(it to depth + 1) }
          }
        }
      }
    }

    val scrollIndex = if (event.fromIndex >= 0) event.fromIndex else event.toIndex
    val maxIndex = event.itemCount

    return ReelViewportSnapshot(
      packageName = pkg,
      isReelsContext = isReels,
      scrollIndex = scrollIndex,
      maxIndex = maxIndex,
      scrollDeltaY = deltaY,
      contentSignature = if (signatureAcc == 17) 0 else signatureAcc,
      timestampMs = nowMs,
    )
  }

  private fun looksLikeReelsId(viewId: String): Boolean {
    if (viewId.isEmpty()) return false
    return viewId.contains("clips_") ||
      viewId.contains("reel_viewer") ||
      viewId.contains("clips_viewer") ||
      viewId.contains("reels_tray")
  }

  private fun looksLikeReelsDesc(desc: String, isSelected: Boolean): Boolean {
    if (desc.isEmpty()) return false
    if (isSelected && (desc == "reels" || desc.contains("reels tab"))) return true
    return desc.contains("reel by ") || desc.contains("original audio")
  }
}
