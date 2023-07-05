package mega.privacy.android.domain.entity.transfer

import javax.inject.Inject

/**
 * Mapper for creating an [ActiveTransfer] from a [Transfer]
 */
internal class ActiveTransferMapper @Inject constructor() {
    operator fun invoke(transfer: Transfer) =
        ActiveTransfer(
            tag = transfer.tag,
            transferType = transfer.type,
            totalBytes = transfer.totalBytes,
            transferredBytes = transfer.transferredBytes,
            isFinished = transfer.isFinished
        )
}