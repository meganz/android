package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChanges
import nz.mega.sdk.MegaChatRoom

/**
 * Mapper to convert [MegaChatRoom] to [ChatRoom]
 */
typealias ChatRoomMapper = (@JvmSuppressWildcards MegaChatRoom) -> @JvmSuppressWildcards ChatRoom

internal fun toChatRoom(megaChatRoom: MegaChatRoom): ChatRoom =
    ChatRoom(
        megaChatRoom.chatId,
        mapChanges(megaChatRoom.changes),
        megaChatRoom.title,
        mapOwnPrivilege(megaChatRoom.ownPrivilege),
        megaChatRoom.isGroup,
        megaChatRoom.isMeeting,
        megaChatRoom.isPublic,
        megaChatRoom.isPreview,
        megaChatRoom.isArchived,
        megaChatRoom.isOpenInvite
    )

private fun mapOwnPrivilege(ownPrivilege: Int): ChatRoomPermission =
    when (ownPrivilege) {
        MegaChatRoom.PRIV_RM -> ChatRoomPermission.Removed
        MegaChatRoom.PRIV_RO -> ChatRoomPermission.ReadOnly
        MegaChatRoom.PRIV_STANDARD -> ChatRoomPermission.Standard
        MegaChatRoom.PRIV_MODERATOR -> ChatRoomPermission.Moderator
        else -> ChatRoomPermission.Unknown

    }

private fun mapChanges(changes: Int): ChatRoomChanges {
    return when (changes) {
        MegaChatRoom.CHANGE_TYPE_STATUS -> ChatRoomChanges.Status
        MegaChatRoom.CHANGE_TYPE_UNREAD_COUNT -> ChatRoomChanges.UnreadCount
        MegaChatRoom.CHANGE_TYPE_PARTICIPANTS -> ChatRoomChanges.Participants
        MegaChatRoom.CHANGE_TYPE_TITLE -> ChatRoomChanges.Title
        MegaChatRoom.CHANGE_TYPE_USER_TYPING -> ChatRoomChanges.UserTyping
        MegaChatRoom.CHANGE_TYPE_CLOSED -> ChatRoomChanges.Closed
        MegaChatRoom.CHANGE_TYPE_OWN_PRIV -> ChatRoomChanges.OwnPrivilege
        MegaChatRoom.CHANGE_TYPE_USER_STOP_TYPING -> ChatRoomChanges.UserStopTyping
        MegaChatRoom.CHANGE_TYPE_ARCHIVE -> ChatRoomChanges.Archive
        MegaChatRoom.CHANGE_TYPE_CHAT_MODE -> ChatRoomChanges.ChatMode
        MegaChatRoom.CHANGE_TYPE_UPDATE_PREVIEWERS -> ChatRoomChanges.UpdatePreviewers
        MegaChatRoom.CHANGE_TYPE_RETENTION_TIME -> ChatRoomChanges.RetentionTime
        MegaChatRoom.CHANGE_TYPE_OPEN_INVITE -> ChatRoomChanges.OpenInvite
        MegaChatRoom.CHANGE_TYPE_SPEAK_REQUEST -> ChatRoomChanges.SpeakRequest
        else -> ChatRoomChanges.WaitingRoom
    }
}