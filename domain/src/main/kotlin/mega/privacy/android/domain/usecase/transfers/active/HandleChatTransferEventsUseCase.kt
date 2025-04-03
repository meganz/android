package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pendingMessageIds
import mega.privacy.android.domain.usecase.chat.message.AttachNodeWithPendingMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import javax.inject.Inject

/**
 * Use case to handle chat transfer events
 */
class HandleChatTransferEventsUseCase @Inject constructor(
    private val attachNodeWithPendingMessageUseCase: AttachNodeWithPendingMessageUseCase,
    private val updatePendingMessageUseCase: UpdatePendingMessageUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(vararg events: TransferEvent) {
        events
            .filter { it.transfer.transferType == TransferType.CHAT_UPLOAD }
            .filterIsInstance<TransferEvent.TransferFinishEvent>()
            .forEach { finishEvent ->
                finishEvent.transfer.pendingMessageIds()?.let { pendingMessageIds ->
                    pendingMessageIds.forEach { pendingMessageId ->
                        if (finishEvent.error == null) {
                            runCatching {
                                //once uploaded, it can be attached to the chat
                                attachNodeWithPendingMessageUseCase(
                                    pendingMessageId,
                                    NodeId(finishEvent.transfer.nodeHandle),
                                    finishEvent.transfer.appData,
                                )
                            }.onFailure {
                                updateState(
                                    pendingMessageId,
                                    PendingMessageState.ERROR_ATTACHING
                                )
                            }
                        } else {
                            updateState(pendingMessageId, PendingMessageState.ERROR_UPLOADING)
                        }
                    }
                }
            }
    }

    private suspend fun updateState(
        pendingMessageId: Long,
        state: PendingMessageState,
    ) {
        updatePendingMessageUseCase(UpdatePendingMessageStateRequest(pendingMessageId, state))
    }
}