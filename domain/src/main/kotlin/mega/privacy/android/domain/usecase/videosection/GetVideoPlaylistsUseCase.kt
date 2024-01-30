package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * The use case for getting all video playlists.
 */
class GetVideoPlaylistsUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository
) {

    /**
     * Get all video playlists.
     */
    suspend operator fun invoke(): List<VideoPlaylist> = videoSectionRepository.getVideoPlaylists()
}