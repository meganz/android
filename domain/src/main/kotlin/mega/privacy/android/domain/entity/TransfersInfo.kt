package mega.privacy.android.domain.entity


/**
 * Transfers info.
 *
 * @property status                             the current status of transfers
 * @property totalSizeTransferred               total size transferred
 * @property totalSizePendingTransfer           total size pending transfer
 * @property uploading                          true if there are transfers uploading, false if not (downloading)
 */
data class TransfersInfo(
    val status: TransfersStatus = TransfersStatus.NotTransferring,
    val totalSizeTransferred: Long = 0L,
    val totalSizePendingTransfer: Long = 0L,
    val uploading: Boolean = false,
) {
    /**
     * computed value of the completed progress (base 1)
     */
    val completedProgress = if (totalSizePendingTransfer > 0) {
        (totalSizeTransferred.toDouble() / totalSizePendingTransfer.toDouble()).toFloat()
    } else {
        0f
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
