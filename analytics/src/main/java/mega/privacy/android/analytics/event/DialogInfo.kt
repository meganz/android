package mega.privacy.android.analytics.event

/**
 * Dialog display
 */
interface DialogInfo : AnalyticsInfo {
    /**
     * Name
     */
    val name: String

    /**
     * Unique identifier
     */
    override val uniqueIdentifier: Int
}