package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Events for download transfers that involves more than one node
 */
sealed interface DownloadNodesEvent {

    /**
     * Transfer for this node has not been started for some reason
     * @param nodeId [NodeId] of the node that has not been transferred
     * @param exception that caused the issue
     */
    data class TransferNotStarted(val nodeId: NodeId, val exception: Throwable?) :
        DownloadNodesEvent

    /**
     * Transfer for this node has finished processing
     * @param nodeId [NodeId] of the node that has finished processing
     */
    data class TransferFinishedProcessing(val nodeId: NodeId) : DownloadNodesEvent

    /**
     * Wraps a [TransferEvent] for a single node
     * @param transferEvent wrapped [TransferEvent]
     */
    data class SingleTransferEvent(val transferEvent: TransferEvent) : DownloadNodesEvent {
        /**
         * return true if this event represents a finish processing event
         */
        fun isFinishProcessingEvent() = with(transferEvent) {
            when {
                this is TransferEvent.TransferUpdateEvent &&
                        transfer.isFolderTransfer && transfer.stage == TransferStage.STAGE_TRANSFERRING_FILES -> {
                    true
                }

                this is TransferEvent.TransferFinishEvent && transfer.isFolderTransfer -> true
                this is TransferEvent.TransferStartEvent && !transfer.isFolderTransfer -> true
                else -> false
            }
        }
    }

    /**
     * All transfers has been processed by the sdk, starting from this event downloads can be retried by sdk if the app is closed
     */
    object FinishProcessingTransfers : DownloadNodesEvent

    /**
     * Event to notify that the download cannot be done due to insufficient storage space in the destination path
     */
    object NotSufficientSpace : DownloadNodesEvent
}