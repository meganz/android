package mega.privacy.android.feature.sync.domain.entity

/**
 * This class encapsulates the type of sync notification
 */
enum class SyncNotificationType {
    /**
     * Notification for a stalled issue
     */
    STALLED_ISSUE,

    /**
     * Notification for an SDK error
     */
    ERROR,

    /**
     * Notification for when device is low on battery and is not charging
     */
    BATTERY_LOW,

    /**
     * Notification for when device is not connected to WiFi
     * but the sync preference is set to only happen when connected to WiFi
     */
    NOT_CONNECTED_TO_WIFI
}