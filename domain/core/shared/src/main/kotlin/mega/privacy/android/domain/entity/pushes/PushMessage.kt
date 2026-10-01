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
     * @property chatId
     * @constructor Create empty Call push message
     */
    data class CallPushMessage(val chatId: Long) : PushMessage()

    /**
     * Chat push message
     *
     * @property shouldBeep
     * @property chatId
     * @property msgId
     * @constructor Create empty Chat push message
     */
    data class ChatPushMessage(
        val shouldBeep: Boolean,
        val chatId: Long,
        val msgId: Long,
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

    /**
     *  Promo notification push message
     *
     *  @property id
     *  @property title
     *  @property subtitle
     *  @property description
     *  @property redirectLink
     *  @property imagePath
     *  @property sound
     */
    data class PromoPushMessage(
        val id: Int,
        val title: String,
        val subtitle: String?,
        val description: String,
        val redirectLink: String,
        val imagePath: String?,
        val sound: String?,
    ) : PushMessage()
}
