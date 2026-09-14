package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.logging.CreateLogEntryRequest
import mega.privacy.android.domain.entity.logging.LogEntry
import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Default implementation of [CreateLogEntry].
 *
 * Every log line — SDK, Chat SDK and client app — ends up in the same log file, so
 * this is the single place that shapes them. Each entry keeps a tag identifying its
 * origin, since the file no longer separates them:
 *
 * - `[sdk]` and `[chat]` entries are written as they arrive. The native layers
 *   already provide their own context, and capturing a stack trace for them would
 *   cost more than it tells us given their volume.
 * - Everything else is a client app entry and gets a stack trace, plus a
 *   `[clientApp <version>]` tag when the caller did not supply one.
 *
 * Timestamp and log level are added by the Logback encoder (see `logback.xml`), not
 * here, so all three kinds of entry line up in the file.
 */
internal class DefaultCreateLogEntry @Inject constructor(
    private val createTraceString: CreateTraceString,
    private val environmentRepository: EnvironmentRepository,
) : CreateLogEntry {

    private var appVersion: String? = null

    override suspend fun invoke(request: CreateLogEntryRequest): LogEntry =
        if (isNativeLog(request.tag)) {
            LogEntry(
                tag = request.tag,
                message = request.message,
                stackTrace = null,
                priority = request.priority.intValue,
                throwable = request.throwable,
            )
        } else {
            LogEntry(
                tag = request.tag ?: "[clientApp ${getAppVersion()}]",
                message = request.message,
                stackTrace = createTraceString(request.trace, request.loggingClasses),
                priority = request.priority.intValue,
                throwable = request.throwable,
            )
        }

    private fun isNativeLog(tag: String?) = tag == SDK_TAG || tag == CHAT_TAG

    private suspend fun getAppVersion() =
        appVersion ?: environmentRepository.getAppInfo().appVersion.also {
            appVersion = it
        }

    private companion object {
        const val SDK_TAG = "[sdk]"
        const val CHAT_TAG = "[chat]"
    }
}
