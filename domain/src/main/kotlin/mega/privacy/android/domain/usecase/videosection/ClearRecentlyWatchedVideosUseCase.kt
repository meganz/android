package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * Use case to clear video recently watched
 */
class ClearRecentlyWatchedVideosUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository,
) {

    /**
     * Clear recently watched videos
     */
    suspend operator fun invoke() = videoSectionRepository.clearRecentlyWatchedVideos()
}