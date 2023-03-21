package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.statistics.MeetingsStatisticsEvents
import mega.privacy.android.domain.repository.StatisticsRepository
import javax.inject.Inject

/**
 * Default implementation of Meetings statistics
 */
class SendStatisticsMeetingsUseCase @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
) {
    /**
     * Invoke the use case
     *
     * @param event [MeetingsStatisticsEvents]
     */
    suspend operator fun invoke(event: MeetingsStatisticsEvents) =
        statisticsRepository.sendEvent(event.id, event.message)
}