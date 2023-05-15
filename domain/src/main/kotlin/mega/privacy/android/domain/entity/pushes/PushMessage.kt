package mega.privacy.android.domain.entity.pushes

/**
 * Push message
 *
 * @constructor Create empty Push message
 */
sealed class PushMessage {

    /**
     * Call push message
     *
     * @constructor Create empty Call push message
     */
    object CallPushMessage : PushMessage()

    /**
     * Chat push message
     *
     * @property shouldBeep
     * @constructor Create empty Chat push message
     */
    data class ChatPushMessage(
        val shouldBeep: Boolean,
    ) : PushMessage()

    /**
     * Scheduled meeting push message
     *
     * @property schedId
     * @property userHandle
     * @property chatRoomHandle
     * @property title
     * @property description
     * @property startTimestamp
     * @property endTimestamp
     * @property timezone
     * @property isStartReminder
     * @constructor Create empty Scheduled meeting push message
     */
    data class ScheduledMeetingPushMessage(
        val schedId: Long,
        val userHandle: Long,
        val chatRoomHandle: Long,
        val title: String?,
        val description: String?,
        val startTimestamp: Long,
        val endTimestamp: Long,
        val timezone: String?,
        val isStartReminder: Boolean,
    ) : PushMessage()
}
