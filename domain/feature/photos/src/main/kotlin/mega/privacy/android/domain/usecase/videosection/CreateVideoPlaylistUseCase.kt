package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * Use Case to create a video playlist
 */
class CreateVideoPlaylistUseCase @Inject constructor(
    private val validatePlaylistNameUseCase: ValidatePlaylistNameUseCase,
    private val videoSectionRepository: VideoSectionRepository,
) {
    /**
     * Create a new video playlist.
     *
     * @param title The title of the new video playlist.
     */
    suspend operator fun invoke(title: String): VideoPlaylist {
        validatePlaylistNameUseCase(title)
        return videoSectionRepository.createVideoPlaylist(title)
    }
}
