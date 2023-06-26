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
data class ArchivedChatsState constructor(
    val items: List<ChatRoomItem> = emptyList(),
    val searchQuery: String? = null,
    val snackBar: SnackBarItem? = null
)
