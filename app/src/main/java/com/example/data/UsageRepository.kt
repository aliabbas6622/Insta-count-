package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class DayUsageSummary(
  val dateKey: String,
  val shortLabel: String,
  val displayLabel: String,
  val reelCount: Int,
  val sessionCount: Int,
  val dailyLimitSnapshot: Int,
  val thresholdLevel: ThresholdLevel,
)

data class WeeklyUsageStats(
  val days: List<DayUsageSummary>,
  val sevenDayAverage: Int,
  val currentUnderLimitStreak: Int,
  val totalWeekReels: Int,
)

class UsageRepository(
  private val dao: DailyUsageDao,
  private val preferencesRepository: PreferencesRepository,
) {
  fun observeWeeklyStats(nowEpochMs: Long = System.currentTimeMillis()): Flow<WeeklyUsageStats> {
    val keys = DateKeyHelper.trailingDateKeys(7, nowEpochMs)
    val startKey = keys.first()
    val todayKey = keys.last()
    return combine(dao.observeAllDesc(), preferencesRepository.preferences) { allEntities, prefs ->
      val byDate = allEntities.associateBy { it.dateKey }
      val summaries =
        keys.map { key ->
          val entity = byDate[key]
          val count = entity?.reelCount ?: 0
          val limit =
            if (key == todayKey) prefs.dailyLimit
            else entity?.dailyLimitSnapshot ?: prefs.dailyLimit
          val threshold =
            if (limit <= 0) ThresholdLevel.SAFE
            else {
              val ratio = count.toFloat() / limit.toFloat()
              when {
                ratio >= 1.0f -> ThresholdLevel.OVER_LIMIT
                ratio >= 0.75f -> ThresholdLevel.WARNING
                else -> ThresholdLevel.SAFE
              }
            }
          DayUsageSummary(
            dateKey = key,
            shortLabel = DateKeyHelper.shortDayLabel(key),
            displayLabel = DateKeyHelper.displayDateLabel(key, todayKey),
            reelCount = count,
            sessionCount = entity?.sessionCount ?: 0,
            dailyLimitSnapshot = limit,
            thresholdLevel = threshold,
          )
        }
      val totalWeek = summaries.sumOf { it.reelCount }
      val avg = if (summaries.isEmpty()) 0 else kotlin.math.round(totalWeek / 7.0).toInt()
      val streak = computeUnderLimitStreak(allEntities, todayKey, prefs.dailyLimit)
      WeeklyUsageStats(
        days = summaries,
        sevenDayAverage = avg,
        currentUnderLimitStreak = streak,
        totalWeekReels = totalWeek,
      )
    }
  }

  suspend fun getTodayRecord(dateKey: String): DailyUsageEntity? = dao.getByDate(dateKey)

  suspend fun saveDailyRecord(entity: DailyUsageEntity) {
    dao.upsert(entity)
  }

  suspend fun clearAllHistory() {
    dao.clearAll()
  }

  companion object {
    /**
     * Counts consecutive days ending today (or yesterday if today has 0 recorded reels yet)
     * where daily reelCount stayed within the active daily limit (or limit was disabled = 0).
     * Requires at least one recorded day to start a streak.
     */
    fun computeUnderLimitStreak(
      recordsDesc: List<DailyUsageEntity>,
      todayKey: String,
      currentDailyLimit: Int,
    ): Int {
      if (recordsDesc.isEmpty()) return 0
      val byDate = recordsDesc.associateBy { it.dateKey }
      var streak = 0
      var cursor = todayKey
      // If today has no row yet, allow streak to continue from yesterday
      if (!byDate.containsKey(todayKey)) {
        cursor = DateKeyHelper.previousDateKey(todayKey)
      }
      var guard = 0
      while (guard < 365) {
        val record = byDate[cursor] ?: break
        val effectiveLimit =
          if (cursor == todayKey) currentDailyLimit else record.dailyLimitSnapshot
        val withinLimit = effectiveLimit <= 0 || record.reelCount <= effectiveLimit
        if (!withinLimit) break
        streak++
        cursor = DateKeyHelper.previousDateKey(cursor)
        guard++
      }
      return streak
    }
  }
}
