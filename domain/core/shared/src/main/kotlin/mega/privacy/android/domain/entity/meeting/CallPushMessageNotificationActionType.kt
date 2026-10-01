package mega.privacy.android.domain.entity.meeting

/**
 * Call Push Message Notification Action Types
 */
enum class CallPushMessageNotificationActionType {
    /**
     *  Show notification
     */
    Show,

    /**
     * Update notification
     */
    Update,

    /**
     * Hide notification
     */
    Hide,

    /**
     * Remove notification
     */
    Remove,

    /**
     * Missed notification
     */
    Missed
}