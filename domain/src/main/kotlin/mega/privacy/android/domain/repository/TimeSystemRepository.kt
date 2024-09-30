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

    /**
     * Based on the device locale and other preferences, check if times should be
     * formatted as 24 hour times or 12 hour (AM/PM).
     *
     * @return true if 24 hour time format is selected, false otherwise.
     */
    fun is24HourFormat(): Boolean
}
