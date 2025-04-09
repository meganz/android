package mega.privacy.android.app.presentation.chat.archived.model

import mega.privacy.android.app.presentation.data.SnackBarItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem

/**
 * Archived chats state
 *
 * @property items
 * @property searchQuery
 * @property snackBar
 */
data class ArchivedChatsState(
    val items: List<ChatRoomItem> = emptyList(),
    val searchQuery: String? = null,
    val snackBar: SnackBarItem? = null,
) {
    /**
     * Check if the chats list is empty
     */
    val noChats
        get() = items.isEmpty()

    /**
     * Check if the only chat is a note to self
     */
    val onlyNoteToSelfChat
        get() = items.size == 1 && items.first() is ChatRoomItem.NoteToSelfChatRoomItem
}
