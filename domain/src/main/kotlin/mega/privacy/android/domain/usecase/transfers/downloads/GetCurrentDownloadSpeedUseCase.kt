package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for getting the current download speed.
 *
 * @property transferRepository [TransferRepository]
 */
class GetCurrentDownloadSpeedUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     *
     * @return The number of current download speed.
     */
    suspend operator fun invoke() = transferRepository.getCurrentDownloadSpeed()
}