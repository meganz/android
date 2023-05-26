package mega.privacy.android.domain.entity.analytics

/**
 * Screen view event
 *
 * @property identifier
 * @property viewId
 * @constructor Create empty Screen view event
 */
data class ScreenViewEvent(
    private val identifier: ScreenViewEventIdentifier,
    override val viewId: String,
) : AnalyticsEvent() {
    override val eventTypeIdentifier = 0
    override val uniqueEventIdentifier: Int
        get() = identifier.uniqueIdentifier

    override fun data(): Map<String, Any?> {
        return mapOf(
            "screen_name" to identifier.name
        )
    }
}