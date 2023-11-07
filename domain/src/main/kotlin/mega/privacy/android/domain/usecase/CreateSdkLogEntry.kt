package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.logging.CreateLogEntryRequest
import mega.privacy.android.domain.entity.logging.LogEntry
import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Create sdk log entry implementation of [CreateLogEntry]
 *
 */
internal class CreateSdkLogEntry @Inject constructor(
    private val createTraceString: CreateTraceString,
    private val environmentRepository: EnvironmentRepository,
) : CreateLogEntry {

    private var appVersion: String? = null

    override suspend fun invoke(request: CreateLogEntryRequest): LogEntry? {
        return when {
            isSdkLog(request.tag) -> LogEntry(
                request.tag,
                request.message,
                null,
                request.priority.intValue,
                request.throwable
            )

            isNotSdkLog(request.trace, request.sdkLoggers) -> LogEntry(
                "[clientApp ${getAppVersion()}]",
                request.message,
                createTraceString(request.trace, request.loggingClasses),
                request.priority.intValue,
                request.throwable
            )

            else -> null
        }
    }

    private fun isSdkLog(tag: String?) = tag != null

    private fun isNotSdkLog(trace: List<StackTraceElement>, sdkLoggers: List<String>) =
        trace.none { it.className in sdkLoggers }

    private suspend fun getAppVersion() =
        appVersion ?: environmentRepository.getAppInfo().appVersion.also {
            appVersion = it
        }

}