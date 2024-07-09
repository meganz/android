package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.entity.Progress
import java.math.BigInteger

/**
 * A representation of an in progress transfer.
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
    override val isPaused: Boolean,
    override val progress: Progress,
) : TypedTransfer, InProgressTransferData