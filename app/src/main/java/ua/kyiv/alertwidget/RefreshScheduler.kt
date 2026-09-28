package ua.kyiv.alertwidget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object RefreshScheduler {
    private const val PERIODIC_NAME = "kyiv-alert-periodic"
    private const val IMMEDIATE_NAME = "kyiv-alert-immediate"
    private val network = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun ensurePeriodic(context: Context) {
        val work = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES)
            .setConstraints(network)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, work
        )
    }

    fun refreshNow(context: Context) {
        val work = OneTimeWorkRequestBuilder<RefreshWorker>()
            .setConstraints(network)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_NAME, ExistingWorkPolicy.KEEP, work
        )
    }

    fun stopPeriodic(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_NAME)
    }
}
