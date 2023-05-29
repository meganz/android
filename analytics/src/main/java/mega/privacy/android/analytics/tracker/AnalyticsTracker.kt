package mega.privacy.android.analytics.tracker

import mega.privacy.android.analytics.event.ScreenView

/**
 * Analytics tracker
 */
interface AnalyticsTracker {
    /**
     * Track screen view
     *
     * @param screen
     */
    fun trackScreenView(screen: ScreenView)
}
