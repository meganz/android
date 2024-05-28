package mega.privacy.android.domain.usecase.transfers.chatuploads

import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageTransferTagRequest
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.usecase.chat.message.AttachNodeWithPendingMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import javax.inject.Inject

/**
 * Handle transfer events for chat upload
 */
class HandleChatUploadTransferEventUseCase @Inject constructor(
    private val updatePendingMessageUseCase: UpdatePendingMessageUseCase,
    private val attachNodeWithPendingMessageUseCase: AttachNodeWithPendingMessageUseCase,

    ) {

    /**
     * Invoke
     */
    suspend operator fun invoke(
        event: MultiTransferEvent,
        vararg pendingMessageIds: Long,
    ) {
        val singleTransferEvent = (event as? MultiTransferEvent.SingleTransferEvent)
        //update transfer tag on Start event
        (singleTransferEvent?.transferEvent as? TransferEvent.TransferStartEvent)?.transfer?.tag?.let { transferTag ->
            pendingMessageIds.forEach { pendingMessageId ->
                updatePendingMessageUseCase(
                    UpdatePendingMessageTransferTagRequest(
                        pendingMessageId,
                        transferTag,
                        PendingMessageState.UPLOADING
                    )
                )
            }
        }
        //attach it if it's already uploaded
        singleTransferEvent
            ?.alreadyTransferredIds
            ?.singleOrNull()
            ?.takeIf { it.longValue != -1L }
            ?.let { alreadyTransferredNodeId ->
                pendingMessageIds.forEach { pendingMessageId ->
                    attachNodeWithPendingMessageUseCase(
                        pendingMessageId,
                        alreadyTransferredNodeId
                    )
                }
            }
        //mark as error if it's a temporary error (typically an over quota error)
        if (singleTransferEvent?.transferEvent is TransferEvent.TransferTemporaryErrorEvent) {
            updatePendingMessageUseCase(
                updatePendingMessageRequests = pendingMessageIds.map { pendingMessageId ->
                    UpdatePendingMessageStateRequest(
                        pendingMessageId,
                        PendingMessageState.ERROR_UPLOADING
                    )
                }.toTypedArray()
            )
        }
    }
}