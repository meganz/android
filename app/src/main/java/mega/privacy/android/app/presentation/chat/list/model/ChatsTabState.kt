package mega.privacy.android.app.presentation.chat.list.model

import mega.privacy.android.domain.entity.chat.ChatItem
import mega.privacy.android.domain.entity.chat.ChatStatus

/**
 * Chats tab state
 *
 * @property chats
 * @property meetings
 * @property selectedIds
 * @property currentChatStatus
 * @property currentCallChatId
 * @property searchQuery
 * @property snackBar
 * @constructor Create empty Chat tab state
 */
data class ChatsTabState constructor(
    val chats: List<ChatItem> = emptyList(),
    val meetings: List<ChatItem> = emptyList(),
    val selectedIds: List<Long> = emptyList(),
    val currentChatStatus: ChatStatus? = null,
    val currentCallChatId: Long? = null,
    val searchQuery: String? = null,
    val snackBar: Int? = null,
)
