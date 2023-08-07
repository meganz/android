package mega.privacy.android.data.mapper.chat

import mega.privacy.android.data.model.chat.AndroidMegaChatMessage
import mega.privacy.android.domain.entity.chat.PendingMessage
import javax.inject.Inject

/**
 * Mapper for converting a list of [AndroidMegaChatMessage] to a list of [PendingMessage]
 */
internal class PendingMessageListMapper @Inject constructor() {

    operator fun invoke(messages: List<AndroidMegaChatMessage>) =
        messages.mapNotNull { it.pendingMessage }
}