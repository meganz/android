package mega.privacy.android.domain.usecase.chat.message.paging

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.usecase.chat.message.GetExistsInMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.reactions.GetReactionsUseCase
import mega.privacy.android.domain.usecase.node.DoesNodeExistUseCase
import javax.inject.Inject

/**
 * Create save message request use case
 */
class CreateSaveMessageRequestUseCase @Inject constructor(
    private val getReactionsUseCase: GetReactionsUseCase,
    private val doesNodeExistUseCase: DoesNodeExistUseCase,
    private val getExistsInMessageUseCase: GetExistsInMessageUseCase,
) {

    /**
     * Invoke
     *
     * @param chatMessages List of [ChatMessage]
     * @param currentUserHandle Current user handle
     * @return List of [CreateTypedMessageRequest]
     */
    suspend operator fun invoke(
        chatId: Long,
        chatMessages: List<ChatMessage>,
        currentUserHandle: Long,
    ): List<CreateTypedMessageRequest> {

        return chatMessages
            .map { chatMessage ->
                val isMine = chatMessage.userHandle == currentUserHandle
                val reactions = chatMessage.getReactions(
                    chatId = chatId,
                    currentUserHandle = currentUserHandle
                )
                val exists = chatMessage.doNodesExist(
                    isMine = isMine,
                    chatId = chatId
                )

                CreateTypedMessageRequest(
                    chatMessage = chatMessage,
                    chatId = chatId,
                    isMine = isMine,
                    reactions = reactions,
                    exists = exists
                )
            }
    }

    private suspend fun ChatMessage.getReactions(
        chatId: Long,
        currentUserHandle: Long,
    ): List<Reaction> = if (hasConfirmedReactions) {
        this@CreateSaveMessageRequestUseCase.getReactionsUseCase(
            chatId,
            messageId,
            currentUserHandle
        )
    } else {
        emptyList()
    }

    private suspend fun ChatMessage.doNodesExist(
        isMine: Boolean,
        chatId: Long,
    ) = nodeList.firstOrNull()?.let {
        if (isMine) {
            this@CreateSaveMessageRequestUseCase.doesNodeExistUseCase(it.id)
        } else {
            this@CreateSaveMessageRequestUseCase.getExistsInMessageUseCase(chatId, messageId)
        }
    } ?: true
}