package ua.kyiv.alertwidget

import android.content.Context
import android.os.SystemClock
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AlertRepository {
    const val PREFS = "kyiv_alert_state"
    const val KEY_STATUS = "status"
    const val KEY_CHECKED_AT = "checked_at"
    const val KEY_REFRESHING_AT = "refreshing_at"
    const val KEY_LAST_ATTEMPT_FAILED = "last_attempt_failed"

    private const val ENDPOINT = "https://kyiv.digital/open-api/air-alert/state"

    internal fun refresh(context: Context, source: RequestSource): Boolean {
        val startedAt = SystemClock.elapsedRealtime()
        var connection: HttpURLConnection? = null
        var status: AlertStatus? = null
        var httpCode: Int? = null
        var phase = RequestPhase.CONNECT
        var failureKind: FailureKind? = null
        var failure: Exception? = null
        try {
            connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 4_000
                readTimeout = 4_000
            }
            phase = RequestPhase.HEADERS
            httpCode = connection.responseCode
            if (httpCode == HttpURLConnection.HTTP_OK) {
                phase = RequestPhase.BODY
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                phase = RequestPhase.PARSE
                val current = JSONObject(body).getJSONObject("current")
                val state = current.getInt("state")
                val values = current.getJSONArray("causes")
                val causes = (0 until values.length()).map { values.getString(it) }
                status = AlertStatusMapper.from(state, causes)
            } else {
                failureKind = RequestFailureClassifier.fromHttpCode(httpCode ?: -1)
            }
        } catch (error: Exception) {
            failure = error
            failureKind = RequestFailureClassifier.fromException(error)
            // Keep the last known status; a failed request is never an all-clear.
        } finally {
            connection?.disconnect()
        }

        val durationMs = SystemClock.elapsedRealtime() - startedAt
        if (status != null) {
            RequestLogger.success(context, source, status, durationMs)
        } else {
            RequestLogger.failure(
                context, source, failureKind ?: FailureKind.UNEXPECTED,
                phase, httpCode, failure, durationMs,
            )
        }

        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_LAST_ATTEMPT_FAILED, status == null)
            .remove(KEY_REFRESHING_AT)
        if (status != null) {
            val checkedAt = System.currentTimeMillis()
            editor.putString(KEY_STATUS, status.name)
                .putLong(KEY_CHECKED_AT, checkedAt)
        }
        editor.apply()
        WidgetRenderer.renderAll(context)
        if (status != null && WidgetRenderer.hasWidgets(context)) {
            RefreshScheduler.scheduleFreshnessCheck(context)
        }
        return status != null
    }
}
