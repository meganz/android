package mega.privacy.android.domain.repository

/**
 * The MEGA Stats Repository
 */
interface StatisticsRepository {

    /**
     * Send an event to MEGA stats
     */
    suspend fun sendEvent(eventID: Int, message: String)
}
