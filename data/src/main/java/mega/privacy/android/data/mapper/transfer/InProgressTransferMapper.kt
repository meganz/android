package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import javax.inject.Inject

/**
 * [Transfer] to [InProgressTransfer] mapper
 */
internal class InProgressTransferMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param transfer [Transfer]
     * @return [InProgressTransfer]
     */
    operator fun invoke(transfer: Transfer) = with(transfer) {
        InProgressTransfer(
            tag = tag,
            transferType = transferType,
            transferredBytes = transferredBytes,
            totalBytes = totalBytes,
            fileName = fileName,
            speed = speed,
            state = state,
            priority = priority,
            isPaused = isPaused,
            progress = progress,
        )
    }
}
