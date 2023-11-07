package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.logging.CreateLogEntryRequest
import mega.privacy.android.domain.entity.logging.LogEntry
import mega.privacy.android.domain.entity.logging.LogPriority
import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Create chat log entry implementation of [CreateLogEntry]
 *
 */
internal class CreateChatLogEntry @Inject constructor(
    private val createTraceString: CreateTraceString,
    private val getCurrentTimeString: GetCurrentTimeString,
    private val environmentRepository: EnvironmentRepository,
) : CreateLogEntry {

    private var appVersion: String? = null

    override suspend fun invoke(request: CreateLogEntryRequest): LogEntry? {
        val entry = if (isChatLog(request.tag)) {
            var megaTag: String? = null
            var stackTrace: String? = null

            if (isNotSdkLog(request.trace, request.sdkLoggers)) {
                megaTag = createClientAppTag(request.priority)
                stackTrace = createTraceString(request.trace, request.loggingClasses)
            }
            LogEntry(
                megaTag,
                request.message,
                stackTrace,
                request.priority.intValue,
                request.throwable
            )
        } else null
        return entry
    }

    private fun isChatLog(tag: String?) = tag == null

    private fun isNotSdkLog(trace: List<StackTraceElement>, sdkLoggers: List<String>) =
        trace.none { it.className in sdkLoggers }

    private suspend fun createClientAppTag(priority: LogPriority): String =
        "[${getFormattedTime()}][${priority.name}][clientApp ${getAppVersion()}]"

    private suspend fun getAppVersion() =
        appVersion ?: environmentRepository.getAppInfo().appVersion.also {
            appVersion = it
        }


    private suspend fun getFormattedTime() = getCurrentTimeString("dd-MM HH:mm:ss", "UTC")
}