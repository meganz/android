package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import javax.inject.Inject

/**
 * Map chat message list use case
 *
 * @property createTypedMessageUseCases Map of [ChatMessageType] to [CreateTypedMessageUseCase]
 */
class MapChatMessageListUseCase @Inject constructor(
    private val createTypedMessageUseCases: Map<@JvmSuppressWildcards ChatMessageType, @JvmSuppressWildcards CreateTypedMessageUseCase>,
    private val createInvalidMessageUseCase: CreateInvalidMessageUseCase,
) {
    /**
     * Invoke
     *
     * @param chatMessages List of [ChatMessage]
     * @param currentUserHandle Current user handle
     * @return List of [TypedMessage]
     */
    operator fun invoke(
        chatMessages: List<ChatMessage>,
        currentUserHandle: Long,
    ): List<TypedMessage> {
        return chatMessages.map { chatMessage ->
            val isMine = chatMessage.userHandle == currentUserHandle
            val request = CreateTypedMessageRequest(chatMessage, isMine)
            createTypedMessageUseCases[chatMessage.type]?.invoke(request)
                ?: createInvalidMessageUseCase(request)
        }
    }
}