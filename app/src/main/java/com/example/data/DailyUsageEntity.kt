package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_usage")
data class DailyUsageEntity(
  @PrimaryKey val dateKey: String,
  val reelCount: Int = 0,
  val sessionCount: Int = 0,
  val dailyLimitSnapshot: Int = 30,
  val lastUpdatedEpochMs: Long = 0L,
)
