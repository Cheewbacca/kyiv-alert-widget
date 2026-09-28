package ua.kyiv.alertwidget

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class StaleWidgetWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        WidgetRenderer.renderAll(applicationContext)
        return Result.success()
    }
}
