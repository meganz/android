package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for monitoring paused transfers.
 *
 */
class MonitorPausedTransfersUseCase @Inject constructor(private val transferRepository: TransferRepository) {

    /**
     * Invoke
     *
     * @return Flow of Boolean. True if all transfers are paused, false if not.
     */
    operator fun invoke() = transferRepository.monitorPausedTransfers()
}
