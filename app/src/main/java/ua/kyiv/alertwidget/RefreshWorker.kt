package ua.kyiv.alertwidget

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
class RefreshWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        WidgetRenderer.showRefreshing(applicationContext)
        AlertRepository.refresh(applicationContext, RequestSource.BACKGROUND)
        return Result.success()
    }
}
