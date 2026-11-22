package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateKeyHelper {
  fun dateKeyForEpochMs(epochMs: Long = System.currentTimeMillis()): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return formatter.format(epochMs)
  }

  fun trailingDateKeys(days: Int, endEpochMs: Long = System.currentTimeMillis()): List<String> {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = Calendar.getInstance().apply { timeInMillis = endEpochMs }
    cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
    val result = ArrayList<String>(days)
    repeat(days) {
      result.add(formatter.format(cal.time))
      cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return result
  }

  fun previousDateKey(dateKey: String): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val date = formatter.parse(dateKey) ?: return dateKey
    val cal = Calendar.getInstance().apply {
      time = date
      add(Calendar.DAY_OF_YEAR, -1)
    }
    return formatter.format(cal.time)
  }

  fun shortDayLabel(dateKey: String): String {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFmt = SimpleDateFormat("EEE", Locale.US)
    val parsed = parser.parse(dateKey) ?: return dateKey
    return labelFmt.format(parsed)
  }

  fun displayDateLabel(dateKey: String, todayKey: String): String {
    if (dateKey == todayKey) return "Today"
    if (dateKey == previousDateKey(todayKey)) return "Yesterday"
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val outFmt = SimpleDateFormat("EEE, MMM d", Locale.US)
    val parsed = parser.parse(dateKey) ?: return dateKey
    return outFmt.format(parsed)
  }
}
