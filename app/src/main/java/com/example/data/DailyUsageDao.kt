package com.example.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {
  @Query("SELECT * FROM daily_usage WHERE dateKey >= :startDateKey ORDER BY dateKey ASC")
  fun observeUsageSince(startDateKey: String): Flow<List<DailyUsageEntity>>

  @Query("SELECT * FROM daily_usage ORDER BY dateKey DESC")
  fun observeAllDesc(): Flow<List<DailyUsageEntity>>

  @Query("SELECT * FROM daily_usage WHERE dateKey = :dateKey LIMIT 1")
  fun observeByDate(dateKey: String): Flow<DailyUsageEntity?>

  @Query("SELECT * FROM daily_usage WHERE dateKey = :dateKey LIMIT 1")
  suspend fun getByDate(dateKey: String): DailyUsageEntity?

  @Query("SELECT * FROM daily_usage ORDER BY dateKey DESC")
  suspend fun getAllDesc(): List<DailyUsageEntity>

  @Upsert suspend fun upsert(entity: DailyUsageEntity)

  @Query("DELETE FROM daily_usage") suspend fun clearAll()
}
