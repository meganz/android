package mega.privacy.android.domain.usecase.transfers.completed

import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Monitor completed transfers use case.
 *
 * @param transferRepository [TransferRepository].
 */
class MonitorCompletedTransfersByStateWithLimitUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke
     *
     * @param limit the limit size of the list. If null, the limit does not apply
     */
    operator fun invoke(limit: Int, vararg states: TransferState) =
        transferRepository.monitorCompletedTransfersByStateWithLimit(limit, *states)
}
