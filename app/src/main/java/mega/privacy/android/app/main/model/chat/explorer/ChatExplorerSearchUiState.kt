package mega.privacy.android.app.main.model.chat.explorer

import android.util.SparseBooleanArray
import mega.privacy.android.app.main.megachat.chat.explorer.ChatExplorerListItem

/**
 * Chat explorer search ui state
 *
 * @property items The list of search items.
 * @property selectedItems Array of selected search items' indexes.
 */
data class ChatExplorerSearchUiState(
    val items: List<ChatExplorerListItem> = emptyList(),
    val selectedItems: SparseBooleanArray = SparseBooleanArray(),
)
