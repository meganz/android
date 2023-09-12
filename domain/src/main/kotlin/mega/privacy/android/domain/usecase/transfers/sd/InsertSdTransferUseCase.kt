package mega.privacy.android.domain.usecase.transfers.sd

import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Insert Sd Transfer Use Case
 *
 */
class InsertSdTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(transfer: SdTransfer) =
        transferRepository.insertSdTransfer(transfer)
}