package mega.privacy.android.domain.usecase.chat.message.paging

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Create save message request use case
 */
class CreateSaveMessageRequestUseCase @Inject constructor() {

    /**
     * Invoke
     *
     * @param chatMessages List of [ChatMessage]
     * @param currentUserHandle Current user handle
     * @return List of [CreateTypedMessageRequest]
     */
    operator fun invoke(
        chatMessages: List<ChatMessage>,
        currentUserHandle: Long,
        nextMessageUserHandle: Long?,
    ): List<CreateTypedMessageRequest> {
        return chatMessages
            .sortedBy { it.timestamp }
            .mapIndexed { index, chatMessage ->
                val isMine = chatMessage.userHandle == currentUserHandle
                val shouldShowAvatar = shouldShowAvatar(
                    current = chatMessage,
                    nextUserHandle = getNextMessageUserHandle(
                        chatMessages,
                        chatMessages.indexOf(chatMessage),
                        nextMessageUserHandle
                    ),
                    currentIsMine = isMine
                )
                val previous = chatMessages.getOrNull(index - 1)
                val shouldShowTime = shouldShowTime(
                    chatMessage,
                    previous
                )
                val shouldShowDate = shouldShowDate(
                    chatMessage,
                    previous
                )

                CreateTypedMessageRequest(
                    chatMessage = chatMessage,
                    isMine = isMine,
                    shouldShowAvatar = shouldShowAvatar,
                    shouldShowTime = shouldShowTime,
                    shouldShowDate = shouldShowDate,
                )
            }
    }

    private fun getNextMessageUserHandle(
        typedMessages: List<ChatMessage>,
        index: Int,
        nextMessageUserHandle: Long?,
    ) = typedMessages.getOrNull(index + 1)?.userHandle ?: nextMessageUserHandle

    private fun shouldShowAvatar(
        current: ChatMessage,
        nextUserHandle: Long?,
        currentIsMine: Boolean,
    ) = !currentIsMine && !current.hasSameSender(nextUserHandle)

    private fun shouldShowTime(
        current: ChatMessage,
        previous: ChatMessage?,
    ) = !current.hasSameSender(previous?.userHandle) || current.timestamp.minus(
        previous?.timestamp ?: 0
    ) > TimeUnit.MINUTES.toSeconds(3)

    private fun ChatMessage.hasSameSender(
        other: Long?,
    ) = userHandle == other

    private fun shouldShowDate(
        current: ChatMessage,
        previous: ChatMessage?,
    ) = previous?.getDate() != current.getDate()

    private fun ChatMessage.getDate() =
        Calendar.getInstance().apply {
            timeInMillis = TimeUnit.SECONDS.toMillis(this@getDate.timestamp)
        }.get(Calendar.DATE)
}