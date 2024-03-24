package mega.privacy.android.domain.usecase.chat.message.retry

import kotlinx.coroutines.flow.collect
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.PendingAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.usecase.chat.message.AttachNodeWithPendingMessageUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.StartChatUploadsWithWorkerUseCase
import javax.inject.Inject

/**
 * Retry pending message with error use case
 */
class RetryPendingMessageUseCase @Inject constructor(
    private val startChatUploadsWithWorkerUseCase: StartChatUploadsWithWorkerUseCase,
    private val attachNodeWithPendingMessageUseCase: AttachNodeWithPendingMessageUseCase,
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase,
) : RetryMessageUseCase() {
    override fun canRetryMessage(message: TypedMessage): Boolean =
        message is PendingAttachmentMessage

    override suspend operator fun invoke(message: TypedMessage) {
        if (message !is PendingAttachmentMessage) throw IllegalArgumentException("Only PendingAttachmentMessage can be resend by this use-case")
        when (message.state) {
            PendingMessageState.ERROR_UPLOADING -> {
                message.file?.let {
                    startChatUploadsWithWorkerUseCase(
                        file = it,
                        pendingMessageId = message.msgId,
                        chatFilesFolderId = getMyChatsFilesFolderIdUseCase()
                    ).collect()
                }
                    ?: throw IllegalArgumentException("Only messages with file can be retried when the state is ERROR_UPLOADING")
            }

            PendingMessageState.ERROR_ATTACHING -> {

                message.nodeId?.let { nodeId ->
                    attachNodeWithPendingMessageUseCase(message.msgId, nodeId)
                }
                    ?: throw IllegalArgumentException("Only messages with nodeId can be retried when the state is ERROR_ATTACHING")

            }

            else -> throw IllegalArgumentException("Only messages in error state can be retried")
        }
    }
}