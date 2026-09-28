package ua.kyiv.alertwidget

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
class RefreshWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        return if (AlertRepository.refresh(applicationContext)) Result.success() else Result.retry()
    }
}
