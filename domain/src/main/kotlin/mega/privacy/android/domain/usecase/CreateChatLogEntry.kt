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
        return when {
            isChatLog(request.tag) -> LogEntry(
                null,
                request.message,
                null,
                request.priority.intValue,
                request.throwable
            )

            request.tag != "[sdk]" -> LogEntry(
                request.tag ?: createClientAppTag(request.priority),
                request.message,
                createTraceString(request.trace, request.loggingClasses),
                request.priority.intValue,
                request.throwable
            )

            else -> null
        }
    }

    private fun isChatLog(tag: String?) = tag == "[chat_sdk]"

    private suspend fun createClientAppTag(priority: LogPriority): String =
        "[${getFormattedTime()}][${priority.name}][clientApp ${getAppVersion()}]"

    private suspend fun getAppVersion() =
        appVersion ?: environmentRepository.getAppInfo().appVersion.also {
            appVersion = it
        }


    private suspend fun getFormattedTime() = getCurrentTimeString("MM-dd HH:mm:ss", "UTC")
}