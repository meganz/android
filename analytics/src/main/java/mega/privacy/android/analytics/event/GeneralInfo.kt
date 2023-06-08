package mega.privacy.android.analytics.event

/**
 * General info
 */
interface GeneralInfo : AnalyticsInfo {
    /**
     * Name
     */
    val name: String

    /**
     * Info
     */
    val info: String?
}