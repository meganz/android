package mega.privacy.android.analytics.event

/**
 * Tab selected
 */
interface TabSelected {
    /**
     * ScreenView
     */
    val screenView: ScreenView

    /**
     * Name
     */
    val name: String

    /**
     * Unique identifier
     */
    val uniqueIdentifier: Int
}