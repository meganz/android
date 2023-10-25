package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.data.mapper.handles.HandleListMapper
import mega.privacy.android.domain.entity.meeting.ChatWaitingRoom
import mega.privacy.android.domain.entity.meeting.WaitingRoomStatus
import nz.mega.sdk.MegaChatWaitingRoom
import javax.inject.Inject


/**
 * Mapper to convert [MegaChatWaitingRoom] to [ChatWaitingRoom]
 */
internal class ChatWaitingRoomMapper @Inject constructor(
    private val waitingRoomStatusMapper: WaitingRoomStatusMapper,
    private val handleListMapper: HandleListMapper,
) {

    operator fun invoke(megaChatWaitingRoom: MegaChatWaitingRoom?): ChatWaitingRoom? =
        megaChatWaitingRoom?.let {
            return ChatWaitingRoom(
                size = megaChatWaitingRoom.size(),
                peers = handleListMapper(megaChatWaitingRoom.users),
                peerStatus = megaChatWaitingRoom.toPeerStatusByHandles(),
            )
        }

    private fun MegaChatWaitingRoom.toPeerStatusByHandles(): Map<Long, WaitingRoomStatus> =
        (0 until users.size()).associateWith { peerHandle ->
            waitingRoomStatusMapper(
                getUserStatus(
                    peerHandle
                )
            )
        }
}