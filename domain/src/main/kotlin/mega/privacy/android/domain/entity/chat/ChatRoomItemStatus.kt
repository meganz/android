package mega.privacy.android.domain.entity.chat

import java.time.Duration
import java.time.Instant

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

    /**
     * Get call duration based on current Status
     *
     * @return  Call duration in seconds
     */
    fun getDuration(): Long? =
        callStartTimestamp?.let { timesTamp ->
            Duration.between(
                Instant.ofEpochSecond(timesTamp),
                Instant.now()
            ).seconds
        }
}
