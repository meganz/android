package mega.privacy.android.domain.usecase

/**
 * Send Statistics event for Media Discovery
 */
fun interface SendStatisticsMediaDiscovery {
    /**
     * Invoke the use case
     *
     **/
    suspend operator fun invoke(mediaHandle: Long)
}