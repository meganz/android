package mega.privacy.android.domain.usecase.recentactions

import mega.privacy.android.domain.repository.RecentActionsRepository
import javax.inject.Inject

/**
 * Monitor when recent activity has been cleared
 */
class MonitorRecentActivityClearedUseCase @Inject constructor(
    private val recentActionsRepository: RecentActionsRepository,
) {
    operator fun invoke() = recentActionsRepository.monitorRecentActivityCleared()
}
