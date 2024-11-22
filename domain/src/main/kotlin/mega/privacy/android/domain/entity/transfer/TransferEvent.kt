package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.exception.MegaException


/**
 * Transfer event domain model of GlobalTransfer object
 *
 * @property transfer
 */
sealed interface TransferEvent {
    val transfer: Transfer


    /**
     * Transfer start event
     *
     * @property transfer
     */
    data class TransferStartEvent(override val transfer: Transfer) : TransferEvent

    /**
     * Transfer finish event
     *
     * @property transfer
     * @property error
     */
    data class TransferFinishEvent(
        override val transfer: Transfer,
        val error: MegaException?,
    ) : TransferEvent

    /**
     * Transfer update event
     *
     * @property transfer
     */
    data class TransferUpdateEvent(override val transfer: Transfer) :
        TransferEvent

    /**
     * Transfer temporary error
     *
     * @property transfer
     * @property error
     */
    data class TransferTemporaryErrorEvent(
        override val transfer: Transfer,
        val error: MegaException?,
    ) : TransferEvent

    /**
     * Transfer data
     *
     * @property transfer
     * @property buffer
     */
    data class TransferDataEvent(override val transfer: Transfer, val buffer: ByteArray?) :
        TransferEvent

    /**
     * Transfer has been paused or resumed
     * @param paused
     */
    data class TransferPaused(override val transfer: Transfer, val paused: Boolean) : TransferEvent


    /**
     * Folder transfer update
     *
     * @property transfer
     * @property stage
     * @property folderCount
     * @property createdFolderCount
     * @property fileCount
     * @property currentFolder
     * @property currentFileLeafName
     */
    data class FolderTransferUpdateEvent(
        override val transfer: Transfer,
        val stage: TransferStage,
        val folderCount: Long,
        val createdFolderCount: Long,
        val fileCount: Long,
        val currentFolder: String?,
        val currentFileLeafName: String?,
    ) : TransferEvent
}

/**
 * return true if this event represents a finish processing event (or already finished)
 */
val TransferEvent.isFinishScanningEvent: Boolean
    get() = when {
        (this as? TransferEvent.FolderTransferUpdateEvent)?.stage == TransferStage.STAGE_TRANSFERRING_FILES -> true
        this is TransferEvent.TransferFinishEvent -> true
        this is TransferEvent.TransferStartEvent && isFileTransfer -> true
        this is TransferEvent.TransferUpdateEvent && isFileTransfer -> true
        else -> false
    }

val TransferEvent.isTransferUpdated: Boolean
    get() = when {
        isFileTransfer && this !is TransferEvent.TransferStartEvent -> true
        isFolderTransfer && isFinishScanningEvent -> true
        else -> false
    }

/**
 * This event indicates that the transfer was not done due to being already transferred.
 */
val TransferEvent.isAlreadyTransferredEvent: Boolean
    get() = with(this.transfer) {
        !isFolderTransfer && isAlreadyTransferred
    }

/**
 * This event is related to a file transfer, not a folder.
 */
val TransferEvent.isFileTransfer: Boolean
    get() = !this.transfer.isFolderTransfer

val TransferEvent.isFolderTransfer: Boolean
    get() = this.transfer.isFolderTransfer
