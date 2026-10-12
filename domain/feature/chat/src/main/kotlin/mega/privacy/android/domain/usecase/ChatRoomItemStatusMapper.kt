package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ChatRoomItemStatus
import javax.inject.Inject

/**
 * Chat call item mapper
 */
class ChatRoomItemStatusMapper @Inject constructor() {

    /**
     * Map [ChatCall] to [ChatRoomItemStatus]
     *
     * @param chatCall  [ChatCall]
     * @return          [ChatRoomItemStatus]
     */
    operator fun invoke(chatCall: ChatCall): ChatRoomItemStatus =
        when (chatCall.status) {
            ChatCallStatus.Connecting,
            ChatCallStatus.Joining,
            ChatCallStatus.InProgress,
            -> ChatRoomItemStatus.Joined

            ChatCallStatus.UserNoPresent,
            ChatCallStatus.WaitingRoom,
            -> ChatRoomItemStatus.NotJoined

            else -> ChatRoomItemStatus.NotStarted
        }
}
