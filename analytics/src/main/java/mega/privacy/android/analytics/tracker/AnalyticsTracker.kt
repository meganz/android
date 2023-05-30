package mega.privacy.android.analytics.tracker

import mega.privacy.android.analytics.event.ScreenInfo
import mega.privacy.android.analytics.event.TabInfo

/**
 * Analytics tracker
 */
interface AnalyticsTracker {
    /**
     * Track screen view
     *
     * @param screen
     */
    fun trackScreenView(screen: ScreenInfo)

    /**
     * Track tab selected
     *
     * @param tab
     */
    fun trackTabSelected(tab: TabInfo)
}
