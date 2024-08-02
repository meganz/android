package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.logging.LogEntry
import java.io.File

/**
 * Logging repository
 *
 */
interface LoggingRepository {

    /**
     * Enable log all to console
     *
     */
    fun enableLogAllToConsole()

    /**
     * Reset sdk logging
     *
     */
    fun resetSdkLogging()

    /**
     * Get sdk logging flow
     *
     */
    fun getSdkLoggingFlow(): Flow<LogEntry>

    /**
     * Get chat logging flow
     *
     */
    fun getChatLoggingFlow(): Flow<LogEntry>

    /**
     * Log to sdk file
     *
     * @param logMessage
     */
    suspend fun logToSdkFile(logMessage: LogEntry)

    /**
     * Log to chat file
     *
     * @param logMessage
     */
    suspend fun logToChatFile(logMessage: LogEntry)

    /**
     * Compress logs
     *
     * @return the file for the newly created archive
     */
    suspend fun compressLogs(): File
}