package mega.privacy.android.analytics.event

/**
 * Tab selected
 */
interface TabInfo {
    /**
     * ScreenView
     */
    val screenInfo: ScreenInfo

    /**
     * Name
     */
    val name: String

    /**
     * Unique identifier
     */
    val uniqueIdentifier: Int
}