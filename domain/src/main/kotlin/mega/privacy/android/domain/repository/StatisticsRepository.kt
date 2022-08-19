package mega.privacy.android.domain.repository

/**
 * The MEGA Stats Repository
 */
interface StatisticsRepository {

    /**
     * Send an event to MEGA stats
     */
    suspend fun sendEvent(eventID: Int, message: String)

    /**
     * Get the media discovery click count
     */
    suspend fun getMediaDiscoveryClickCount(): Int

    /**
     * Set the media discovery click count
     *
     * @param clickCount
     */
    suspend fun setMediaDiscoveryClickCount(clickCount: Int)

    /**
     * Get the media discovery folder click count
     */
    suspend fun getMediaDiscoveryClickCountFolder(mediaHandle: Long): Int

    /**
     * Set the media discovery folder click count
     */
    suspend fun setMediaDiscoveryClickCountFolder(clickCountFolder: Int, mediaHandle: Long)
}
