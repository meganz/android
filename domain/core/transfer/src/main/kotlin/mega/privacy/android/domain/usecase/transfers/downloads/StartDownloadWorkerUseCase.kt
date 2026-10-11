package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Start download worker use case
 *
 * @property transferRepository [TransferRepository].
 */
class StartDownloadWorkerUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     *
     */
    suspend operator fun invoke() = transferRepository.startDownloadWorker()
}
