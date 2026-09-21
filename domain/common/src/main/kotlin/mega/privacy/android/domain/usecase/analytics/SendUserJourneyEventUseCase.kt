package mega.privacy.android.domain.usecase.analytics

import mega.privacy.android.domain.repository.StatisticsRepository
import javax.inject.Inject

/**
 * Send user journey event use case
 *
 * @property statisticsRepository
 */
class SendUserJourneyEventUseCase @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
) {
    suspend operator fun invoke(
        eventId: Int,
        message: String,
        viewId: String?,
    ) = statisticsRepository.sendEvent(
        eventId = eventId,
        message = message,
        addJourneyId = true,
        viewId = viewId
    )
}