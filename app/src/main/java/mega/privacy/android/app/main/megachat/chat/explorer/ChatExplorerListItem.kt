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
 * @property isSelected True if the item is selected, false otherwise
 */
data class ChatExplorerListItem @JvmOverloads constructor(
    val contactItem: ContactItemUiState? = null,
    val chat: ChatListItem? = null,
    val title: String? = null,
    val id: String? = null,
    val isRecent: Boolean = false,
    val isHeader: Boolean = false,
    val isSelected: Boolean = false,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChatExplorerListItem

        if (contactItem != other.contactItem) return false
        if (chat != other.chat) return false
        if (title != other.title) return false
        if (id != other.id) return false
        if (isRecent != other.isRecent) return false
        if (isHeader != other.isHeader) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contactItem?.hashCode() ?: 0
        result = 31 * result + (chat?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + isRecent.hashCode()
        result = 31 * result + isHeader.hashCode()
        return result
    }
}
