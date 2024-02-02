package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * The use case for adding videos to a playlist.
 */
class AddVideosToPlaylistUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository,
) {

    /**
     * Add videos to the playlist.
     *
     * @param playlistID playlist id
     * @param videoIDs added video ids
     */
    suspend operator fun invoke(playlistID: NodeId, videoIDs: List<NodeId>) =
        videoSectionRepository.addVideosToPlaylist(playlistID, videoIDs)

}