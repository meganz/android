package mega.privacy.android.domain.entity.analytics

/**
 * Analytics event
 *
 * @constructor Create empty Analytics event
 */
sealed class AnalyticsEvent {

    private val platformEventIdentifier = 300_000

    /**
     * Event type identifier
     */
    protected abstract val eventTypeIdentifier: Int

    /**
     * Unique event identifier
     */
    protected abstract val uniqueEventIdentifier: Int

    /**
     * Get event identifier
     *
     */
    fun getEventIdentifier() = platformEventIdentifier + eventTypeIdentifier + uniqueEventIdentifier

    /**
     * View id
     */
    abstract val viewId: String?

    /**
     * Data
     *
     * @return
     */
    abstract fun data(): Map<String, Any?>
}