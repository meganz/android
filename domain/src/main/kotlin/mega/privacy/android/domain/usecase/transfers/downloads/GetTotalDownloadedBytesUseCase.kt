package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for getting the total downloaded bytes.
 *
 * @property transferRepository [TransferRepository]
 */
class GetTotalDownloadedBytesUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     *
     * @return The number of total downloaded bytes.
     */
    suspend operator fun invoke() = transferRepository.getTotalDownloadedBytes()
}