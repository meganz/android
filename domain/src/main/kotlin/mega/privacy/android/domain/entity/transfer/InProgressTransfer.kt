package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.entity.Progress
import java.math.BigInteger

/**
 * A representation of an in progress transfer.
 *
 * @property tag An integer that identifies this transfer.
 * @property transferType [TransferType]
 * @property transferredBytes Transferred bytes during this transfer.
 * @property totalBytes Total bytes to be transferred to complete the transfer.
 * @property fileName Name of the file that is being transferred.
 * @property speed The average speed of this transfer.
 * @property state [TransferState]
 * @property priority Returns the priority of the transfer.
 */
data class InProgressTransfer(
    override val tag: Int,
    override val transferType: TransferType,
    override val transferredBytes: Long,
    override val totalBytes: Long,
    override val fileName: String,
    override val speed: Long,
    override val state: TransferState,
    override val priority: BigInteger,
) : TypedTransfer, InProgressTransferData {
    /**
     * True if the transfer is paused, false otherwise
     */
    override val isPaused get() = state == TransferState.STATE_PAUSED

    /**
     * progress of the transfer
     */
    override val progress = Progress(transferredBytes, totalBytes)
}