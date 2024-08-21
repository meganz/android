package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * Use case to get video recently watched
 */
class GetVideoRecentlyWatchedUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository,
) {

    /**
     * Get video recently watched
     *
     * @return the list of recently watched video nodes
     */
    suspend operator fun invoke() = videoSectionRepository.getRecentlyWatchedVideoNodes()
}