package mega.privacy.android.domain.usecase.transfer

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for monitoring when transfers management have to stop.
 */
class MonitorStopTransfersWorkUseCase @Inject constructor(private val transferRepository: TransferRepository) {

    /**
     * Invoke
     *
     * @return Flow of Boolean.
     */
    operator fun invoke() = transferRepository.monitorStopTransfersWork()
}