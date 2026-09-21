package mega.privacy.android.domain.usecase.analytics

import mega.privacy.android.domain.repository.StatisticsRepository
import javax.inject.Inject

/**
 * Get view id use case
 *
 * @property statisticsRepository
 */
class GetViewIdUseCase @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = statisticsRepository.generateViewId()
}