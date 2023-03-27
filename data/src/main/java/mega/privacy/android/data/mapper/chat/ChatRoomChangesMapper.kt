package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.ChatRoomChanges
import nz.mega.sdk.MegaChatRoom
import javax.inject.Inject

/**
 * Mapper to convert chat room changes to [ChatRoomChanges]
 */
internal class ChatRoomChangesMapper @Inject constructor() {
    operator fun invoke(change: Int): ChatRoomChanges =
        chatRoomChanges[change] ?: ChatRoomChanges.Unknown

    companion object {
        internal val chatRoomChanges = mapOf(
            MegaChatRoom.CHANGE_TYPE_STATUS to ChatRoomChanges.Status,
            MegaChatRoom.CHANGE_TYPE_UNREAD_COUNT to ChatRoomChanges.UnreadCount,
            MegaChatRoom.CHANGE_TYPE_PARTICIPANTS to ChatRoomChanges.Participants,
            MegaChatRoom.CHANGE_TYPE_TITLE to ChatRoomChanges.Title,
            MegaChatRoom.CHANGE_TYPE_USER_TYPING to ChatRoomChanges.UserTyping,
            MegaChatRoom.CHANGE_TYPE_CLOSED to ChatRoomChanges.Closed,
            MegaChatRoom.CHANGE_TYPE_OWN_PRIV to ChatRoomChanges.OwnPrivilege,
            MegaChatRoom.CHANGE_TYPE_USER_STOP_TYPING to ChatRoomChanges.UserStopTyping,
            MegaChatRoom.CHANGE_TYPE_ARCHIVE to ChatRoomChanges.Archive,
            MegaChatRoom.CHANGE_TYPE_CHAT_MODE to ChatRoomChanges.ChatMode,
            MegaChatRoom.CHANGE_TYPE_UPDATE_PREVIEWERS to ChatRoomChanges.UpdatePreviewers,
            MegaChatRoom.CHANGE_TYPE_RETENTION_TIME to ChatRoomChanges.RetentionTime,
            MegaChatRoom.CHANGE_TYPE_OPEN_INVITE to ChatRoomChanges.OpenInvite,
            MegaChatRoom.CHANGE_TYPE_SPEAK_REQUEST to ChatRoomChanges.SpeakRequest,
            MegaChatRoom.CHANGE_TYPE_WAITING_ROOM to ChatRoomChanges.WaitingRoom,
        )
    }
}