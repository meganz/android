package mega.privacy.android.app.domain.usecase

import android.util.Log
import mega.privacy.android.app.domain.entity.logging.CreateLogEntryRequest
import mega.privacy.android.app.domain.entity.logging.LogEntry
import javax.inject.Inject

/**
 * Create chat log entry implementation of [CreateLogEntry]
 *
 */
class CreateChatLogEntry @Inject constructor(
    private val createTraceString: CreateTraceString,
    private val getCurrentTimeString: GetCurrentTimeString,
) : CreateLogEntry {
    override suspend fun invoke(request: CreateLogEntryRequest): LogEntry? {
        val entry = if (isChatLog(request.tag)) {
            var megaTag: String? = null
            var stackTrace: String? = null

            if (isNotSdkLog(request.trace, request.sdkLoggers)) {
                megaTag = createClientAppTag(request.priority)
                stackTrace = createTraceString(request.trace, request.loggingClasses)
            }
            LogEntry(megaTag, request.message, stackTrace, request.priority, request.throwable)
        } else null
        return entry
    }

    private fun isChatLog(tag: String?) = tag == null

    private fun isNotSdkLog(trace: List<StackTraceElement>, sdkLoggers: List<String>) =
        trace.none { it.className in sdkLoggers }

    private suspend fun createClientAppTag(priority: Int): String =
        "[${getFormattedTime()}][${getPriorityString(priority)}][clientApp]"

    private fun getPriorityString(priority: Int) = when (priority) {
        Log.VERBOSE -> "VERBOSE"
        Log.DEBUG -> "DEBUG"
        Log.INFO -> "INFO"
        Log.ASSERT -> "ASSERT"
        Log.WARN -> "WARN"
        Log.ERROR -> "ERROR"
        else -> "UNKNOWN"
    }

    private suspend fun getFormattedTime() = getCurrentTimeString("dd-MM HH:mm:ss", "UTC")
}