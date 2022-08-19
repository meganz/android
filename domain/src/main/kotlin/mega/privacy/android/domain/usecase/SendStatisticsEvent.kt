package mega.privacy.android.domain.usecase

/**
 * Send a MEGA Stats event
 */
fun interface SendStatisticsEvent {

    /**
     * Invoke the use case
     *
     * @param eventID
     * @param message
     */
    suspend operator fun invoke(eventID: Int, message: String)
}