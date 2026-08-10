package mega.privacy.android.domain.entity.chat

/**
 * Mute state of the notifications of a chat room.
 *
 * @property isMuted             True if the chat notifications are muted, false otherwise.
 * @property mutedUntilTimestamp Timestamp (in seconds since the Epoch) until which the
 *                               notifications are muted, or null when they are not muted or
 *                               muted until turned back on.
 */
data class ChatNotificationMuteState(
    val isMuted: Boolean,
    val mutedUntilTimestamp: Long?,
)
