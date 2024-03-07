package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.entity.Progress

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
     * @param transferEvent wrapped [TransferEvent]
     * @param totalBytesTransferred the total amount of bytes already transferred in all involved transfers.
     * @param totalBytesToTransfer the total amount of bytes to be transferred in all involved transfers. May be inaccurate since some nodes may not have been processed yet
     */
    data class SingleTransferEvent(
        val transferEvent: TransferEvent,
        val totalBytesTransferred: Long,
        val totalBytesToTransfer: Long,
    ) :
        MultiTransferEvent {
        /**
         * return true if this event represents a finish processing event
         */
        val isFinishScanningEvent by lazy {
            with(transferEvent) {
                when {
                    this is TransferEvent.TransferUpdateEvent &&
                            transfer.isFolderTransfer && transfer.stage == TransferStage.STAGE_TRANSFERRING_FILES -> {
                        true
                    }

                    this is TransferEvent.TransferFinishEvent -> true
                    this is TransferEvent.TransferUpdateEvent && !transfer.isFolderTransfer -> true
                    else -> false
                }
            }
        }

        /**
         * This event indicates that the transfer was not done due to being already transferred.
         */
        val isAlreadyTransferredEvent by lazy {
            with(transferEvent.transfer) {
                !isFolderTransfer && isAlreadyDownloaded
            }
        }

        /**
         * This event is related to a file transfer, not a folder.
         */
        val isFileTransferEvent by lazy { !transferEvent.transfer.isFolderTransfer }


        /**
         * Current overall progress of all the initiated transfers. May be inaccurate since some nodes may not have been processed yet, and therefore, totalBytesToTransfer could be inaccurate.
         */
        val overallProgress = Progress(totalBytesTransferred, totalBytesToTransfer)
    }

    /**
     * All transfers has been scanned by the sdk, starting from this event transfers can be retried by sdk if the app is closed
     * @property scannedFiles the amount of files scanned for the transfer of the involved nodes
     * @property alreadyDownloadedFiles the amount of already downloaded files
     */
    data class ScanningFoldersFinished(val scannedFiles: Int, val alreadyDownloadedFiles: Int) :
        MultiTransferEvent

    /**
     * Event to notify that the download cannot be done due to insufficient storage space in the destination path
     */
    data object InsufficientSpace : MultiTransferEvent
}