package mega.privacy.android.analytics.event

/**
 * Screen view
 *
 * @constructor Create empty Screen view
 */
interface ScreenInfo : AnalyticsInfo {
    /**
     * Name
     */
    val name: String

    /**
     * Unique identifier
     */
    override val uniqueIdentifier: Int

}