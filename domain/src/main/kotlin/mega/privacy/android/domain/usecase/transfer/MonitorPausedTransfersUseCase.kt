package mega.privacy.android.domain.usecase.transfer

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for monitoring transfers paused Globally (individual transfers can be paused or not).
 *
 */
class MonitorPausedTransfersUseCase @Inject constructor(private val transferRepository: TransferRepository) {

    /**
     * Invoke
     *
     * @return Flow of Boolean. True if transfers are paused globally, false if not (individual transfers can be paused or not).
     */
    operator fun invoke(): Flow<Boolean> = transferRepository.monitorPausedTransfers()
}