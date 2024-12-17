package mega.privacy.android.domain.usecase.transfers.completed

import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Add completed transfer if not exist use case
 *
 * @property transferRepository
 */
class AddCompletedTransferIfNotExistUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke(transfers: List<Transfer>) =
        transferRepository.addCompletedTransfersIfNotExist(transfers)
}
