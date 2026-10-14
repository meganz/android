package mega.privacy.android.domain.usecase.chat.message.nodehistory

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.usecase.chat.message.CreateNodeAttachmentMessageUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import javax.inject.Inject

/**
 * Creates the [NodeAttachmentMessage] for a message of the node history of a chat.
 *
 * The message gives only the file of its first node. See [CreateNodeAttachmentMessageUseCase] for
 * the reason.
 */
internal class CreateNodeHistoryAttachmentMessageUseCase @Inject constructor(
    private val createNodeAttachmentMessageUseCase: CreateNodeAttachmentMessageUseCase,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
) {

    /**
     * Invoke.
     *
     * @param chatId Id of the chat the message belongs to.
     * @param message Attachment message of the node history.
     * @return The [NodeAttachmentMessage], or null if the first node of the message is not a
     * file, for example because the message was deleted, or if the file cannot be read.
     */
    suspend operator fun invoke(chatId: Long, message: ChatMessage): NodeAttachmentMessage? =
        runCatching {
            createNodeAttachmentMessageUseCase(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    chatId = chatId,
                    isMine = message.userHandle == getMyUserHandleUseCase(),
                    reactions = emptyList(),
                    exists = true,
                )
            ) as? NodeAttachmentMessage
        }.getOrNull()
}
