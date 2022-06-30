package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.logging.LogEntry
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

    /**
     * Is sdk logging enabled
     *
     * @return flow that emits true if enabled else false
     */
    fun isSdkLoggingEnabled(): Flow<Boolean>

    /**
     * Set sdk logging enabled
     *
     * @param enabled
     */
    suspend fun setSdkLoggingEnabled(enabled: Boolean)

    /**
     * Is chat logging enabled
     *
     * @return flow that emits true if enabled else false
     */
    fun isChatLoggingEnabled(): Flow<Boolean>

    /**
     * Set chat logging enabled
     *
     * @param enabled
     */
    suspend fun setChatLoggingEnabled(enabled: Boolean)
}