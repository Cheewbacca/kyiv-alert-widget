package ua.kyiv.alertwidget

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import org.json.JSONException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant
import java.util.Locale
import javax.net.ssl.SSLException

internal enum class RequestSource { MANUAL, BACKGROUND }
internal enum class RequestPhase { CONNECT, HEADERS, BODY, PARSE }
internal enum class FailureKind {
    HTTP_CLIENT, HTTP_SERVER, HTTP_OTHER, TIMEOUT, DNS, CONNECTION, TLS,
    INVALID_RESPONSE, NETWORK_IO, UNEXPECTED,
}

internal object RequestFailureClassifier {
    fun fromHttpCode(code: Int): FailureKind = when (code) {
        in 400..499 -> FailureKind.HTTP_CLIENT
        in 500..599 -> FailureKind.HTTP_SERVER
        else -> FailureKind.HTTP_OTHER
    }

    fun fromException(error: Exception): FailureKind = when (error) {
        is SocketTimeoutException -> FailureKind.TIMEOUT
        is UnknownHostException -> FailureKind.DNS
        is ConnectException -> FailureKind.CONNECTION
        is SSLException -> FailureKind.TLS
        is JSONException, is IllegalArgumentException -> FailureKind.INVALID_RESPONSE
        is IOException -> FailureKind.NETWORK_IO
        else -> FailureKind.UNEXPECTED
    }
}

/** One metadata-only line per request in Logcat, plus bounded private request and error history. */
internal object RequestLogger {
    const val TAG = "KyivAlertWidget"
    private const val FILE_NAME = "request-log.txt"
    private const val ERROR_FILE_NAME = "request-errors.txt"
    private const val MAX_ENTRIES = 100
    private val writeLock = Any()

    fun success(
        context: Context,
        source: RequestSource,
        status: AlertStatus,
        durationMs: Long,
    ) {
        record(
            context,
            warning = false,
            fields = "source=${source.label()} outcome=success http=200 " +
                "status=${status.name.lowercase(Locale.ROOT)} duration_ms=${durationMs.coerceAtLeast(0)}",
        )
    }

    fun failure(
        context: Context,
        source: RequestSource,
        kind: FailureKind,
        phase: RequestPhase,
        httpCode: Int?,
        error: Exception?,
        durationMs: Long,
    ) {
        record(
            context,
            warning = true,
            fields = "source=${source.label()} outcome=failure " +
                "reason=${kind.name.lowercase(Locale.ROOT)} " +
                "phase=${phase.name.lowercase(Locale.ROOT)} http=${httpCode ?: "-"} " +
                "exception=${error?.javaClass?.simpleName?.take(64) ?: "-"} " +
                "duration_ms=${durationMs.coerceAtLeast(0)}",
        )
    }

    private fun RequestSource.label(): String = name.lowercase(Locale.ROOT)

    private fun record(context: Context, warning: Boolean, fields: String) {
        if (warning) Log.w(TAG, fields) else Log.i(TAG, fields)
        val line = "${Instant.now()} $fields"
        try {
            synchronized(writeLock) {
                if (warning) append(context, ERROR_FILE_NAME, line)
                append(context, FILE_NAME, line)
            }
        } catch (error: Exception) {
            Log.w(TAG, "request_history_write_failed exception=${error.javaClass.simpleName}")
        }
    }

    private fun append(context: Context, fileName: String, line: String) {
        val file = AtomicFile(File(context.filesDir, fileName))
        val previous = try {
            file.openRead().bufferedReader(Charsets.UTF_8).use {
                it.readLines().takeLast(MAX_ENTRIES - 1)
            }
        } catch (_: FileNotFoundException) {
            emptyList()
        }
        val bytes = (previous + line).joinToString("\n", postfix = "\n")
            .toByteArray(Charsets.UTF_8)
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }
}
