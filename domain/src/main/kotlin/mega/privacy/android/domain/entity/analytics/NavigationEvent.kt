package mega.privacy.android.domain.entity.analytics

import mega.privacy.android.domain.entity.analytics.identifier.NavigationEventIdentifier


/**
 * Navigation event
 *
 * @property identifier
 * @property viewId
 * @constructor Create empty Navigation event
 */
class NavigationEvent(
    private val identifier: NavigationEventIdentifier,
    override val viewId: String?,
) : AnalyticsEvent() {
    override val eventTypeIdentifier = 4000
    override val uniqueEventIdentifier: Int
        get() = identifier.uniqueIdentifier

    override fun data(): Map<String, Any?> {
        return mapOf(
            "navigation_element_type" to identifier.navigationElementType,
            "destination" to identifier.destination,
        )
    }
}