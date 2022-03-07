package mega.privacy.android.app.domain.repository

/**
 * Logging repository
 *
 */
interface LoggingRepository {
    /**
     * Enable write sdk logs to file
     *
     */
    fun enableWriteSdkLogsToFile()

    /**
     * Disable write sdk logs to file
     *
     */
    fun disableWriteSdkLogsToFile()

    /**
     * Enable write chat logs to file
     *
     */
    fun enableWriteChatLogsToFile()

    /**
     * Disable write chat logs to file
     *
     */
    fun disableWriteChatLogsToFile()

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
}