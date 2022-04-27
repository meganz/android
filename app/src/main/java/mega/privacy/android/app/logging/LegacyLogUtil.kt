package mega.privacy.android.app.logging

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import mega.privacy.android.app.AndroidChatLogger
import mega.privacy.android.app.AndroidLogger
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Util.*
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid

const val LOG_PREFERENCES = "LOG_PREFERENCES"
const val SDK_LOGS = "SDK_LOGS"
const val KARERE_LOGS = "KARERE_LOGS"

class LegacyLogUtil() : LegacyLoggingSettings, LegacyLog {

    private var loggerSDK: AndroidLogger? = null
    private var loggerKarere: AndroidChatLogger? = null

    /**
     * Gets the status of the SDK logger.
     *
     * @return True if enabled or false if disabled.
     */
    var statusLoggerSdk = false
        private set

    /**
     * Gets the status of the Karere logger.
     *
     * @return True if enabled or false if disabled.
     */
    var statusLoggerKarere = false
        private set

    /**
     * Send a log message with FATAL level to the logging system.
     *
     * @param message Message for the logging system.
     */
    override fun logFatal(message: String) {
        log(MegaApiAndroid.LOG_LEVEL_FATAL, message)
    }

    /**
     * Send a log message with FATAL level to the logging system.
     *
     * @param message   Message for the logging system.
     * @param exception Exception which produced the error.
     */
    override fun logFatal(message: String, exception: Throwable) {
        log(MegaApiAndroid.LOG_LEVEL_FATAL, message, exception, true)
    }

    /**
     * Send a log message with ERROR level to the logging system.
     *
     * @param message Message for the logging system.
     */
    override fun logError(message: String?) {
        log(MegaApiAndroid.LOG_LEVEL_ERROR, message)
    }

    /**
     * Send a log message with ERROR level to the logging system.
     *
     * @param message   Message for the logging system.
     * @param exception Exception which produced the error.
     */
    override fun logError(message: String?, exception: Throwable?) {
        log(MegaApiAndroid.LOG_LEVEL_ERROR, message, exception, false)
    }

    /**
     * Send a log message with WARNING level to the logging system.
     *
     * @param message Message for the logging system.
     */
    override fun logWarning(message: String) {
        log(MegaApiAndroid.LOG_LEVEL_WARNING, message)
    }

    /**
     * Send a log message with WARNING level to the logging system.
     *
     * @param message   Message for the logging system.
     * @param exception Exception which produced the warning.
     */
    override fun logWarning(message: String, exception: Throwable) {
        log(MegaApiAndroid.LOG_LEVEL_WARNING, message, exception, false)
    }

    /**
     * Send a log message with INFO level to the logging system.
     *
     * @param message Message for the logging system.
     */
    override fun logInfo(message: String) {
        log(MegaApiAndroid.LOG_LEVEL_INFO, message)
    }

    /**
     * Send a log message with DEBUG level to the logging system.
     *
     * @param message Message for the logging system.
     */
    override fun logDebug(message: String) {
        log(MegaApiAndroid.LOG_LEVEL_DEBUG, message)
    }

    /**
     * Send a log message with MAX level to the logging system.
     *
     * @param message Message for the logging system.
     */
    override fun logMax(message: String) {
        log(MegaApiAndroid.LOG_LEVEL_MAX, message)
    }

    /**
     * Send a log message to the logging system.
     *
     * @param logLevel Log level for this message.
     * @param message  Message for the logging system.
     */
    private fun log(logLevel: Int, message: String?) {
        if (!DEBUG && !statusLoggerSdk && !statusLoggerKarere) {
            return
        }
        val STACK_TRACE_LEVELS_UP = 4
        val stackTrace = Thread.currentThread().stackTrace[STACK_TRACE_LEVELS_UP]
        val fileName = stackTrace.fileName
        val methodName = stackTrace.methodName
        val line = stackTrace.lineNumber
        MegaApiAndroid.log(
            logLevel, "[clientApp]: " + message +
                    " (" + fileName + "::" + methodName + ":" + line + ")"
        )
    }


    /**
     * Send a log message to the logging system.
     *
     * @param logLevel        Log level for this message.
     * @param message         Message for the logging system.
     * @param exception       Exception which produced the error or warning.
     * @param printStackTrace Flag to print the stack trace of the exception.
     */
    private fun log(
        logLevel: Int,
        message: String?,
        exception: Throwable?,
        printStackTrace: Boolean
    ) {
        if (!DEBUG && !statusLoggerSdk && !statusLoggerKarere) {
            return
        }
        val STACK_TRACE_LEVELS_UP = 4
        val stackTrace = Thread.currentThread().stackTrace[STACK_TRACE_LEVELS_UP]
        val fileName = stackTrace.fileName
        val methodName = stackTrace.methodName
        val line = stackTrace.lineNumber
        if (printStackTrace) {
            MegaApiAndroid.log(
                logLevel,
                "[clientApp]: $message ($fileName::$methodName:$line)${System.lineSeparator()}${
                    Log.getStackTraceString(exception)
                }"
            )
        } else {
            MegaApiAndroid.log(
                logLevel,
                "[clientApp]: $message ($fileName::$methodName:$line)${System.lineSeparator()}[$exception]"
            )
        }

    }

    /**
     * Enables or disables the SDK logs depending on the "enabled" parameter.
     *
     * @param context Context from where the logs are being to be enabled/disabled.
     * @param enabled True to enable logs or false to disable,
     */
    override fun setStatusLoggerSDK(context: Context, enabled: Boolean) {
        if (!enabled) {
            logInfo("SDK logs are now disabled - App Version: " + getVersion())
        }
        updateSDKLogs(enabled)
        statusLoggerSdk = enabled
        if (enabled) {
            MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX)
            logInfo("SDK logs are now enabled - App Version: " + getVersion())
            showSnackbar(context, context.getString(R.string.settings_enable_logs))
        } else {
            showSnackbar(context, context.getString(R.string.settings_disable_logs))
            MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_FATAL)
        }

    }

    /**
     * Enables or disables the Karere logs depending on the "enabled" parameter.
     *
     * @param context Context from where the logs are being to be enabled/disabled.
     * @param enabled True to enable logs or false to disable,
     */
    override fun setStatusLoggerKarere(context: Context, enabled: Boolean) {
        if (!enabled) {
            logInfo("Karere logs are now disabled - App Version: " + getVersion())
        }
        updateKarereLogs(enabled)
        statusLoggerKarere = enabled
        if (enabled) {
            MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX)
            logInfo("Karere logs are now enabled - App Version: " + getVersion())
            showSnackbar(context, context.getString(R.string.settings_enable_logs))
        } else {
            showSnackbar(context, context.getString(R.string.settings_disable_logs))
            MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_ERROR)
        }
    }

    /**
     * Init the SDK logger.
     */
    fun initLoggerSDK() {
        if (loggerSDK == null) {
            loggerSDK = AndroidLogger(AndroidLogger.LOG_FILE_NAME, this)
        }
        statusLoggerSdk = areSDKLogsEnabled()
        MegaApiAndroid.addLoggerObject(loggerSDK)
        MegaApiAndroid.setLogLevel(if (DEBUG || statusLoggerSdk) MegaApiAndroid.LOG_LEVEL_MAX else MegaApiAndroid.LOG_LEVEL_FATAL)
        logInfo("SDK logger initialized")
    }

    /**
     * Reset the current SDK logger.
     */
    override fun resetLoggerSDK() {
        logInfo("Resetting SDK logger...")
        if (loggerSDK != null) {
            MegaApiAndroid.removeLoggerObject(loggerSDK)
        }
        initLoggerSDK()
    }

    /**
     * Init the Karere logger.
     */
    fun initLoggerKarere() {
        if (loggerKarere == null) {
            loggerKarere = AndroidChatLogger(AndroidChatLogger.LOG_FILE_NAME, this)
        }
        statusLoggerKarere = areKarereLogsEnabled()
        MegaChatApiAndroid.setLoggerObject(loggerKarere)
        MegaChatApiAndroid.setLogLevel(if (DEBUG || statusLoggerKarere) MegaChatApiAndroid.LOG_LEVEL_MAX else MegaChatApiAndroid.LOG_LEVEL_ERROR)
        logInfo("Karere logger initialized")
    }

    /**
     * Checks if loggerSDK is initialized.
     *
     * @return True if loggerSDK is initialized, false otherwise.
     */
    val isLoggerSDKInitialized: Boolean
        get() = loggerSDK != null

    /**
     * Checks if loggerKarere is initialized.
     *
     * @return True if loggerKarere is initialized, false otherwise.
     */
    val isLoggerKarereInitialized: Boolean
        get() = loggerKarere != null

    /**
     * Gets Log SharedPreferences.
     *
     * @return Log SharedPreferences
     */
    private val logSharedPreferences: SharedPreferences
        get() = MegaApplication.getInstance()
            .getSharedPreferences(LOG_PREFERENCES, Context.MODE_PRIVATE)

    /**
     * Checks if SDK logs are enabled.
     *
     * @return True if SDK logs are enabled, false otherwise.
     */
    override fun areSDKLogsEnabled(): Boolean {
        return logSharedPreferences.getBoolean(SDK_LOGS, false)
    }

    /**
     * Updates SDK logs preference.
     *
     * @param enabled True if should enable SDK logs, false otherwise.
     */
    override fun updateSDKLogs(enabled: Boolean) {
        logSharedPreferences.edit().putBoolean(SDK_LOGS, enabled).apply()
    }

    /**
     * Checks if SDK logs are enabled.
     *
     * @return True if SDK logs are enabled, false otherwise.
     */
    override fun areKarereLogsEnabled(): Boolean {
        return logSharedPreferences.getBoolean(KARERE_LOGS, false)
    }

    /**
     * Updates Karere logs preference.
     *
     * @param enabled True if should enable Karere logs, false otherwise.
     */
    override fun updateKarereLogs(enabled: Boolean) {
        logSharedPreferences.edit().putBoolean(KARERE_LOGS, enabled).apply()
    }
}