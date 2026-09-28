package ua.kyiv.alertwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WidgetRenderer {
    private data class Palette(
        val badge: Int,
        val tile: Int,
        val shield: Int,
        val refresh: Int,
        val foreground: Int,
    )

    fun showRefreshing(context: Context) {
        context.getSharedPreferences(AlertRepository.PREFS, Context.MODE_PRIVATE).edit()
            .putLong(AlertRepository.KEY_REFRESHING_AT, System.currentTimeMillis())
            .apply()
        renderAll(context)
    }

    fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return widgetIds(manager, context, AlertWidgetProvider::class.java).isNotEmpty() ||
            widgetIds(manager, context, CompactAlertWidgetProvider::class.java).isNotEmpty()
    }

    fun renderAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val largeIds = widgetIds(manager, context, AlertWidgetProvider::class.java)
        val compactIds = widgetIds(manager, context, CompactAlertWidgetProvider::class.java)
        if (largeIds.isEmpty() && compactIds.isEmpty()) return

        val prefs = context.getSharedPreferences(AlertRepository.PREFS, Context.MODE_PRIVATE)
        val checkedAt = prefs.getLong(AlertRepository.KEY_CHECKED_AT, 0L)
        val status = prefs.getString(AlertRepository.KEY_STATUS, null)
            ?.let { runCatching { AlertStatus.valueOf(it) }.getOrNull() }
        val visual = WidgetStatePolicy.evaluate(
            status = status,
            checkedAt = checkedAt,
            refreshingAt = prefs.getLong(AlertRepository.KEY_REFRESHING_AT, 0L),
            now = System.currentTimeMillis(),
        )
        val lastAttemptFailed = prefs.getBoolean(AlertRepository.KEY_LAST_ATTEMPT_FAILED, false)
        val longTime = checkedAt.takeIf { it > 0 }?.let { formatTime(it, "HH:mm:ss") }
        val shortTime = checkedAt.takeIf { it > 0 }?.let { formatTime(it, "HH:mm") }
        val palette = palette(visual.level)
        val statusText = when (visual.level) {
            WidgetLevel.GREEN -> R.string.clear
            WidgetLevel.YELLOW -> R.string.yellow
            WidgetLevel.RED -> R.string.red
            WidgetLevel.UNKNOWN -> if (visual.refreshing && checkedAt == 0L) R.string.loading else R.string.no_data
        }
        val detail = when {
            visual.refreshing && shortTime != null -> context.getString(R.string.updating_at, shortTime)
            visual.refreshing -> context.getString(R.string.time_unavailable)
            visual.stale && shortTime != null -> context.getString(R.string.stale_at, shortTime)
            lastAttemptFailed && shortTime != null -> context.getString(R.string.refresh_failed_at, shortTime)
            lastAttemptFailed -> context.getString(R.string.refresh_failed)
            longTime != null -> context.getString(R.string.checked_at, longTime)
            else -> context.getString(R.string.tap_to_refresh)
        }
        val statusDescription = when {
            visual.level == WidgetLevel.RED && status == AlertStatus.ACTIVE_UNKNOWN ->
                context.getString(R.string.unknown_description)
            else -> context.getString(statusText)
        }
        var description = if (longTime == null) statusDescription else
            context.getString(R.string.accessibility_verified, statusDescription, longTime)
        if (visual.stale) description = context.getString(R.string.accessibility_stale, description)
        if (lastAttemptFailed && !visual.refreshing) {
            description = context.getString(R.string.accessibility_failed, description)
        }
        if (visual.refreshing) {
            description = context.getString(R.string.accessibility_refreshing, description)
        }
        val refreshIntent = Intent(context, AlertWidgetProvider::class.java).apply {
            action = AlertWidgetProvider.ACTION_REFRESH
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        for (id in largeIds) {
            val views = RemoteViews(context.packageName, R.layout.alert_widget)
            views.setImageViewResource(R.id.widget_badge_background, palette.badge)
            views.setImageViewResource(R.id.widget_badge_shield, palette.shield)
            views.setTextViewText(R.id.widget_status, context.getString(statusText))
            views.setTextViewText(R.id.widget_checked_at, detail)
            views.setViewVisibility(
                R.id.widget_refresh_indicator, if (visual.refreshing) View.VISIBLE else View.GONE
            )
            views.setContentDescription(R.id.widget_root, description)
            for (target in intArrayOf(
                R.id.widget_root, R.id.widget_content, R.id.widget_badge,
                R.id.widget_badge_background, R.id.widget_badge_shield,
                R.id.widget_city, R.id.widget_status, R.id.widget_checked_at,
                R.id.widget_refresh_indicator,
            )) views.setOnClickPendingIntent(target, pendingIntent)
            manager.updateAppWidget(id, views)
        }

        val compactTime = shortTime ?: context.getString(R.string.time_unavailable)
        for (id in compactIds) {
            val views = RemoteViews(context.packageName, R.layout.alert_widget_compact)
            views.setImageViewResource(R.id.compact_background, palette.tile)
            views.setImageViewResource(R.id.compact_shield, palette.shield)
            views.setImageViewResource(R.id.compact_refresh_indicator, palette.refresh)
            views.setTextViewText(R.id.compact_checked_at, compactTime)
            views.setTextColor(R.id.compact_checked_at, context.getColor(palette.foreground))
            views.setViewVisibility(
                R.id.compact_refresh_indicator, if (visual.refreshing) View.VISIBLE else View.GONE
            )
            views.setContentDescription(R.id.compact_root, description)
            for (target in intArrayOf(
                R.id.compact_root, R.id.compact_background, R.id.compact_content,
                R.id.compact_shield, R.id.compact_checked_at, R.id.compact_refresh_indicator,
            )) views.setOnClickPendingIntent(target, pendingIntent)
            manager.updateAppWidget(id, views)
        }
    }

    private fun widgetIds(
        manager: AppWidgetManager,
        context: Context,
        provider: Class<*>,
    ): IntArray = manager.getAppWidgetIds(ComponentName(context, provider))

    private fun formatTime(timestamp: Long, pattern: String): String =
        SimpleDateFormat(pattern, Locale("uk", "UA")).format(Date(timestamp))

    private fun palette(level: WidgetLevel): Palette = when (level) {
        WidgetLevel.GREEN -> Palette(
            R.drawable.widget_badge_green, R.drawable.widget_tile_green,
            R.drawable.ic_shield_light, R.drawable.ic_refresh_light,
            R.color.on_alert_light,
        )
        WidgetLevel.YELLOW -> Palette(
            R.drawable.widget_badge_yellow, R.drawable.widget_tile_yellow,
            R.drawable.ic_shield_dark, R.drawable.ic_refresh_dark,
            R.color.on_alert_yellow,
        )
        WidgetLevel.RED -> Palette(
            R.drawable.widget_badge_red, R.drawable.widget_tile_red,
            R.drawable.ic_shield_light, R.drawable.ic_refresh_light,
            R.color.on_alert_light,
        )
        WidgetLevel.UNKNOWN -> Palette(
            R.drawable.widget_badge_unknown, R.drawable.widget_tile_unknown,
            R.drawable.ic_shield_light, R.drawable.ic_refresh_light,
            R.color.on_alert_light,
        )
    }
}
