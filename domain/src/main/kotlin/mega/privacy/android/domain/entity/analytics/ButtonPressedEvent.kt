package mega.privacy.android.domain.entity.analytics

import mega.privacy.android.domain.entity.analytics.identifier.ButtonPressedEventIdentifier


/**
 * Button pressed event
 *
 * @property identifier
 * @property viewId
 * @constructor Create empty Button pressed event
 */
class ButtonPressedEvent(
    private val identifier: ButtonPressedEventIdentifier,
    override val viewId: String?,
) : AnalyticsEvent() {
    override val eventTypeIdentifier = 2000
    override val uniqueEventIdentifier: Int
        get() = identifier.uniqueIdentifier

    override fun data(): Map<String, Any?> {
        return mapOf(
            "screen_name" to identifier.screenName,
            "dialog_name" to identifier.dialogName,
            "button_name" to identifier.buttonName,
        )
    }
}