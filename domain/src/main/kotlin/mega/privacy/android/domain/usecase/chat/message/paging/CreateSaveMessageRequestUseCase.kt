package mega.privacy.android.domain.usecase.chat.message.paging

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.ChatMessageInfo
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
        nextMessage: ChatMessageInfo?,
    ): List<CreateTypedMessageRequest> {

        val avatarMessageIds = chatMessages.getAvatarMessageIds(
            nextMessageUserHandle = nextMessage?.takeUnless { ignoredTypes.contains(it.type) }?.userHandle,
            currentUserHandle = currentUserHandle
        )

        return chatMessages
            .map { chatMessage ->
                val isMine = chatMessage.userHandle == currentUserHandle
                val shouldShowAvatar = avatarMessageIds.contains(chatMessage.messageId)
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
                    shouldShowAvatar = shouldShowAvatar,
                    reactions = reactions,
                    exists = exists
                )
            }
    }

    private fun List<ChatMessage>.getAvatarMessageIds(
        nextMessageUserHandle: Long?,
        currentUserHandle: Long,
    ) = sortedBy { it.timestamp }
        .fold(mutableListOf<ChatMessage>()) { acc, chatMessage ->
            if (ignoredTypes.contains(chatMessage.type)) return@fold acc
            if (acc.isEmpty() || acc.last().userHandle != chatMessage.userHandle) {
                acc.add(chatMessage)
            } else {
                acc[acc.size - 1] = chatMessage
            }
            acc
        }.apply {
            if (lastMessageMatchesNext(nextMessageUserHandle)) removeLast()
            removeIf { it.userHandle == currentUserHandle }
        }.map { it.messageId }
        .toSet()

    private fun MutableList<ChatMessage>.lastMessageMatchesNext(
        nextMessageUserHandle: Long?,
    ) = nextMessageUserHandle != null && lastOrNull()?.userHandle == nextMessageUserHandle

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

    private val ignoredTypes = setOf(
        ChatMessageType.UNKNOWN,
        ChatMessageType.INVALID,
        ChatMessageType.ALTER_PARTICIPANTS,
        ChatMessageType.TRUNCATE,
        ChatMessageType.PRIV_CHANGE,
        ChatMessageType.CHAT_TITLE,
        ChatMessageType.CALL_ENDED,
        ChatMessageType.CALL_STARTED,
        ChatMessageType.PUBLIC_HANDLE_CREATE,
        ChatMessageType.PUBLIC_HANDLE_DELETE,
        ChatMessageType.SET_PRIVATE_MODE,
        ChatMessageType.SET_RETENTION_TIME,
        ChatMessageType.SCHED_MEETING,
        ChatMessageType.REVOKE_NODE_ATTACHMENT,
    )
}