package mega.privacy.android.app.utils;

import android.content.Context;
import android.util.Log;

import mega.privacy.android.app.AndroidChatLogger;
import mega.privacy.android.app.AndroidLogger;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.Util.*;

public class LogUtil {

    private static MegaApplication app = MegaApplication.getInstance();

    private static AndroidLogger loggerSDK = null;
    private static AndroidChatLogger loggerKarere = null;

    private static boolean statusLoggerSDK = false;
    private static boolean statusLoggerKarere = false;

    //Indicates if app is requesting the required permissions to enable the SDK logger
    private static boolean permissionLoggerSDK = false;
    //Indicates if app is requesting the required permissions to enable the Karere logger
    private static boolean permissionLoggerKarere = false;

    /**
     * Send a log message with FATAL level to the logging system.
     *
     * @param message Message for the logging system.
     */
    public static void logFatal(String message) {
        log(MegaApiAndroid.LOG_LEVEL_FATAL, message);
    }

    /**
     * Send a log message with FATAL level to the logging system.
     *
     * @param message Message for the logging system.
     * @param exception Exception which produced the error.
     */
    public static void logFatal(String message, Throwable exception) {
        log(MegaApiAndroid.LOG_LEVEL_FATAL, message, exception, true);
    }

    /**
     * Send a log message with ERROR level to the logging system.
     *
     * @param message Message for the logging system.
     */
    public static void logError(String message) {
        log(MegaApiAndroid.LOG_LEVEL_ERROR, message);
    }

    /**
     * Send a log message with ERROR level to the logging system.
     *
     * @param message Message for the logging system.
     * @param exception Exception which produced the error.
     */
    public static void logError(String message, Throwable exception) {
        log(MegaApiAndroid.LOG_LEVEL_ERROR, message, exception, false);
    }

    /**
     * Send a log message with WARNING level to the logging system.
     *
     * @param message Message for the logging system.
     */
    public static void logWarning(String message) {
        log(MegaApiAndroid.LOG_LEVEL_WARNING, message);
    }

    /**
     * Send a log message with WARNING level to the logging system.
     *
     * @param message Message for the logging system.
     * @param exception Exception which produced the warning.
     */
    public static void logWarning(String message, Throwable exception) {
        log(MegaApiAndroid.LOG_LEVEL_WARNING, message, exception, false);
    }

    /**
     * Send a log message with INFO level to the logging system.
     *
     * @param message Message for the logging system.
     */
    public static void logInfo(String message) {
        log(MegaApiAndroid.LOG_LEVEL_INFO, message);
    }

    /**
     * Send a log message with DEBUG level to the logging system.
     *
     * @param message Message for the logging system.
     */
    public static void logDebug(String message) {
        log(MegaApiAndroid.LOG_LEVEL_DEBUG, message);
    }

    /**
     * Send a log message with MAX level to the logging system.
     *
     * @param message Message for the logging system.
     */
    public static void logMax(String message) {
        log(MegaApiAndroid.LOG_LEVEL_MAX, message);
    }

    /**
     * Send a log message to the logging system.
     *
     * @param logLevel Log level for this message.
     * @param message  Message for the logging system.
     */
    private static void log(int logLevel, String message) {
        if (!Util.DEBUG && !statusLoggerSDK && !statusLoggerKarere) {
            return;
        }

        final int STACK_TRACE_LEVELS_UP = 4;
        final StackTraceElement stackTrace = Thread.currentThread().getStackTrace()[STACK_TRACE_LEVELS_UP];

        String fileName = stackTrace.getFileName();
        String methodName = stackTrace.getMethodName();
        int line = stackTrace.getLineNumber();

        MegaApiAndroid.log(logLevel, "[clientApp]: " + message +
                " (" + fileName + "::" + methodName + ":" + line + ")");
    }

    /**
     * Send a log message to the logging system.
     *
     * @param logLevel Log level for this message.
     * @param message  Message for the logging system.
     * @param exception Exception which produced the error or warning.
     * @param printStackTrace Flag to print the stack trace of the exception.
     */
    private static void log(int logLevel, String message, Throwable exception, boolean printStackTrace) {
        if (!Util.DEBUG && !statusLoggerSDK && !statusLoggerKarere) {
            return;
        }

        final int STACK_TRACE_LEVELS_UP = 4;
        final StackTraceElement stackTrace = Thread.currentThread().getStackTrace()[STACK_TRACE_LEVELS_UP];

        String fileName = stackTrace.getFileName();
        String methodName = stackTrace.getMethodName();
        int line = stackTrace.getLineNumber();

        if (printStackTrace) {
            MegaApiAndroid.log(logLevel, "[clientApp]: " + message +
                    " (" + fileName + "::" + methodName + ":" + line + ")" +
                    System.lineSeparator() + Log.getStackTraceString(exception));
        } else {
            MegaApiAndroid.log(logLevel, "[clientApp]: " + message +
                    " (" + fileName + "::" + methodName + ":" + line + ")" +
                    System.lineSeparator() + "[" + exception.toString() + "]");
        }
    }

    /**
     * Enables or disables the SDK logs depending on the "enabled" parameter.
     * @param context Context from where the logs are being to be enabled/disabled.
     * @param enabled True to enable logs or false to disable,
     */
    public static void setStatusLoggerSDK(Context context, boolean enabled) {
        if (!enabled) {
            logInfo("SDK logs are now disabled - App Version: " + getVersion());
        }

        app.getDbH().setFileLoggerSDK(enabled);
        statusLoggerSDK = enabled;
        if (enabled) {
            MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX);
            logInfo("SDK logs are now enabled - App Version: " + getVersion());
            showSnackbar(context, context.getString(R.string.settings_enable_logs));
        } else {
            showSnackbar(context, context.getString(R.string.settings_disable_logs));
            MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_FATAL);
        }
    }

    /**
     * Gets the status of the SDK logger.
     * @return True if enabled or false if disabled.
     */
    public static boolean getStatusLoggerSDK(){
        return statusLoggerSDK;
    }

    /**
     * Enables or disables the Karere logs depending on the "enabled" parameter.
     * @param context Context from where the logs are being to be enabled/disabled.
     * @param enabled True to enable logs or false to disable,
     */
    public static void setStatusLoggerKarere(Context context, boolean enabled) {
        if (!enabled) {
            logInfo("Karere logs are now disabled - App Version: " + getVersion());
        }

        app.getDbH().setFileLoggerKarere(enabled);
        statusLoggerKarere = enabled;
        if (enabled) {
            MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX);
            logInfo("Karere logs are now enabled - App Version: " + getVersion());
            showSnackbar(context, context.getString(R.string.settings_enable_logs));
        } else {
            showSnackbar(context, context.getString(R.string.settings_disable_logs));
            MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_ERROR);
        }
    }

    /**
     * Gets the status of the Karere logger.
     * @return True if enabled or false if disabled.
     */
    public static boolean getStatusLoggerKarere(){
        return statusLoggerKarere;
    }

    /**
     * Init the SDK logger.
     */
    public static void initLoggerSDK() {
        if (loggerSDK == null) {
            loggerSDK = new AndroidLogger(AndroidLogger.LOG_FILE_NAME);
        }

        DatabaseHandler dbH = app.getDbH();
        if (dbH != null) {
            MegaAttributes attrs = dbH.getAttributes();
            if (attrs != null && attrs.getFileLoggerSDK() != null) {
                statusLoggerSDK = Boolean.parseBoolean(attrs.getFileLoggerSDK());
            }
        }

        MegaApiAndroid.addLoggerObject(loggerSDK);
        MegaApiAndroid.setLogLevel(DEBUG || statusLoggerSDK ? MegaApiAndroid.LOG_LEVEL_MAX : MegaApiAndroid.LOG_LEVEL_FATAL);
        logInfo("SDK logger initialized");
    }

    /**
     * Reset the current SDK logger.
     */
    public static void resetLoggerSDK() {
        logInfo("Resetting SDK logger...");
        if (loggerSDK != null) {
            MegaApiAndroid.removeLoggerObject(loggerSDK);
        }
        initLoggerSDK();
    }

    /**
     * Init the Karere logger.
     */
    public static void initLoggerKarere() {
        if (loggerKarere == null) {
            loggerKarere = new AndroidChatLogger(AndroidChatLogger.LOG_FILE_NAME);
        }

        DatabaseHandler dbH = app.getDbH();
        if (dbH != null) {
            MegaAttributes attrs = dbH.getAttributes();
            if (attrs != null && attrs.getFileLoggerKarere() != null) {
                statusLoggerKarere = Boolean.parseBoolean(attrs.getFileLoggerKarere());
            }
        }

        MegaChatApiAndroid.setLoggerObject(loggerKarere);
        MegaChatApiAndroid.setLogLevel(DEBUG || statusLoggerKarere ? MegaChatApiAndroid.LOG_LEVEL_MAX : MegaChatApiAndroid.LOG_LEVEL_ERROR);
        logInfo("Karere logger initialized");
    }
}
