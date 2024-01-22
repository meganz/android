package mega.privacy.android.data.mapper.chat

import mega.privacy.android.data.database.entity.ChatRoomPreferenceEntity
import mega.privacy.android.domain.entity.chat.ChatRoomPreference
import javax.inject.Inject

internal class ChatRoomPreferenceModelMapper @Inject constructor() {
    operator fun invoke(entity: ChatRoomPreferenceEntity) = ChatRoomPreference(
        chatId = entity.chatId,
        draftMessage = entity.draftMessage,
    )
}