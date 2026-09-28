package ua.kyiv.alertwidget

import java.util.concurrent.TimeUnit

internal enum class WidgetLevel { GREEN, YELLOW, RED, UNKNOWN }

internal data class WidgetVisualState(
    val level: WidgetLevel,
    val refreshing: Boolean,
    val stale: Boolean,
)

internal object WidgetStatePolicy {
    private val staleAfterMillis = TimeUnit.MINUTES.toMillis(45)
    private val refreshingWindowMillis = TimeUnit.SECONDS.toMillis(15)

    fun evaluate(
        status: AlertStatus?,
        checkedAt: Long,
        refreshingAt: Long,
        now: Long,
    ): WidgetVisualState {
        val age = now - checkedAt
        val fresh = status != null && checkedAt > 0 && age >= 0 && age < staleAfterMillis
        val level = when {
            !fresh -> WidgetLevel.UNKNOWN
            status == AlertStatus.CLEAR -> WidgetLevel.GREEN
            status == AlertStatus.YELLOW -> WidgetLevel.YELLOW
            else -> WidgetLevel.RED
        }
        val refreshAge = now - refreshingAt
        return WidgetVisualState(
            level = level,
            refreshing = refreshingAt > 0 && refreshAge >= 0 && refreshAge < refreshingWindowMillis,
            stale = checkedAt > 0 && !fresh,
        )
    }
}
