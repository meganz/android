package mega.privacy.android.app.main.megachat.chat.explorer

import mega.privacy.android.domain.entity.chat.ChatListItem

/**
 * Chat explorer list item
 *
 * @property contactItem [ContactItemUiState]
 * @property chat [ChatListItem]
 * @property title The title of the item
 * @property id The ID of the item
 * @property isRecent True if the item is a recent contact, false otherwise
 * @property isHeader True is the item is a header item in the list, false otherwise
 */
data class ChatExplorerListItem @JvmOverloads constructor(
    val contactItem: ContactItemUiState? = null,
    val chat: ChatListItem? = null,
    val title: String? = null,
    val id: String? = null,
    val isRecent: Boolean = false,
    val isHeader: Boolean = false,
)
