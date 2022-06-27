package mega.privacy.android.app.domain.repository

import java.io.File

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.logging.loggers.FileLogMessage

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
    fun getSdkLoggingFlow(): Flow<FileLogMessage>

    /**
     * Get chat logging flow
     *
     */
    fun getChatLoggingFlow(): Flow<FileLogMessage>

    /**
     * Log to sdk file
     *
     * @param logMessage
     */
    fun logToSdkFile(logMessage: FileLogMessage)

    /**
     * Log to chat file
     *
     * @param logMessage
     */
    fun logToChatFile(logMessage: FileLogMessage)

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