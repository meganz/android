package mega.privacy.android.app.middlelayer.crashreporter

/**
 * When uncaught exception occurs, upload related info to platform tools. For example, Firebase Crashlytics. 
 */
interface CrashReporter {

    /**
     * Report the cause exception to corresponding platform.
     */
    fun report(e: Throwable)
}