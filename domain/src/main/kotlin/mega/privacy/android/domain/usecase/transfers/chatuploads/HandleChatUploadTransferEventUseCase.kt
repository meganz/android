package mega.privacy.android.domain.usecase.transfers.chatuploads

import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageTransferTagRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.isAlreadyTransferredEvent
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
        event: TransferEvent,
        vararg pendingMessageIds: Long,
    ) {
        //update transfer tag on Start event
        (event as? TransferEvent.TransferStartEvent)?.transfer?.tag?.let { transferTag ->
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
        event
            .takeIf { it.isAlreadyTransferredEvent }
            ?.transfer?.nodeHandle
            .takeIf { it != -1L }
            ?.let { nodeHandle ->
                pendingMessageIds.forEach { pendingMessageId ->
                    attachNodeWithPendingMessageUseCase(
                        pendingMessageId,
                        NodeId(nodeHandle),
                        event.transfer.appData,
                    )
                }
            }
        //mark as error if it's a temporary error (typically an over quota error)
        if (event is TransferEvent.TransferTemporaryErrorEvent) {
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