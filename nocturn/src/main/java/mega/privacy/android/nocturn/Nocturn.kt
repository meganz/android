package mega.privacy.android.nocturn

/**
 * Nocturn is a debugging tool to monitor ANR event.
 * If the main thread is blocked for N duration, it will raise a notification with related stack trace details.
 *
 * The purpose of this tool is to raise awareness for developer to improve performance of app.
 */
fun interface Nocturn {
    /**
     * Monitor main thread and raise notification if [waitTimeout] reached.
     */
    fun monitor(waitTimeout: Long)
}
