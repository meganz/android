package mega.privacy.android.domain.monitoring
/**
 * When uncaught exception occurs, upload related info to platform tools. For example, Firebase Crashlytics.
 */
interface CrashReporter {

    /**
     * Report the cause exception to corresponding platform.
     */
    fun report(e: Throwable)

    /**
     * Set if allow to collect and upload crash info.
     * Controlled by analytics cookie.
     *
     * @param enabled true, if allowed, false otherwise.
     */
    fun setEnabled(enabled: Boolean)

    /**
     * Add custom log using BreadCrumbs of Firebase Crashlytics
     * @param message log message
     */
    fun log(message: String)
}
