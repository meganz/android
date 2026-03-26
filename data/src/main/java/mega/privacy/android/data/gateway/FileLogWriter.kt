package mega.privacy.android.data.gateway

import android.util.Log
import mega.privacy.android.domain.entity.logging.LogEntry
import org.slf4j.Logger

/**
 * File logger
 *
 * Writes log messages to file
 *
 * @param loggerProvider lazy provider for the SLF4J [Logger] instance
 */
internal class FileLogWriter(
    loggerProvider: () -> Logger,
) : LogWriterGateway {
    private val logger: Logger by lazy(loggerProvider)
    /**
     * Log to file
     *
     * @param logEntry
     */
    override fun writeLogEntry(
        logEntry: LogEntry,
    ) {
        val logMessage = logEntry.toString()
        with(logger) {
            when (logEntry.priority) {
                Log.VERBOSE -> trace(logMessage)
                Log.DEBUG -> debug(logMessage)
                Log.INFO -> info(logMessage)
                Log.ASSERT -> info(logMessage)
                Log.WARN -> warn(logMessage)
                Log.ERROR -> {
                    if (logEntry.throwable != null) {
                        error(logMessage, logEntry.throwable)
                    } else {
                        error(logMessage)
                    }
                }
            }
        }

    }
}