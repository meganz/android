package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * The use case for getting all video playlist sets
 */
class GetVideoPlaylistSetsUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository,
) {
    /**
     * Get all video playlist sets
     *
     * @return all video playlist sets
     */
    suspend operator fun invoke() = videoSectionRepository.getVideoPlaylistSets()
}