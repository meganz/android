package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Monitor failed transfer
 *
 */
class MonitorFailedTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     *
     * @return the flow of true if has failed transfer otherwise false
     */
    operator fun invoke() = transferRepository.monitorFailedTransfer()
}