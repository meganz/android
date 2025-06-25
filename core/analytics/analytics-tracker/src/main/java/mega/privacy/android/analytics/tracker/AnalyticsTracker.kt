package mega.privacy.android.analytics.tracker

import mega.privacy.mobile.analytics.core.event.identifier.EventIdentifier


/**
 * Analytics tracker
 */
interface AnalyticsTracker {
    /**
     * Track event
     *
     * @param eventIdentifier
     */
    fun trackEvent(eventIdentifier: EventIdentifier)
}