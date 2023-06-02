package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for getting transfer data.
 */
class GetTransferDataUseCase @Inject constructor(
    private val transferRepository: TransferRepository
) {

    /**
     * Invoke.
     *
     * @return
     */
    suspend operator fun invoke() = transferRepository.getTransferData()
}