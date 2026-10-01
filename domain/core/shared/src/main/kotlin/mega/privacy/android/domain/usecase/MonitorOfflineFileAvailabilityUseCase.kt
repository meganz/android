package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject


/**
 * Monitor completed transfer event broadcast
 *
 * @property transferRepository
 */
class MonitorOfflineFileAvailabilityUseCase @Inject constructor(private val transferRepository: TransferRepository) {

    /**
     * Invoke
     *
     * @return Flow of Completed transfer
     */
    operator fun invoke() = transferRepository.monitorOfflineFileAvailability()
}
