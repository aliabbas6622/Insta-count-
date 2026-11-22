package com.example.detector

import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Lightweight, depth-bounded AccessibilityNodeInfo inspector designed for minimal CPU
 * overhead while handling both Resource-ID and obfuscated/Litho Instagram Reels layouts.
 */
object InstagramNodeInspector {
  private const val MAX_TRAVERSAL_DEPTH = 20
  private const val MAX_VISITED_NODES = 160

  private val CHROME_WORDS =
    setOf(
      "like",
      "unlike",
      "comment",
      "comments",
      "share",
      "send",
      "remix",
      "follow",
      "following",
      "reels",
      "reel",
      "home",
      "search",
      "explore",
      "profile",
      "more",
      "save",
      "audio",
      "original audio",
      "sponsored",
      "subscribe",
    )

  fun inspect(
    event: AccessibilityEvent,
    rootNode: AccessibilityNodeInfo?,
    recentlyInReels: Boolean = false,
    nowMs: Long = System.currentTimeMillis(),
  ): ReelViewportSnapshot {
    val pkg = event.packageName?.toString().orEmpty()
    if (pkg != ReelScrollDetector.INSTAGRAM_PACKAGE) {
      return ReelViewportSnapshot(packageName = pkg, isReelsContext = false, timestampMs = nowMs)
    }

    val isScrollEvent = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
    val eventClass = event.className?.toString().orEmpty()
    val isPagerOrListScroll =
      isScrollEvent &&
        (eventClass.contains("RecyclerView", ignoreCase = true) ||
          eventClass.contains("ViewPager", ignoreCase = true) ||
          eventClass.contains("Pager", ignoreCase = true) ||
          eventClass.contains("ScrollView", ignoreCase = true))

    val deltaY =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        event.scrollDeltaY
      } else {
        0
      }

    var isReels = false
    var isExplicitNonReels = false
    var ufiActionSignals = 0
    var hasVerticalScrollable = false
    var signatureAcc = 17
    var signatureParts = 0

    val sourceNode = event.source
    if (sourceNode != null) {
      val sourceId = sourceNode.viewIdResourceName.orEmpty().lowercase()
      if (looksLikeReelsId(sourceId)) {
        isReels = true
      }
      if (sourceNode.isScrollable && isScrollEvent) {
        hasVerticalScrollable = true
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
        val desc = node.contentDescription?.toString()?.trim()?.lowercase().orEmpty()
        val text = node.text?.toString()?.trim().orEmpty()
        val textLower = text.lowercase()
        val className = node.className?.toString().orEmpty()

        if (node.isScrollable &&
          (className.contains("ViewPager", ignoreCase = true) ||
            className.contains("RecyclerView", ignoreCase = true))
        ) {
          hasVerticalScrollable = true
        }

        if (looksLikeNonReelsTabSelected(desc, textLower, node.isSelected)) {
          isExplicitNonReels = true
        }

        if (!isReels && (looksLikeReelsId(viewId) || looksLikeReelsDesc(desc, textLower, node.isSelected))) {
          isReels = true
        }

        if (looksLikeReelsUfiButton(desc, textLower, viewId)) {
          ufiActionSignals++
        }

        if (signatureParts < 6 && isStableIdentityNode(viewId, desc, text, textLower)) {
          val token = text.ifEmpty { desc }
          signatureAcc = 31 * signatureAcc + token.hashCode()
          signatureParts++
        }

        if (depth < MAX_TRAVERSAL_DEPTH) {
          val childCount = node.childCount.coerceAtMost(18)
          for (i in 0 until childCount) {
            node.getChild(i)?.let { queue.add(it to depth + 1) }
          }
        }
      }
    }

    if (!isExplicitNonReels) {
      if (!isReels && ufiActionSignals >= 2 && (hasVerticalScrollable || isPagerOrListScroll)) {
        isReels = true
      } else if (!isReels && recentlyInReels && isPagerOrListScroll) {
        isReels = true
      }
    } else {
      isReels = false
    }

    val scrollIndex =
      when {
        event.toIndex >= 0 -> event.toIndex
        event.fromIndex >= 0 -> event.fromIndex
        else -> -1
      }
    val scrollY = event.scrollY
    val maxIndex = event.itemCount

    return ReelViewportSnapshot(
      packageName = pkg,
      isReelsContext = isReels,
      isExplicitNonReelsContext = isExplicitNonReels,
      isScrollEvent = isScrollEvent,
      scrollIndex = scrollIndex,
      scrollY = scrollY,
      maxIndex = maxIndex,
      scrollDeltaY = deltaY,
      contentSignature = if (signatureParts > 0) signatureAcc else 0,
      timestampMs = nowMs,
    )
  }

  private fun looksLikeReelsId(viewId: String): Boolean {
    if (viewId.isEmpty()) return false
    return viewId.contains("clips") ||
      viewId.contains("reel_viewer") ||
      viewId.contains("reels_")
  }

  private fun looksLikeReelsDesc(desc: String, textLower: String, isSelected: Boolean): Boolean {
    if (isSelected && (desc == "reels" || desc.contains("reels tab") || textLower == "reels")) {
      return true
    }
    return desc.contains("reel by ") ||
      desc.contains("original audio") ||
      desc.contains("audio page") ||
      desc.contains("reels camera") ||
      desc.contains("use audio") ||
      desc.contains("like reel") ||
      desc.contains("comment on reel") ||
      textLower == "reels"
  }

  private fun looksLikeNonReelsTabSelected(desc: String, textLower: String, isSelected: Boolean): Boolean {
    if (!isSelected) return false
    val combined = desc.ifEmpty { textLower }
    return combined == "home" ||
      combined == "search and explore" ||
      combined == "explore" ||
      combined == "profile" ||
      combined == "create" ||
      combined.contains("home tab") ||
      combined.contains("search tab") ||
      combined.contains("profile tab")
  }

  private fun looksLikeReelsUfiButton(desc: String, textLower: String, viewId: String): Boolean {
    if (viewId.contains("clips_ufi") || viewId.contains("like_button") || viewId.contains("comment_button")) {
      return true
    }
    val label = desc.ifEmpty { textLower }
    return label == "like" ||
      label == "unlike" ||
      label.startsWith("like ") ||
      label == "comment" ||
      label == "comments" ||
      label.contains("comment") ||
      label == "share" ||
      label == "send" ||
      label == "remix"
  }

  private fun isStableIdentityNode(
    viewId: String,
    desc: String,
    text: String,
    textLower: String,
  ): Boolean {
    val candidate = text.ifEmpty { desc }
    if (candidate.length !in 3..64) return false
    val lower = candidate.lowercase()
    if (CHROME_WORDS.contains(lower)) return false
    // Ignore pure numbers/counts ("12.4K", "456") and duration timers ("0:12")
    if (candidate.all { it.isDigit() || it == ',' || it == '.' || it == ':' || it == 'K' || it == 'M' || it == ' ' }) {
      return false
    }
    if (viewId.contains("username") ||
      viewId.contains("caption") ||
      viewId.contains("audio") ||
      viewId.contains("clips") ||
      desc.contains("reel by ") ||
      desc.contains("original audio") ||
      desc.contains("profile picture")
    ) {
      return true
    }
    // Also capture non-chrome creator handle or caption strings when view IDs are stripped
    return text.startsWith("@") || (textLower.length in 4..48 && viewId.isEmpty())
  }
}
