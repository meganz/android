package mega.privacy.android.data.mapper.chat

import mega.privacy.android.data.database.entity.ChatRoomPreferenceEntity
import mega.privacy.android.domain.entity.chat.ChatRoomPreference
import javax.inject.Inject

internal class ChatRoomPreferenceEntityMapper @Inject constructor() {
    operator fun invoke(entity: ChatRoomPreference) = ChatRoomPreferenceEntity(
        chatId = entity.chatId,
        draftMessage = entity.draftMessage,
    )
}