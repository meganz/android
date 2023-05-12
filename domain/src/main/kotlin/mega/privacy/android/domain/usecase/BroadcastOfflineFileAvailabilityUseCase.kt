package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Broadcast the offline availability of the file
 *
 * @property transferRepository
 */
class BroadcastOfflineFileAvailabilityUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke(nodeHandle: Long) =
        transferRepository.broadcastOfflineFileAvailability(nodeHandle)
}
