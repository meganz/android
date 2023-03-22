package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for cancelling all transfers, uploads and downloads.
 */
class CancelTransfersUseCase @Inject constructor(private val transferRepository: TransferRepository) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() = transferRepository.cancelTransfers()
}