package mega.privacy.android.app.logging.loggers

import mega.privacy.android.app.logging.ChatLogger
import mega.privacy.android.app.logging.SdkLogger
import mega.privacy.android.app.logging.TimberLegacyLog
import mega.privacy.android.app.utils.LogUtil
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Mega file log tree
 *
 * Writes logs to the default log file, adding additional information for log messages
 * from the client app. (Identified by not having an existing tag)
 */
class MegaFileLogTree @Inject constructor(
    @SdkLogger private val sdkLogger: FileLogger,
    @ChatLogger private val chatLogger: FileLogger,
) : Timber.Tree() {

    private val ignoredClasses = listOf(
        Timber::class.java.name,
        Timber.Forest::class.java.name,
        Timber.Tree::class.java.name,
        Timber.DebugTree::class.java.name,
        TimberLegacyLog::class.java.name,
        LogUtil::class.java.name,
        MegaFileLogTree::class.java.name,
    )

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (isSdkLog(tag)) {
            sdkLogger.logToFile(tag, message, null, priority, t)
        } else {
            val trace = Throwable().stackTrace
            var megaTag: String? = null
            var stackTrace: String? = null

            if (isAppLog(trace)) {
                megaTag = createTag()
                stackTrace = createTrace(trace)
                sdkLogger.logToFile(megaTag, message, stackTrace, priority, t)
            }

            chatLogger.logToFile(megaTag, message, stackTrace, priority, t)
        }
    }

    private fun isSdkLog(tag: String?) = tag != null

    private fun isAppLog(trace: Array<out StackTraceElement?>?) =
        trace?.none { it?.className?.contains(TimberChatLogger::class.java.name) ?: true } ?: true

    private fun createTrace(trace: Array<StackTraceElement>): String? {
        return trace
            .firstOrNull { it.className !in ignoredClasses }
            ?.let { "${it.fileName}#${it.methodName}:${it.lineNumber}" }
    }

    private fun createTag(): String = "[ ${getFormattedTime()} ][ clientApp ]"

    private fun getFormattedTime() = DateTimeFormatter.ofPattern("dd-MM HH:mm:ss")
        .withZone(ZoneId.from(ZoneOffset.UTC))
        .format(Instant.now())
}