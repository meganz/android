package mega.privacy.android.domain.entity.analytics

import mega.privacy.android.domain.entity.analytics.identifier.NotificationEventIdentifier

/**
 * Notification event
 *
 * @property identifier
 * @property viewId
 * @constructor Create empty Notification event
 */
data class NotificationEvent(
    private val identifier: NotificationEventIdentifier,
) : AnalyticsEvent() {
    override val viewId: String? = null
    override val eventTypeIdentifier = 6000
    override val uniqueEventIdentifier: Int
        get() = identifier.uniqueIdentifier

    override fun data(): Map<String, Any?> {
        return mapOf(
            "notification_name" to identifier.name
        )
    }
}