package ua.kyiv.alertwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

open class AlertWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetRenderer.renderAll(context)
        RefreshScheduler.ensurePeriodic(context)
        RefreshScheduler.refreshNow(context)
    }

    override fun onEnabled(context: Context) {
        RefreshScheduler.ensurePeriodic(context)
        RefreshScheduler.refreshNow(context)
    }

    override fun onDisabled(context: Context) {
        if (!WidgetRenderer.hasWidgets(context)) RefreshScheduler.stopPeriodic(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            val appContext = context.applicationContext
            WidgetRenderer.showRefreshing(appContext)
            val pendingResult = goAsync()
            Thread({
                try {
                    AlertRepository.refresh(appContext, RequestSource.MANUAL)
                } finally {
                    pendingResult.finish()
                }
            }, "kyiv-alert-manual-refresh").start()
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_REFRESH = "ua.kyiv.alertwidget.REFRESH"
    }
}

class CompactAlertWidgetProvider : AlertWidgetProvider()
