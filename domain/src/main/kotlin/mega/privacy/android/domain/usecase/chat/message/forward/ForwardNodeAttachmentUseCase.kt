package mega.privacy.android.domain.usecase.chat.message.forward

import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.chat.message.AttachNodeUseCase
import javax.inject.Inject

/**
 * Use case to forward a normal message.
 */
class ForwardNodeAttachmentUseCase @Inject constructor(
    private val attachNodeUseCase: AttachNodeUseCase,
) : ForwardMessageUseCase() {
    override suspend fun forwardMessage(targetChatId: Long, message: TypedMessage): ForwardResult? {
        val nodeAttachmentMessage = message as? NodeAttachmentMessage ?: return null

        return runCatching {
            attachNodeUseCase(
                chatId = targetChatId,
                fileNode = nodeAttachmentMessage.fileNode
            )
            ForwardResult.Success(targetChatId)
        }.getOrElse {
            // API_ENOENT = -9, the file is not available
            if ((it as MegaException).errorCode == API_ENOENT) {
                ForwardResult.ErrorNotAvailable
            } else {
                ForwardResult.GeneralError
            }
        }
    }

    companion object {
        internal const val API_ENOENT = -9
    }
}