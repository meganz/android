package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Events for transfers (Upload or Download) that may involve more than one file or folder
 */
sealed interface MultiTransferEvent {

    /**
     * Transfer for this node has not been started for some reason
     * @param T item type, usually [NodeId] or [File]
     * @param item to identify the transfer, usually [NodeId] for downloads and [File] for uploads
     * @param exception that caused the issue
     */
    data class TransferNotStarted<T>(val item: T, val exception: Throwable?) :
        MultiTransferEvent

    /**
     * Wraps a [TransferEvent] for a single node event
     * @property transferEvent wrapped [TransferEvent]
     * @property totalBytesTransferred the total amount of bytes already transferred in all involved transfers.
     * @property totalBytesToTransfer the total amount of bytes to be transferred in all involved transfers. May be inaccurate if [scanningFinished] is false since some nodes may not have been processed yet
     * @property startedFiles the amount of files scanned for the transfer of the involved nodes
     * @property alreadyTransferred the amount of already transferred files
     * @property alreadyTransferredIds the ids of the nodes already transferred
     * @property scanningFinished All transfers has been scanned by the sdk, starting from this event transfers can be retried by sdk if the app is closed
     * @property allTransfersUpdated All transfers have been started and updated by the sdk, starting from this event we can know all the transfer that will
     * be skipped by the SDK because they have ben already downloaded. When we receive the Start even event from the SDK we are not sure if it will be
     * downloaded or not, we need to wait for the first update (it will be downloaded) or for Finish event without transferred bites to 0 (it has been skipped)
     */
    data class SingleTransferEvent(
        val transferEvent: TransferEvent,
        val totalBytesTransferred: Long,
        val totalBytesToTransfer: Long,
        val startedFiles: Int = 0,
        val alreadyTransferred: Int = 0,
        val alreadyTransferredIds: Set<NodeId> = emptySet(),
        val scanningFinished: Boolean = false,
        val allTransfersUpdated: Boolean = false,
    ) : MultiTransferEvent {

        /**
         * Returns true if the transfer finished with error.
         */
        val finishedWithError by lazy {
            transferEvent is TransferEvent.TransferFinishEvent && transferEvent.error != null
        }

        /**
         * Current overall progress of all the initiated transfers. May be inaccurate since some nodes may not have been processed yet, and therefore, totalBytesToTransfer could be inaccurate.
         */
        val overallProgress = Progress(totalBytesTransferred, totalBytesToTransfer)
    }

    /**
     * Event to notify that the download cannot be done due to insufficient storage space in the destination path
     */
    data object InsufficientSpace : MultiTransferEvent
}