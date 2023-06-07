package mega.privacy.android.domain.entity.analytics

import mega.privacy.android.domain.entity.analytics.identifier.MenuItemEventIdentifier


/**
 * Menu item event
 *
 * @property identifier
 * @property viewId
 * @constructor Create empty Menu item event
 */
class MenuItemEvent(
    private val identifier: MenuItemEventIdentifier,
    override val viewId: String?,
) : AnalyticsEvent() {
    override val eventTypeIdentifier = 5000
    override val uniqueEventIdentifier: Int
        get() = identifier.uniqueIdentifier

    override fun data(): Map<String, Any?> {
        return mapOf(
            "screen_name" to identifier.screenName,
            "menu_item" to identifier.menuItem,
            "menu_type" to identifier.menuType,
        )
    }
}