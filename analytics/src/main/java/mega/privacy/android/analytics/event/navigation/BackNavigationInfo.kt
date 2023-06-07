package mega.privacy.android.analytics.event.navigation

import mega.privacy.android.analytics.event.NavigationInfo

/**
 * BackNavigationInfo
 */
object BackNavigationInfo : NavigationInfo {
    /**
     * Source
     */
    override val source = NavigationEventSource.System

    /**
     * Destination
     */
    override val destination = "back"

    /**
     * Unique identifier
     */
    override val uniqueIdentifier = 0
}