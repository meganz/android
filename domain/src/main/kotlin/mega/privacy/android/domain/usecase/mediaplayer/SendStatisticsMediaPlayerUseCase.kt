package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.entity.statistics.MediaPlayerStatisticsEvents
import mega.privacy.android.domain.repository.StatisticsRepository
import javax.inject.Inject

/**
 * The use case for data statistics regarding media player
 */
class SendStatisticsMediaPlayerUseCase @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
) {
    /**
     * Invoke the use case
     *
     * @param event [MediaPlayerStatisticsEvents]
     */
    suspend operator fun invoke(event: MediaPlayerStatisticsEvents) =
        statisticsRepository.sendEvent(
            eventId = event.id,
            message = event.message,
            addJourneyId = event.addJourneyId,
            viewId = event.viewId
        )
}