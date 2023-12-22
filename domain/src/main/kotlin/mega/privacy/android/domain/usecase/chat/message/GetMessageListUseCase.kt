package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import javax.inject.Inject

/**
 * Get message list use case
 */
class GetMessageListUseCase @Inject constructor() {

    /**
     * Invoke
     *
     * @param messageFlow
     */
    suspend operator fun invoke(messageFlow: Flow<ChatMessage?>) =
        messageFlow
            .takeWhile { it != null }
            .take(32)
            .filterNotNull()
            .filter(::isValidMessage)
            .toList()

    private fun isValidMessage(message: ChatMessage) =
        message.type != ChatMessageType.UNKNOWN && message.type != ChatMessageType.REVOKE_NODE_ATTACHMENT
}