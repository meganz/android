package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.flow.toList
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Get message list use case
 */
class GetMessageListUseCase @Inject constructor() {

    /**
     * Invoke
     *
     * @param messageFlow
     */
    @OptIn(FlowPreview::class)
    suspend operator fun invoke(messageFlow: Flow<ChatMessage?>) =
        messageFlow
            .timeout(5.seconds)
            .takeWhile { it != null }
            .take(32)
            .filterNotNull()
            .filter(::isValidMessage)
            .toList()

    private fun isValidMessage(message: ChatMessage) =
        message.type != ChatMessageType.UNKNOWN && message.type != ChatMessageType.REVOKE_NODE_ATTACHMENT
}