package mega.privacy.android.analytics.event

/**
 * Notification info
 *
 * @constructor Create empty Notification info
 */
interface NotificationInfo : AnalyticsInfo {
    /**
     * Notification name
     */
    val notificationName: String
}