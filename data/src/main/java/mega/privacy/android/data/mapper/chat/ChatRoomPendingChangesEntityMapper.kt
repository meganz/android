package mega.privacy.android.data.mapper.chat

import mega.privacy.android.data.database.entity.ChatPendingChangesEntity
import mega.privacy.android.domain.entity.chat.ChatPendingChanges
import javax.inject.Inject

internal class ChatRoomPendingChangesEntityMapper @Inject constructor() {
    operator fun invoke(entity: ChatPendingChanges) = ChatPendingChangesEntity(
        chatId = entity.chatId,
        draftMessage = entity.draftMessage,
        editingMessageId = entity.editingMessageId,
    )
}