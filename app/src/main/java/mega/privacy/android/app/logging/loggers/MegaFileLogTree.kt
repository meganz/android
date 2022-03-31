package mega.privacy.android.app.logging.loggers

import android.util.Log
import mega.privacy.android.app.logging.TimberLegacyLog
import mega.privacy.android.app.utils.LogUtil
import org.slf4j.LoggerFactory
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
class MegaFileLogTree @Inject constructor() : Timber.Tree() {
    private val fileLogger = LoggerFactory.getLogger(TimberMegaLogger::class.java)
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
        val megaTag = tag ?: createTag()
        val stackTrace = if (tag == null) createTrace() else null
        val logMessage = "$megaTag $message ${stackTrace.orEmpty()}".trim()
        when (priority) {
            Log.VERBOSE -> fileLogger.trace(logMessage)
            Log.DEBUG -> fileLogger.debug(logMessage)
            Log.INFO -> fileLogger.info(logMessage)
            Log.ASSERT -> fileLogger.info(logMessage)
            Log.WARN -> fileLogger.warn(logMessage)
            Log.ERROR -> {
                if (t != null) {
                    fileLogger.error(logMessage, t)
                } else {
                    fileLogger.error(logMessage)
                }
            }
        }
    }

    private fun createTrace(): String? {
        return Throwable().stackTrace
            .firstOrNull{ it.className !in ignoredClasses }
            ?.let { "${it.fileName}#${it.methodName}:${it.lineNumber}" }
    }

    private fun createTag(): String = "[ ${getFormattedTime()} ][ clientApp ]:"

    private fun getFormattedTime() = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.from(ZoneOffset.UTC))
        .format(Instant.now())
}