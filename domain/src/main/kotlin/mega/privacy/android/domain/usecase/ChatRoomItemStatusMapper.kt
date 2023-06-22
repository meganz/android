package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatRoomItemStatus
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
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
            -> ChatRoomItemStatus.Joined(chatCall.getStartTimestamp())

            ChatCallStatus.UserNoPresent ->
                ChatRoomItemStatus.NotJoined(chatCall.getStartTimestamp())

            else -> ChatRoomItemStatus.NotStarted
        }
}
