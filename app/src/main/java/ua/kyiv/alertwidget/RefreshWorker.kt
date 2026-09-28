package ua.kyiv.alertwidget

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RefreshWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                WidgetRenderer.renderAll(applicationContext)
                return Result.retry()
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(body).getJSONObject("current")
            val state = current.getInt("state")
            val values = current.getJSONArray("causes")
            val causes = (0 until values.length()).map { values.getString(it) }
            val status = AlertStatusMapper.from(state, causes)
            applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_STATUS, status.name)
                .putLong(KEY_CHECKED_AT, System.currentTimeMillis())
                .apply()
            WidgetRenderer.renderAll(applicationContext)
            Result.success()
        } catch (_: Exception) {
            WidgetRenderer.renderAll(applicationContext)
            Result.retry()
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        const val PREFS = "kyiv_alert_state"
        const val KEY_STATUS = "status"
        const val KEY_CHECKED_AT = "checked_at"
        private const val ENDPOINT = "https://kyiv.digital/open-api/air-alert/state"
    }
}
