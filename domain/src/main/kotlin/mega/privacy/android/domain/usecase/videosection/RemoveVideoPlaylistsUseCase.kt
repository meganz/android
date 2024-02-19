package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * The use case for removing video playlists.
 */
class RemoveVideoPlaylistsUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository,
) {
    /**
     * Remove video playlists.
     *
     * @param playlistIds removed playlist ids
     */
    suspend operator fun invoke(playlistIds: List<NodeId>) =
        videoSectionRepository.removeVideoPlaylists(playlistIds)
}