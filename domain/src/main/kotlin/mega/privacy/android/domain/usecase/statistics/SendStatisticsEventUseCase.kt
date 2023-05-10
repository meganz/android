package mega.privacy.android.domain.usecase.statistics

import mega.privacy.android.domain.repository.StatisticsRepository
import javax.inject.Inject

/**
 * Send a MEGA Stats event
 */
class SendStatisticsEventUseCase @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
) {

    /**
     * Invoke the use case
     *
     * @param eventID
     * @param message
     */
    suspend operator fun invoke(eventID: Int, message: String) =
        statisticsRepository.sendEvent(eventID, message)
}