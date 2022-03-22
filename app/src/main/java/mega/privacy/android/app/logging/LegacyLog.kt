package mega.privacy.android.app.logging

interface LegacyLog {
    /**
     * Send a log message with FATAL level to the logging system.
     *
     * @param message Message for the logging system.
     */
    fun logFatal(message: String)

    /**
     * Send a log message with FATAL level to the logging system.
     *
     * @param message   Message for the logging system.
     * @param exception Exception which produced the error.
     */
    fun logFatal(message: String, exception: Throwable)

    /**
     * Send a log message with ERROR level to the logging system.
     *
     * @param message Message for the logging system.
     */
    fun logError(message: String?)

    /**
     * Send a log message with ERROR level to the logging system.
     *
     * @param message   Message for the logging system.
     * @param exception Exception which produced the error.
     */
    fun logError(message: String?, exception: Throwable?)

    /**
     * Send a log message with WARNING level to the logging system.
     *
     * @param message Message for the logging system.
     */
    fun logWarning(message: String)

    /**
     * Send a log message with WARNING level to the logging system.
     *
     * @param message   Message for the logging system.
     * @param exception Exception which produced the warning.
     */
    fun logWarning(message: String, exception: Throwable)

    /**
     * Send a log message with INFO level to the logging system.
     *
     * @param message Message for the logging system.
     */
    fun logInfo(message: String)

    /**
     * Send a log message with DEBUG level to the logging system.
     *
     * @param message Message for the logging system.
     */
    fun logDebug(message: String)

    /**
     * Send a log message with MAX level to the logging system.
     *
     * @param message Message for the logging system.
     */
    fun logMax(message: String)
}