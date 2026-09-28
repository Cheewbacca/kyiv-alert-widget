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
    private val refreshingWindowMillis = TimeUnit.SECONDS.toMillis(15)

    fun showRefreshing(context: Context) {
        context.getSharedPreferences(AlertRepository.PREFS, Context.MODE_PRIVATE).edit()
            .putLong(AlertRepository.KEY_REFRESHING_AT, System.currentTimeMillis())
            .apply()
        renderAll(context)
    }

    fun renderAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, AlertWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val prefs = context.getSharedPreferences(AlertRepository.PREFS, Context.MODE_PRIVATE)
        val checkedAt = prefs.getLong(AlertRepository.KEY_CHECKED_AT, 0L)
        val status = prefs.getString(AlertRepository.KEY_STATUS, null)
            ?.let { runCatching { AlertStatus.valueOf(it) }.getOrNull() }
        val refreshingAt = prefs.getLong(AlertRepository.KEY_REFRESHING_AT, 0L)
        val refreshing = refreshingAt > 0 &&
            System.currentTimeMillis() - refreshingAt < refreshingWindowMillis
        val lastAttemptFailed = prefs.getBoolean(AlertRepository.KEY_LAST_ATTEMPT_FAILED, false)
        val stale = checkedAt > 0 && System.currentTimeMillis() - checkedAt > staleAfterMillis
        val time = if (checkedAt > 0) SimpleDateFormat("HH:mm:ss", Locale("uk", "UA"))
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
            refreshing -> context.getString(R.string.loading)
            lastAttemptFailed && time != null -> context.getString(R.string.refresh_failed_at, time)
            lastAttemptFailed -> context.getString(R.string.refresh_failed)
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
            views.setOnClickPendingIntent(R.id.widget_content, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_city, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_status, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_checked_at, pendingIntent)
            manager.updateAppWidget(id, views)
        }
    }
}
