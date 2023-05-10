package mega.privacy.android.domain.usecase.photos.mediadiscovery

import mega.privacy.android.domain.repository.StatisticsRepository
import javax.inject.Inject

/**
 * Send Statistics event for Media Discovery
 */
class SendStatisticsMediaDiscoveryUseCase @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
) {

    private val mediaDiscoveryClickedEventId = 99200
    private val multiClickEventId = 99201
    private val sameFolderMultiClickEventId = 99202

    /**
     * Invoke the use case
     *
     **/
    suspend operator fun invoke(mediaHandle: Long) {
        var clickCount = statisticsRepository.getMediaDiscoveryClickCount()
        var clickCountFolder = statisticsRepository.getMediaDiscoveryClickCountFolder(mediaHandle)

        statisticsRepository.setMediaDiscoveryClickCount(++clickCount)
        statisticsRepository.setMediaDiscoveryClickCountFolder(++clickCountFolder, mediaHandle)

        statisticsRepository.sendEvent(
            mediaDiscoveryClickedEventId,
            "Media Discovery Click"
        )
        if (clickCount >= 3) {
            statisticsRepository.sendEvent(
                multiClickEventId,
                "Media Discovery Click >= 3"
            )
        }
        if (clickCountFolder >= 3) {
            statisticsRepository.sendEvent(
                sameFolderMultiClickEventId,
                "Media Discovery Click Specific Folder >= 3"
            )
        }
    }
}