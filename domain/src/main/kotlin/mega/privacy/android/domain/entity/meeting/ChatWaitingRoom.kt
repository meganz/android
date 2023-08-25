package mega.privacy.android.domain.entity.meeting

/**
 * Waiting room item.
 *
 * @property peers              List of peers in the waiting room.
 * @property size               Size of waiting room.
 * @property peerStatus         Map of peer status by their handles.
 */
data class ChatWaitingRoom(
    val peers: List<Long>? = emptyList(),
    val size: Long,
    val peerStatus: Map<Long, WaitingRoomStatus>,
)
