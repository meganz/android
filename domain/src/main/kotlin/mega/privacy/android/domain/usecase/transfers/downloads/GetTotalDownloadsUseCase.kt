package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for getting the total downloads.
 *
 * @property transferRepository [TransferRepository]
 */
@Deprecated(
    "Use case deprecated. Replace with corresponding use case getting the info " +
            "from ActiveTransfers when ready."
)
class GetTotalDownloadsUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     *
     * @return The number of total downloads.
     */
    suspend operator fun invoke() = transferRepository.getTotalDownloads()
}