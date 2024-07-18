package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.entity.Progress
import java.math.BigInteger

/**
 * In progress transfer data interface.
 */
interface InProgressTransferData {

    /**
     * Name of the file that is being transferred.
     */
    val fileName: String

    /**
     * The average speed of this transfer.
     */
    val speed: Long

    /**
     * [TransferState]
     */
    val state: TransferState

    /**
     * Returns the priority of the transfer.
     */
    val priority: BigInteger

    /**
     * Progress of the transfer.
     */
    val progress: Progress
}