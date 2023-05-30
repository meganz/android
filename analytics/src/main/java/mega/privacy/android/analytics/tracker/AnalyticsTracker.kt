package mega.privacy.android.analytics.tracker

import mega.privacy.android.analytics.event.ScreenView
import mega.privacy.android.analytics.event.TabSelected

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

    /**
     * Track tab selected
     *
     * @param tab
     */
    fun trackTabSelected(tab: TabSelected)
}
