package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import nz.mega.sdk.MegaChatListItem
import javax.inject.Inject

/**
 * Mapper to convert chat list item changes to [ChatListItemChanges]
 */
internal class ChatListItemChangesMapper @Inject constructor() {
    operator fun invoke(change: Int?): ChatListItemChanges =
        listItemChange[change] ?: ChatListItemChanges.Unknown

    companion object {
        internal val listItemChange = mapOf(
            MegaChatListItem.CHANGE_TYPE_STATUS to ChatListItemChanges.Status,
            MegaChatListItem.CHANGE_TYPE_OWN_PRIV to ChatListItemChanges.OwnPrivilege,
            MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT to ChatListItemChanges.UnreadCount,
            MegaChatListItem.CHANGE_TYPE_PARTICIPANTS to ChatListItemChanges.Participants,
            MegaChatListItem.CHANGE_TYPE_TITLE to ChatListItemChanges.Title,
            MegaChatListItem.CHANGE_TYPE_CLOSED to ChatListItemChanges.Closed,
            MegaChatListItem.CHANGE_TYPE_LAST_MSG to ChatListItemChanges.LastMessage,
            MegaChatListItem.CHANGE_TYPE_LAST_TS to ChatListItemChanges.LastTS,
            MegaChatListItem.CHANGE_TYPE_ARCHIVE to ChatListItemChanges.Archive,
            MegaChatListItem.CHANGE_TYPE_CALL to ChatListItemChanges.Call,
            MegaChatListItem.CHANGE_TYPE_CHAT_MODE to ChatListItemChanges.ChatMode,
            MegaChatListItem.CHANGE_TYPE_UPDATE_PREVIEWERS to ChatListItemChanges.UpdatePreviewers,
            MegaChatListItem.CHANGE_TYPE_PREVIEW_CLOSED to ChatListItemChanges.PreviewClosed,
            MegaChatListItem.CHANGE_TYPE_DELETED to ChatListItemChanges.Deleted,
        )
    }
}