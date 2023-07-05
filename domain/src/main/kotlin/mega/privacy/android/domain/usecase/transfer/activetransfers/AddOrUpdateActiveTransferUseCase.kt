package mega.privacy.android.domain.usecase.transfer.activetransfers

import mega.privacy.android.domain.entity.transfer.ActiveTransferMapper
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Add or update if already exists an active transfer to local storage
 *
 * @property transferRepository
 */
class AddOrUpdateActiveTransferUseCase @Inject internal constructor(
    private val transferRepository: TransferRepository,
    private val activeTransferMapper: ActiveTransferMapper,
) {

    /**
     * Invoke.
     * @param transfer the [Transfer] that has been updated, so it's active.
     */
    suspend operator fun invoke(transfer: Transfer) =
        transferRepository.insertOrUpdateActiveTransfer(
            activeTransferMapper(transfer)
        )
}