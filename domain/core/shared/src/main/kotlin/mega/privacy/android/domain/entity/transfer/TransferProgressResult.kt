package mega.privacy.android.domain.entity.transfer

/**
 * Data class for representing the progress of transfers, along with an indicator to determine if there is any pending work.
 * It's used to implement specific logic to end each transfer worker.
 * @property monitorOngoingActiveTransfersResult the current progress of transfers.
 * @property pendingTransfers flag to indicate whether there are pending to start transfers or not.
 * @property ongoingTransfers flag to indicate whether there are ongoing transfers or not.
 */
data class TransferProgressResult(
    val monitorOngoingActiveTransfersResult: MonitorOngoingActiveTransfersResult,
    val pendingTransfers: Boolean,
    val ongoingTransfers: Boolean,
) {
    /**
     * Flag to indicate whether there are more work to do or not (ongoing or pending to start transfers)
     */
    val pendingWork = pendingTransfers || ongoingTransfers
}