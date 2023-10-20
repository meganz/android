package mega.privacy.android.domain.usecase.transfers.completed

import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Add a completed, whether succeeded or failed, transfer to local storage
 *
 * @property transferRepository
 */
class AddCompletedTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke(transfer: Transfer, error: MegaException?) =
        transferRepository.addCompletedTransfer(transfer, error)
}
