package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Check if any pending message's transfer has finished without closing the pending message
 */
class CheckFinishedChatUploadsUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
    private val transferRepository: TransferRepository,
    private val attachNodeWithPendingMessageUseCase: AttachNodeWithPendingMessageUseCase,
    private val updatePendingMessageUseCase: UpdatePendingMessageUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() {
        chatMessageRepository.getPendingMessagesByState(PendingMessageState.UPLOADING)
            .forEach { pendingMessage ->
                transferRepository.getTransferByTag(pendingMessage.transferTag)?.let { transfer ->
                    if (transfer.isFinished) {
                        if (transfer.nodeHandle != -1L) {
                            attachNodeWithPendingMessageUseCase(
                                pendingMessage.id,
                                NodeId(transfer.nodeHandle)
                            )
                        } else {
                            pendingMessage.updateToErrorUploading()
                        }
                    }
                } ?: run {
                    pendingMessage.updateToErrorUploading()
                }
            }
    }

    private suspend fun PendingMessage.updateToErrorUploading() {
        updatePendingMessageUseCase(
            UpdatePendingMessageStateRequest(this.id, PendingMessageState.ERROR_UPLOADING)
        )
    }
}