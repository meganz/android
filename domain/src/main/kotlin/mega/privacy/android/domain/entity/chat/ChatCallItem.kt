package mega.privacy.android.domain.entity.chat

/**
 * Chat call item
 *
 * @property callStartTimestamp     Call start timestamp
 */
sealed class ChatCallItem constructor(
    val callStartTimestamp: Long?,
) {

    /**
     *  Call not started
     */
    object NotStarted : ChatCallItem(null)

    /**
     * Call in progress and I am not participating
     *
     * @property callStartTimestamp     Call start timestamp
     */
    class NotJoined(callStartTimestamp: Long?) : ChatCallItem(callStartTimestamp)

    /**
     * Call in progress and I am participating
     *
     * @property callStartTimestamp     Call start timestamp
     */
    class Joined(callStartTimestamp: Long?) : ChatCallItem(callStartTimestamp)
}
