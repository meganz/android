package mega.privacy.android.analytics.event

import mega.privacy.android.analytics.event.menu.MenuType

/**
 * Menu item info
 */
interface MenuItemInfo : AnalyticsInfo {
    /**
     * Screen
     */
    val screen: ScreenInfo?

    /**
     * Menu type
     */
    val menuType: MenuType

    /**
     * Menu item name
     */
    val menuItemName: String
}