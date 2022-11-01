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
}