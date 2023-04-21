package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.entity.transfer.CompletedTransfer
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
    suspend operator fun invoke(transfer: CompletedTransfer) =
        transferRepository.addCompletedTransfer(transfer)
}
