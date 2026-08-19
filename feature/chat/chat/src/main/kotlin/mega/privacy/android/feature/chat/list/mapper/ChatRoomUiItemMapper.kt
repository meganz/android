package mega.privacy.android.feature.chat.list.mapper

import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem.ChatRoomUiAvatar
import javax.inject.Inject

/**
 * Maps a domain [ChatRoomItem] to a [ChatRoomUiItem] for the chat list.
 */
internal class ChatRoomUiItemMapper @Inject constructor() {

    /**
     * Map a [ChatRoomItem] to a [ChatRoomUiItem].
     *
     * @param item Domain chat room item.
     */
    operator fun invoke(item: ChatRoomItem): ChatRoomUiItem = ChatRoomUiItem(
        chatId = item.chatId,
        title = item.title,
        lastMessage = item.lastMessage?.takeIf(String::isNotBlank),
        lastTimestampFormatted = item.lastTimestampFormatted,
        scheduledTimestampFormatted = (item as? ChatRoomItem.MeetingChatRoomItem)
            ?.takeIf { it.isPendingMeeting() }
            ?.scheduledTimestampFormatted,
        unreadCount = item.unreadCount,
        isMuted = item.isMuted && item !is ChatRoomItem.NoteToSelfChatRoomItem,
        highlight = item.highlight,
        isNoteToSelf = item is ChatRoomItem.NoteToSelfChatRoomItem,
        avatar = when (item) {
            is ChatRoomItem.IndividualChatRoomItem -> ChatRoomUiAvatar.Peer(
                placeholderText = item.avatar?.placeholderText?.takeIf(String::isNotBlank),
                filePath = item.avatar?.uri,
                color = item.avatar?.color,
            )

            is ChatRoomItem.NoteToSelfChatRoomItem -> ChatRoomUiAvatar.NoteToSelf
            is ChatRoomItem.GroupChatRoomItem -> ChatRoomUiAvatar.Group
            is ChatRoomItem.MeetingChatRoomItem -> ChatRoomUiAvatar.Meeting
        },
        status = (item as? ChatRoomItem.IndividualChatRoomItem)
            ?.userChatStatus
            .toContactItemStatus(),
    )

    private fun UserChatStatus?.toContactItemStatus(): ContactItemStatus = when (this) {
        UserChatStatus.Online -> ContactItemStatus.Online
        UserChatStatus.Away -> ContactItemStatus.Away
        UserChatStatus.Busy -> ContactItemStatus.Busy
        UserChatStatus.Offline -> ContactItemStatus.Offline
        UserChatStatus.Invalid, null -> ContactItemStatus.Unknown
    }
}
