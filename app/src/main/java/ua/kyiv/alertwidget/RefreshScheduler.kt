package ua.kyiv.alertwidget

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object RefreshScheduler {
    private const val PERIODIC_NAME = "kyiv-alert-periodic"
    private const val IMMEDIATE_NAME = "kyiv-alert-immediate"
    private const val FRESHNESS_NAME = "kyiv-alert-freshness"

    fun ensurePeriodic(context: Context) {
        val work = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME, ExistingPeriodicWorkPolicy.UPDATE, work
        )
    }

    fun refreshNow(context: Context) {
        val work = OneTimeWorkRequestBuilder<RefreshWorker>()
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_NAME, ExistingWorkPolicy.KEEP, work
        )
    }

    fun scheduleFreshnessCheck(context: Context) {
        val work = OneTimeWorkRequestBuilder<StaleWidgetWorker>()
            .setInitialDelay(45, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            FRESHNESS_NAME, ExistingWorkPolicy.REPLACE, work
        )
    }

    fun stopPeriodic(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(FRESHNESS_NAME)
    }
}
