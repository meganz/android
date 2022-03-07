package mega.privacy.android.app.logging

import android.content.Context

interface LegacyLoggingSettings{

    /**
     * Enables or disables the SDK logs depending on the "enabled" parameter.
     *
     * @param context Context from where the logs are being to be enabled/disabled.
     * @param enabled True to enable logs or false to disable,
     */
    fun setStatusLoggerSDK(context: Context, enabled: Boolean)

    /**
     * Enables or disables the Karere logs depending on the "enabled" parameter.
     *
     * @param context Context from where the logs are being to be enabled/disabled.
     * @param enabled True to enable logs or false to disable,
     */
    fun setStatusLoggerKarere(context: Context, enabled: Boolean)


    /**
     * Reset the current SDK logger.
     */
    fun resetLoggerSDK()


    /**
     * Checks if SDK logs are enabled.
     *
     * @return True if SDK logs are enabled, false otherwise.
     */
    fun areSDKLogsEnabled(): Boolean

    /**
     * Updates SDK logs preference.
     *
     * @param enabled True if should enable SDK logs, false otherwise.
     */
    fun updateSDKLogs(enabled: Boolean)

    /**
     * Checks if SDK logs are enabled.
     *
     * @return True if SDK logs are enabled, false otherwise.
     */
    fun areKarereLogsEnabled(): Boolean

    /**
     * Updates Karere logs preference.
     *
     * @param enabled True if should enable Karere logs, false otherwise.
     */
    fun updateKarereLogs(enabled: Boolean)
}