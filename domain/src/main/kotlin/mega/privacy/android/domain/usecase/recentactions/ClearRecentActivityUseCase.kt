package mega.privacy.android.domain.usecase.recentactions

import mega.privacy.android.domain.repository.RecentActionsRepository
import javax.inject.Inject

/**
 * Clear the recent actions up to given timestamp
 */
class ClearRecentActivityUseCase @Inject constructor(
    private val recentActionsRepository: RecentActionsRepository,
) {
    /**
     * Invoke
     *
     * @param until Epoch time (in seconds). Recent actions up to this time will be cleared.
     */
    suspend operator fun invoke(until: Long) = recentActionsRepository.clearRecentActions(until)
}
