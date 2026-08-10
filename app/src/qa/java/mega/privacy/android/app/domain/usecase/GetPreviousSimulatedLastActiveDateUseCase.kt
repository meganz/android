package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.QASimulationRepository
import javax.inject.Inject

/**
 * QA only. Returns the previously simulated last active date (epoch seconds), or null if none was set.
 *
 * It reads the last acknowledged purge timestamp (USER_ATTR_LAST_PURGE_ACKNOWLEDGED) and reverses
 * the purge offset applied by [SimulateUserLastActiveDateUseCase]
 * (`lastActive = acknowledgedPurge - PURGE_OFFSET_SECONDS`). Useful for QA to avoid picking the
 * same date again, which would produce an already-acknowledged purge timestamp the SDK suppresses.
 */
class GetPreviousSimulatedLastActiveDateUseCase @Inject constructor(
    private val qaSimulationRepository: QASimulationRepository,
) {
    /**
     * @return the previously simulated last active timestamp (epoch seconds), or null if none.
     */
    suspend operator fun invoke(): Long? {
        val acknowledgedPurgeTs = qaSimulationRepository.getLastPurgeAcknowledged()
        return if (acknowledgedPurgeTs > 0) {
            acknowledgedPurgeTs - PURGE_OFFSET_SECONDS
        } else {
            null
        }
    }
}
