package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.statistics.MeetingsStatisticsEvents
import mega.privacy.android.domain.repository.StatisticsRepository
import javax.inject.Inject

/**
 * Default implementation of Meetings statistics
 */
class DefaultSendStatisticsMeetings @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
) : SendStatisticsMeetings {

    override suspend fun invoke(event: MeetingsStatisticsEvents) {
        statisticsRepository.sendEvent(event.id, event.message)
    }
}