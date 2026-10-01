package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.analytics.FirebaseAnalyticsEvent

/**
 * Repository for Firebase Analytics
 *
 * Used for events consumed by Firebase A/B Testing experiments; regular product analytics
 * should keep using the MEGA analytics tracker.
 */
interface FirebaseAnalyticsRepository {

    /**
     * Log an event to Firebase Analytics
     *
     * @param event Event to log
     */
    fun logEvent(event: FirebaseAnalyticsEvent)
}
