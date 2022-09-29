package mega.privacy.android.app.utils.wrapper

/**
 * wrapper class for get time
 */
interface TimeWrapper {
    /**
     * get current time
     */
    val now: Long

    /**
     * get nano time
     */
    val nanoTime: Long
}
