package mega.privacy.android.app.utils

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.di.LegacyLoggingEntryPoint
import mega.privacy.android.app.featuretoggle.PurgeLogsToggle
import mega.privacy.android.app.logging.LegacyLog
import mega.privacy.android.app.logging.LegacyLogUtil
import mega.privacy.android.app.logging.LegacyLoggingSettings
import mega.privacy.android.app.logging.TimberLegacyLog

object LogUtil {
    private val legacyLogUtil: LegacyLoggingSettings by lazy {
        EntryPointAccessors.fromApplication(
            MegaApplication.getInstance(),
            LegacyLoggingEntryPoint::class.java
        ).legacyLoggingSettings
    }

    private val legacyLogger: LegacyLog by lazy {
        if (PurgeLogsToggle.enabled) {
            TimberLegacyLog()
        } else {
            LegacyLogUtil()
        }
    }

    /**
     * Send a log message with FATAL level to the logging system.
     *
     * @param message Message for the logging system.
     */
    @JvmStatic
    fun logFatal(message: String?) {
        legacyLogger.logFatal(message!!)
    }

    /**
     * Send a log message with FATAL level to the logging system.
     *
     * @param message   Message for the logging system.
     * @param exception Exception which produced the error.
     */
    @JvmStatic
    fun logFatal(message: String?, exception: Throwable?) {
        legacyLogger.logFatal(message!!, exception!!)
    }

    /**
     * Send a log message with ERROR level to the logging system.
     *
     * @param message Message for the logging system.
     */
    @JvmStatic
    fun logError(message: String?) {
        legacyLogger.logError(message)
    }

    /**
     * Send a log message with ERROR level to the logging system.
     *
     * @param message   Message for the logging system.
     * @param exception Exception which produced the error.
     */
    @JvmStatic
    fun logError(message: String?, exception: Throwable?) {
        legacyLogger.logError(message, exception)
    }

    /**
     * Send a log message with WARNING level to the logging system.
     *
     * @param message Message for the logging system.
     */
    @JvmStatic
    fun logWarning(message: String?) {
        legacyLogger.logWarning(message!!)
    }

    /**
     * Send a log message with WARNING level to the logging system.
     *
     * @param message   Message for the logging system.
     * @param exception Exception which produced the warning.
     */
    @JvmStatic
    fun logWarning(message: String?, exception: Throwable?) {
        legacyLogger.logWarning(message!!, exception!!)
    }

    /**
     * Send a log message with INFO level to the logging system.
     *
     * @param message Message for the logging system.
     */
    @JvmStatic
    fun logInfo(message: String?) {
        legacyLogger.logInfo(message!!)
    }

    /**
     * Send a log message with DEBUG level to the logging system.
     *
     * @param message Message for the logging system.
     */
    @JvmStatic
    fun logDebug(message: String?) {
        legacyLogger.logDebug(message!!)
    }

    /**
     * Send a log message with MAX level to the logging system.
     *
     * @param message Message for the logging system.
     */
    @JvmStatic
    fun logMax(message: String?) {
        legacyLogger.logMax(message!!)
    }

    /**
     * Enables or disables the SDK logs depending on the "enabled" parameter.
     *
     * @param context Context from where the logs are being to be enabled/disabled.
     * @param enabled True to enable logs or false to disable,
     */
    @JvmStatic
    fun setStatusLoggerSDK(context: Context?, enabled: Boolean) {
        legacyLogUtil.setStatusLoggerSDK(context!!, enabled)
    }

    /**
     * Enables or disables the Karere logs depending on the "enabled" parameter.
     *
     * @param context Context from where the logs are being to be enabled/disabled.
     * @param enabled True to enable logs or false to disable,
     */
    @JvmStatic
    fun setStatusLoggerKarere(context: Context?, enabled: Boolean) {
        legacyLogUtil.setStatusLoggerKarere(context!!, enabled)
    }

    /**
     * Reset the current SDK logger.
     */
    @JvmStatic
    fun resetLoggerSDK() {
        legacyLogUtil.resetLoggerSDK()
    }

    /**
     * Checks if SDK logs are enabled.
     *
     * @return True if SDK logs are enabled, false otherwise.
     */
    @JvmStatic
    fun areSDKLogsEnabled(): Boolean {
        return legacyLogUtil.areSDKLogsEnabled()
    }

    /**
     * Updates SDK logs preference.
     *
     * @param enabled True if should enable SDK logs, false otherwise.
     */
    @JvmStatic
    fun updateSDKLogs(enabled: Boolean) {
        legacyLogUtil.updateSDKLogs(enabled)
    }

    /**
     * Checks if SDK logs are enabled.
     *
     * @return True if SDK logs are enabled, false otherwise.
     */
    @JvmStatic
    fun areKarereLogsEnabled(): Boolean {
        return legacyLogUtil.areKarereLogsEnabled()
    }

    /**
     * Updates Karere logs preference.
     *
     * @param enabled True if should enable Karere logs, false otherwise.
     */
    @JvmStatic
    fun updateKarereLogs(enabled: Boolean) {
        legacyLogUtil.updateKarereLogs(enabled)
    }
}