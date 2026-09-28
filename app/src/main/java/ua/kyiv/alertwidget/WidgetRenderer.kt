package ua.kyiv.alertwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object WidgetRenderer {
    private val staleAfterMillis = TimeUnit.MINUTES.toMillis(45)

    fun renderAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, AlertWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val prefs = context.getSharedPreferences(RefreshWorker.PREFS, Context.MODE_PRIVATE)
        val checkedAt = prefs.getLong(RefreshWorker.KEY_CHECKED_AT, 0L)
        val status = prefs.getString(RefreshWorker.KEY_STATUS, null)
            ?.let { runCatching { AlertStatus.valueOf(it) }.getOrNull() }
        val stale = checkedAt > 0 && System.currentTimeMillis() - checkedAt > staleAfterMillis
        val time = if (checkedAt > 0) SimpleDateFormat("HH:mm", Locale("uk", "UA"))
            .format(Date(checkedAt)) else null

        val (textRes, color) = when {
            status == null -> R.string.no_data to Color.GRAY
            stale -> R.string.no_data to Color.GRAY
            status == AlertStatus.CLEAR -> R.string.clear to Color.rgb(25, 118, 72)
            status == AlertStatus.YELLOW -> R.string.yellow to Color.rgb(143, 103, 0)
            status == AlertStatus.RED -> R.string.red to Color.rgb(185, 36, 46)
            else -> R.string.unknown to Color.rgb(185, 36, 46)
        }
        val subtext = when {
            time == null -> context.getString(R.string.tap_to_refresh)
            stale -> context.getString(R.string.stale_at, time)
            else -> context.getString(R.string.checked_at, time)
        }
        val refreshIntent = Intent(context, AlertWidgetProvider::class.java).apply {
            action = AlertWidgetProvider.ACTION_REFRESH
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.alert_widget)
            views.setTextViewText(R.id.widget_status, context.getString(textRes))
            views.setTextColor(R.id.widget_status, color)
            views.setTextViewText(R.id.widget_checked_at, subtext)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            manager.updateAppWidget(id, views)
        }
    }
}
