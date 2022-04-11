package mega.privacy.android.app.logging.loggers

import android.util.Log
import org.slf4j.Logger

/**
 * File logger
 *
 * Writes log messages to file
 *
 * @property logger
 */
class FileLogger(
    private val logger: Logger
) {

    var enabled: Boolean = false

    fun logToFile(
        tag: String?,
        message: String,
        stackTrace: String?,
        priority: Int,
        t: Throwable?
    ) {
        if (enabled) {
            val logMessage = "${tag.orEmpty()} $message ${stackTrace.orEmpty()}".trim()
            with(logger) {
                when (priority) {
                    Log.VERBOSE -> trace(logMessage)
                    Log.DEBUG -> debug(logMessage)
                    Log.INFO -> info(logMessage)
                    Log.ASSERT -> info(logMessage)
                    Log.WARN -> warn(logMessage)
                    Log.ERROR -> {
                        if (t != null) {
                            error(logMessage, t)
                        } else {
                            error(logMessage)
                        }
                    }
                }
            }
        }
    }
}