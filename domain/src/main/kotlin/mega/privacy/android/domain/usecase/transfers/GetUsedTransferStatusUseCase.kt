package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.entity.transfer.UsedTransferStatus
import javax.inject.Inject

/**
 * The use case to get used transfer status based on used transfer percentage
 */
class GetUsedTransferStatusUseCase @Inject constructor() {
    /**
     * Invoke
     */
    operator fun invoke(usedTransferPercentage: Int): UsedTransferStatus =
        when (usedTransferPercentage) {
            in 0..80 -> UsedTransferStatus.NoTransferProblems
            in 81..<100 -> UsedTransferStatus.AlmostFull
            else -> UsedTransferStatus.Full
        }
}