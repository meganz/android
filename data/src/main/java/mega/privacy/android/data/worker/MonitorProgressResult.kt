package mega.privacy.android.data.worker

import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult

/**
 * Data class for representing the progress of transfers, along with an indicator to determine if there is any pending work.
 * It's used to implement specific logic to end each transfer worker.
 * @param monitorOngoingActiveTransfersResult the current progress of transfers
 * @param pendingWork flag to indicate whether there are more work to do or not
 */
internal data class MonitorProgressResult(
    val monitorOngoingActiveTransfersResult: MonitorOngoingActiveTransfersResult,
    val pendingWork: Boolean,
)