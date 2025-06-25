package mega.privacy.android.analytics.tracker

import mega.privacy.mobile.analytics.core.event.identifier.EventIdentifier
import mega.privacy.mobile.analytics.event.tracking.Tracker
import javax.inject.Inject

/**
 * Analytics tracker impl
 *
 * @property tracker
 */
class AnalyticsTrackerImpl @Inject constructor(
    private val tracker: Tracker,
) : AnalyticsTracker {
    override fun trackEvent(eventIdentifier: EventIdentifier) {
        tracker.trackEvent(eventIdentifier)
    }
}