package mega.privacy.android.domain.usecase.analytics

import mega.privacy.android.domain.entity.analytics.AnalyticsEvent
import mega.privacy.android.domain.repository.StatisticsRepository
import javax.inject.Inject

/**
 * Track event use case
 *
 * @property statisticsRepository
 * @constructor Create empty Track event use case
 */
class TrackEventUseCase @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
) {
    /**
     * Invoke
     *
     * @param event
     */
    suspend operator fun invoke(event: AnalyticsEvent) = statisticsRepository.logEvent(event)
}