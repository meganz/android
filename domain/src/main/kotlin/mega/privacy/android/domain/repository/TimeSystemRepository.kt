package mega.privacy.android.domain.repository

/**
 * Time system repository
 *
 */
interface TimeSystemRepository {
    /**
     * Get current time in millis
     *
     */
    fun getCurrentTimeInMillis(): Long


    /**
     * Get current hour of day
     *
     * @return hour of day
     */
    fun getCurrentHourOfDay(): Int


    /**
     * Get current minute
     *
     * @return minute
     */
    fun getCurrentMinute(): Int
}