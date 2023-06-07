package mega.privacy.android.analytics.event

/**
 * Tab selected
 */
interface TabInfo : AnalyticsInfo {
    /**
     * ScreenView
     */
    val screenInfo: ScreenInfo

    /**
     * Name
     */
    val name: String
}