package mega.privacy.android.data.mapper.chat

import mega.privacy.android.data.database.entity.ChatPendingChangesEntity
import mega.privacy.android.domain.entity.chat.ChatPendingChanges
import javax.inject.Inject

internal class ChatRoomPendingChangesModelMapper @Inject constructor() {
    operator fun invoke(entity: ChatPendingChangesEntity) = ChatPendingChanges(
        chatId = entity.chatId,
        draftMessage = entity.draftMessage,
        editingMessageId = entity.editingMessageId,
    )
}