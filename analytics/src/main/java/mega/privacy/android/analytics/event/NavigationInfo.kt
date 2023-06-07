package mega.privacy.android.analytics.event

import mega.privacy.android.analytics.event.navigation.NavigationEventSource

/**
 * Navigation info
 */
interface NavigationInfo : AnalyticsInfo {
    /**
     * Source
     */
    val source: NavigationEventSource

    /**
     * Destination
     */
    val destination: String
}