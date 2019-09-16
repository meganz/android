package mega.privacy.android.app.utils;

import android.util.Log;

import nz.mega.sdk.MegaApiAndroid;

public class LogUtil {

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
        final int STACK_TRACE_LEVELS_UP = 4;

        String fileName = Thread.currentThread().getStackTrace()[STACK_TRACE_LEVELS_UP].getFileName();
        String methodName = Thread.currentThread().getStackTrace()[STACK_TRACE_LEVELS_UP].getMethodName();
        int line = Thread.currentThread().getStackTrace()[STACK_TRACE_LEVELS_UP].getLineNumber();

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
        final int STACK_TRACE_LEVELS_UP = 4;

        String fileName = Thread.currentThread().getStackTrace()[STACK_TRACE_LEVELS_UP].getFileName();
        String methodName = Thread.currentThread().getStackTrace()[STACK_TRACE_LEVELS_UP].getMethodName();
        int line = Thread.currentThread().getStackTrace()[STACK_TRACE_LEVELS_UP].getLineNumber();

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
}
