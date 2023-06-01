package mega.privacy.android.domain.entity.analytics

/**
 * Dialog displayed event
 *
 * @property identifier
 * @property viewId
 * @constructor Create empty Dialog displayed event
 */
class DialogDisplayedEvent(
    private val identifier: DialogDisplayedEventIdentifier,
    override val viewId: String?,
) : AnalyticsEvent() {
    override val eventTypeIdentifier = 3000
    override val uniqueEventIdentifier: Int
        get() = identifier.uniqueIdentifier

    override fun data(): Map<String, Any?> {
        return mapOf(
            "screen_name" to identifier.screenName,
            "dialog_name" to identifier.dialogName
        )
    }
}