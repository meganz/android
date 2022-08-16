package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.StatisticsRepository
import javax.inject.Inject

/**
 * Default implementation of Media Discovery statistics
 */
class DefaultSendStatisticsMediaDiscovery @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
) : SendStatisticsMediaDiscovery {

    private val mediaDiscoveryClickedEventId = 99200
    private val multiClickEventId = 99201
    private val sameFolderMultiClickEventId = 99202


    override suspend fun invoke(mediaHandle: Long) {
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