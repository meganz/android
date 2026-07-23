package mega.privacy.android.data.gateway

/**
 * Gateway for Firebase Analytics
 */
internal interface FirebaseAnalyticsGateway {

    /**
     * Log an event to Firebase Analytics
     *
     * @param eventName Name of the event
     */
    fun logEvent(eventName: String)
}
