package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for getting the total downloads excluding the background ones.
 *
 * @property transferRepository [TransferRepository]
 */
class GetTotalDownloadsNonBackgroundUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     *
     * @return The number of total downloads without background ones.
     */
    suspend operator fun invoke() = transferRepository.getTotalDownloadsNonBackground()
}