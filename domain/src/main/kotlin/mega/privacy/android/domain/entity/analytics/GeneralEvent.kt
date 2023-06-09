package mega.privacy.android.domain.entity.analytics

import mega.privacy.android.domain.entity.analytics.identifier.GeneralEventIdentifier

/**
 * General event
 *
 * @property identifier
 * @property viewId
 * @constructor Create empty General event
 */
data class GeneralEvent(
    private val identifier: GeneralEventIdentifier,
    override val viewId: String?,
) : AnalyticsEvent() {
    override val eventTypeIdentifier = 7000
    override val uniqueEventIdentifier: Int
        get() = identifier.uniqueIdentifier

    override fun data(): Map<String, Any?> {
        return mapOf(
            "name" to identifier.name,
            "info" to identifier.info,
        )
    }
}