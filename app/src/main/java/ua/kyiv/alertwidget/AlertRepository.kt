package ua.kyiv.alertwidget

import android.content.Context
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

    fun refresh(context: Context, manual: Boolean = false): Boolean {
        var connection: HttpURLConnection? = null
        var status: AlertStatus? = null
        try {
            connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 4_000
                readTimeout = 4_000
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val current = JSONObject(body).getJSONObject("current")
                val state = current.getInt("state")
                val values = current.getJSONArray("causes")
                val causes = (0 until values.length()).map { values.getString(it) }
                status = AlertStatusMapper.from(state, causes)
            }
        } catch (_: Exception) {
            // Keep the last known status; a failed request is never an all-clear.
        } finally {
            connection?.disconnect()
        }

        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_LAST_ATTEMPT_FAILED, status == null)
        if (manual) editor.remove(KEY_REFRESHING_AT)
        if (status != null) {
            editor.putString(KEY_STATUS, status.name)
                .putLong(KEY_CHECKED_AT, System.currentTimeMillis())
        }
        editor.apply()
        WidgetRenderer.renderAll(context)
        return status != null
    }
}
