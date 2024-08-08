package mega.privacy.android.domain.usecase.transfers.completed

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Monitor completed transfers use case.
 *
 * @param transferRepository [TransferRepository].
 */
class MonitorCompletedTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke
     *
     * @param size the limit size of the list. If null, the limit does not apply
     */
    operator fun invoke(size: Int? = null) =
        transferRepository.monitorCompletedTransfers(size)
}
