package mega.privacy.android.analytics.event

/**
 * Dialog button info
 */
interface ButtonInfo : AnalyticsInfo {

    /**
     * Name
     */
    val name: String

    /**
     * Screen
     */
    val screen: ScreenInfo?

    /**
     * Dialog
     */
    val dialog: DialogInfo?

    /**
     * Unique identifier
     */
    override val uniqueIdentifier: Int
}