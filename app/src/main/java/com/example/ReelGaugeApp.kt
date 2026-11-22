package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.PreferencesRepository
import com.example.data.UsageRepository
import com.example.service.NotificationController
import com.example.service.OverlayPillController
import com.example.service.ReelTrackerCoordinator

class ReelGaugeApp : Application() {
  lateinit var database: AppDatabase
    private set

  lateinit var preferencesRepository: PreferencesRepository
    private set

  lateinit var usageRepository: UsageRepository
    private set

  lateinit var notificationController: NotificationController
    private set

  lateinit var overlayPillController: OverlayPillController
    private set

  lateinit var coordinator: ReelTrackerCoordinator
    private set

  override fun onCreate() {
    super.onCreate()
    database = AppDatabase.getInstance(this)
    preferencesRepository = PreferencesRepository(this)
    usageRepository = UsageRepository(database.dailyUsageDao(), preferencesRepository)
    notificationController = NotificationController(this)
    overlayPillController = OverlayPillController(this)
    coordinator =
      ReelTrackerCoordinator(
        appContext = this,
        usageRepository = usageRepository,
        preferencesRepository = preferencesRepository,
        notificationController = notificationController,
      )
    coordinator.initialize()
  }
}
