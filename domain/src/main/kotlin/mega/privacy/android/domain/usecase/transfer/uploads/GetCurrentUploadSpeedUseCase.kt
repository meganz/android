package mega.privacy.android.domain.usecase.transfer.uploads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Get current upload speed use case
 *
 * @property transfersRepository
 * @constructor Create empty Get current upload speed use case.
 */
class GetCurrentUploadSpeedUseCase @Inject constructor(
    private val transfersRepository: TransferRepository,
) {

    /**
     * Invoke
     *
     * @return Current upload speed.
     */
    suspend operator fun invoke() = transfersRepository.getCurrentUploadSpeed().toLong()
}