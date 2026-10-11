package mega.privacy.android.domain.usecase.transfers.uploads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Start uploads worker use case
 *
 * @property transferRepository [TransferRepository].
 */
class StartUploadsWorkerUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     *
     */
    suspend operator fun invoke() = transferRepository.startUploadsWorker()
}
