package mega.privacy.android.shared.original.core.ui.model


/**
 * Transfers info to show progress in transfer widget view.
 *
 * @property status                             the current status of transfers
 * @property totalSizeAlreadyTransferred        total size already transferred
 * @property totalSizeToTransfer                total size to transfer in the current transfer batch
 * @property uploading                          true if there are transfers uploading, false if not (downloading)
 */
data class TransfersInfo(
    val status: TransfersStatus = TransfersStatus.NotTransferring,
    val totalSizeAlreadyTransferred: Long = 0L,
    val totalSizeToTransfer: Long = 0L,
    val uploading: Boolean = false,
) {
    /**
     * computed value of the completed progress (base 1)
     */
    val completedProgress = if (totalSizeToTransfer <= 0L) {
        0f
    } else {
        totalSizeAlreadyTransferred.toFloat() / totalSizeToTransfer.toFloat()
    }
}

/**
 * Status of transfers
 *
 */
enum class TransfersStatus {
    /**
     * Transferring is in progress, either uploading or downloading or both
     */
    Transferring,

    /**
     * Transferring is paused
     */
    Paused,

    /**
     * Over quota issue
     */
    OverQuota,

    /**
     * Error happened
     */
    TransferError,

    /**
     * Currently not transferring
     */
    NotTransferring
}
