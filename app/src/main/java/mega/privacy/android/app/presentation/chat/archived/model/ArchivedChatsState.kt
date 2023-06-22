package mega.privacy.android.app.presentation.chat.archived.model

import mega.privacy.android.domain.entity.chat.ChatRoomItem

data class ArchivedChatsState constructor(
    val items: List<ChatRoomItem> = emptyList(),
    val searchQuery: String? = null,
)
