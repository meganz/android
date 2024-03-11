package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * The use case for removing videos from a playlist.
 */
class RemoveVideosFromPlaylistUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository,
) {

    /**
     * Remove videos from the playlist.
     *
     * @param playlistID playlist id
     * @param videoElementIDs removed video element ids
     * @return the removed video handles
     */
    suspend operator fun invoke(playlistID: NodeId, videoElementIDs: List<Long>) =
        videoSectionRepository.removeVideosFromPlaylist(playlistID, videoElementIDs)
}