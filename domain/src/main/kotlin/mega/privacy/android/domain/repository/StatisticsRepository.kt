package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.analytics.AnalyticsEvent

/**
 * The MEGA Stats Repository
 */
interface StatisticsRepository {

    /**
     * Send an event to MEGA stats
     */
    @Deprecated(
        message = "This has been deprecated in favour of the below sendEvent",
        replaceWith = ReplaceWith("sendEvent(eventId, message, addJourneyId, viewId)")
    )
    suspend fun sendEvent(eventID: Int, message: String)

    /**
     * Send an event to the stats server.
     *
     * @param eventId      Event type
     * @param message      Event message. If the message contains quotes, they must be escaped quotes.
     * @param addJourneyId True if JourneyID should be included. Otherwise, false.
     * @param viewId       ViewID value (C-string null-terminated) to be sent with the event.
     *                     This value should have been generated with [this.generateViewId].
     */
    suspend fun sendEvent(
        eventId: Int,
        message: String,
        addJourneyId: Boolean,
        viewId: String?,
    )

    /**
     * Generate an unique ViewID
     * A ViewID consists of a random generated id, encoded in hexadecimal as 16 characters of a
     * null-terminated string.
     *
     * @return the ViewId.
     */
    suspend fun generateViewId(): String

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

    /**
     * Log event
     *
     * @param event
     */
    suspend fun logEvent(event: AnalyticsEvent)
}
