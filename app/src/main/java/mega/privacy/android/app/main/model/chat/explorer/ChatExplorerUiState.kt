package mega.privacy.android.app.main.model.chat.explorer

import mega.privacy.android.app.main.megachat.chat.explorer.ChatExplorerFragment
import mega.privacy.android.app.main.megachat.chat.explorer.ChatExplorerListItem

/**
 * Ui state for [ChatExplorerFragment]
 *
 * @property items The chat explorer list items.
 * @property selectedItems List of selected items.
 * @property isItemUpdated Indicating that the items are updated. Used to optimize the recycler view.
 */
data class ChatExplorerUiState(
    val items: List<ChatExplorerListItem> = emptyList(),
    val selectedItems: List<ChatExplorerListItem> = emptyList(),
    val isItemUpdated: Boolean = false,
)
