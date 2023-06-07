package mega.privacy.android.analytics.event.navigation

/**
 * Navigation event source
 *
 * @constructor Create empty Navigation event source
 */
enum class NavigationEventSource {
    /**
     * Bottom
     */
    Bottom,

    /**
     * Drawer
     */
    Drawer,

    /**
     * Toolbar
     */
    Toolbar,

    /**
     * System
     */
    System,

    /**
     * Other
     */
    Other
}