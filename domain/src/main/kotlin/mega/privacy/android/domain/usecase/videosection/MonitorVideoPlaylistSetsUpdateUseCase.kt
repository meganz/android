package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * Use case for monitoring video playlist sets update
 */
class MonitorVideoPlaylistSetsUpdateUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository,
) {

    /**
     * Invoke
     *
     * @return a flow of video playlist sets update
     */
    operator fun invoke() = videoSectionRepository.monitorVideoPlaylistSetsUpdate()
}