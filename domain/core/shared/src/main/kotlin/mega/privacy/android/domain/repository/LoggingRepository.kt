package mega.privacy.android.domain.repository

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
    fun enableLogAllToConsole(isDebugBuild: Boolean)

    /**
     * Initialise the underlying logging framework (e.g. Logback) so that
     * planted Timber trees can route log entries to the correct destinations.
     *
     * Must be called once at app startup, before any meaningful logging
     * happens.
     */
    suspend fun initialise()

    /**
     * Compress logs
     *
     * @return the file for the newly created archive
     */
    suspend fun compressLogs(): File

    /**
     * Force buffered log data to be written out, reducing the amount lost if the
     * process is killed. Call at lifecycle boundaries such as the app entering the
     * background.
     */
    suspend fun flush()
}
