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
    /**
     * Log to file
     *
     * @param fileLogMessage
     */
    fun logToFile(
        fileLogMessage: FileLogMessage
    ) {
        val logMessage = fileLogMessage.toString()
        with(logger) {
            when (fileLogMessage.priority) {
                Log.VERBOSE -> trace(logMessage)
                Log.DEBUG -> debug(logMessage)
                Log.INFO -> info(logMessage)
                Log.ASSERT -> info(logMessage)
                Log.WARN -> warn(logMessage)
                Log.ERROR -> {
                    if (fileLogMessage.throwable != null) {
                        error(logMessage, fileLogMessage.throwable)
                    } else {
                        error(logMessage)
                    }
                }
            }
        }

    }
}