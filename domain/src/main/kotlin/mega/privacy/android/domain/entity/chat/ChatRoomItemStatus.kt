package mega.privacy.android.domain.entity.chat

/**
 * Chat Room Item Status
 *
 * @property callStartTimestamp     Call start timestamp
 */
sealed class ChatRoomItemStatus constructor(
    val callStartTimestamp: Long?,
) {

    /**
     *  Call not started
     */
    object NotStarted : ChatRoomItemStatus(null)

    /**
     * Call in progress and I am not participating
     *
     * @property callStartTimestamp     Call start timestamp
     */
    class NotJoined(callStartTimestamp: Long?) : ChatRoomItemStatus(callStartTimestamp)

    /**
     * Call in progress and I am participating
     *
     * @property callStartTimestamp     Call start timestamp
     */
    class Joined(callStartTimestamp: Long?) : ChatRoomItemStatus(callStartTimestamp)
}
