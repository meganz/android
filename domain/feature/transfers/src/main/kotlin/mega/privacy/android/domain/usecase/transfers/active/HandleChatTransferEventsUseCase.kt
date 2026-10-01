package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageTransferTagRequest
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
) : IHandleTransferEventUseCase {
    /**
     * Invoke
     */
    override suspend operator fun invoke(vararg events: TransferEvent) {
        val chatEvents = events.filter { it.transfer.transferType == TransferType.CHAT_UPLOAD }
        if (chatEvents.isEmpty()) return

        //update transfer uniqueId on Start event
        chatEvents
            .filterIsInstance<TransferEvent.TransferStartEvent>()
            .flatMap {
                it.transfer.pendingMessageIds()?.map { pendingMessageId ->
                    UpdatePendingMessageTransferTagRequest(
                        pendingMessageId = pendingMessageId,
                        transferUniqueId = it.transfer.uniqueId,
                        state = PendingMessageState.UPLOADING
                    )
                } ?: emptyList()
            }
            .takeIf { it.isNotEmpty() }
            ?.let {
                updatePendingMessageUseCase(*it.toTypedArray())
            }
        //once uploaded, attach the node to the chat
        chatEvents
            .filterIsInstance<TransferEvent.TransferFinishEvent>()
            .forEach { finishEvent ->
                finishEvent.transfer.pendingMessageIds()?.let { pendingMessageIds ->
                    pendingMessageIds.forEach { pendingMessageId ->
                        if (finishEvent.error == null) {
                            runCatching {
                                attachNodeWithPendingMessageUseCase(
                                    pendingMessageId,
                                    NodeId(finishEvent.transfer.nodeHandle),
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
        //mark as error if it's a temporary error (typically an over quota error)
        chatEvents
            .filterIsInstance<TransferEvent.TransferTemporaryErrorEvent>()
            .flatMap {
                it.transfer.pendingMessageIds()?.map { pendingMessageId ->
                    UpdatePendingMessageStateRequest(
                        pendingMessageId = pendingMessageId,
                        state = PendingMessageState.ERROR_UPLOADING
                    )
                } ?: emptyList()
            }
            .takeIf { it.isNotEmpty() }
            ?.let {
                updatePendingMessageUseCase(*it.toTypedArray())
            }
    }

    private suspend fun updateState(
        pendingMessageId: Long,
        state: PendingMessageState,
    ) {
        updatePendingMessageUseCase(UpdatePendingMessageStateRequest(pendingMessageId, state))
    }
}