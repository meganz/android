package mega.privacy.android.app.main.megachat.chat.explorer

import mega.privacy.android.app.MegaContactAdapter
import nz.mega.sdk.MegaChatListItem

/**
 * Chat explorer list item
 *
 * @property contact [MegaContactAdapter]
 * @property chat [MegaChatListItem]
 * @property title The title of the item
 * @property id The ID of the item
 * @property isRecent True if the item is a recent contact, false otherwise
 * @property isHeader True is the item is a header item in the list, false otherwise
 */
data class ChatExplorerListItem @JvmOverloads constructor(
    val contact: MegaContactAdapter? = null,
    val chat: MegaChatListItem? = null,
    val title: String? = null,
    val id: String? = null,
    var isRecent: Boolean = false,
    val isHeader: Boolean = false,
)
