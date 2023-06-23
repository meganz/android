package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.ChatRoomChange
import nz.mega.sdk.MegaChatRoom
import javax.inject.Inject

/**
 * Mapper to convert chat room changes to List of [ChatRoomChange]
 */
internal class ChatRoomChangesMapper @Inject constructor() {

    operator fun invoke(changes: Int) =
        chatRoomChanges.filter { (it.key and changes) != 0 }.values.toList()

    companion object {
        internal val chatRoomChanges = mapOf(
            MegaChatRoom.CHANGE_TYPE_STATUS to ChatRoomChange.Status,
            MegaChatRoom.CHANGE_TYPE_UNREAD_COUNT to ChatRoomChange.UnreadCount,
            MegaChatRoom.CHANGE_TYPE_PARTICIPANTS to ChatRoomChange.Participants,
            MegaChatRoom.CHANGE_TYPE_TITLE to ChatRoomChange.Title,
            MegaChatRoom.CHANGE_TYPE_USER_TYPING to ChatRoomChange.UserTyping,
            MegaChatRoom.CHANGE_TYPE_CLOSED to ChatRoomChange.Closed,
            MegaChatRoom.CHANGE_TYPE_OWN_PRIV to ChatRoomChange.OwnPrivilege,
            MegaChatRoom.CHANGE_TYPE_USER_STOP_TYPING to ChatRoomChange.UserStopTyping,
            MegaChatRoom.CHANGE_TYPE_ARCHIVE to ChatRoomChange.Archive,
            MegaChatRoom.CHANGE_TYPE_CHAT_MODE to ChatRoomChange.ChatMode,
            MegaChatRoom.CHANGE_TYPE_UPDATE_PREVIEWERS to ChatRoomChange.UpdatePreviewers,
            MegaChatRoom.CHANGE_TYPE_RETENTION_TIME to ChatRoomChange.RetentionTime,
            MegaChatRoom.CHANGE_TYPE_OPEN_INVITE to ChatRoomChange.OpenInvite,
            MegaChatRoom.CHANGE_TYPE_SPEAK_REQUEST to ChatRoomChange.SpeakRequest,
            MegaChatRoom.CHANGE_TYPE_WAITING_ROOM to ChatRoomChange.WaitingRoom,
        )
    }
}