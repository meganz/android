package mega.privacy.android.domain.entity.transfer

/**
 * Data class for representing the progress of transfers, along with an indicator to determine if there is any pending work.
 * It's used to implement specific logic to end each transfer worker.
 * @param monitorOngoingActiveTransfersResult the current progress of transfers
 * @param pendingWork flag to indicate whether there are more work to do or not
 */
data class TransferProgressResult(
    val monitorOngoingActiveTransfersResult: MonitorOngoingActiveTransfersResult,
    val pendingWork: Boolean,
)