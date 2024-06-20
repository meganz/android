package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * The user case for adding video to multiple playlists
 */
class AddVideoToMultiplePlaylistsUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository,
) {

    /**
     * Add video to the multiple playlists.
     *
     * @param playlistIDs playlist id list
     * @param videoID added video id
     *
     * @return the ids of the added video playlist which added the video
     */
    suspend operator fun invoke(playlistIDs: List<Long>, videoID: Long) =
        videoSectionRepository.addVideoToMultiplePlaylists(playlistIDs, videoID)
}