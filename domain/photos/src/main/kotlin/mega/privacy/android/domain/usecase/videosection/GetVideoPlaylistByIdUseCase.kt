package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.videosection.PlaylistType
import mega.privacy.android.domain.repository.VideoSectionRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * Use case to get video playlist by id
 */
class GetVideoPlaylistByIdUseCase @Inject constructor(
    private val getCloudSortOrder: GetCloudSortOrder,
    private val videoSectionRepository: VideoSectionRepository
) {
    /**
     * Invoke
     *
     * @param playlistId  The playlist id
     * @param type        The playlist type
     * @return            The video playlist
     */
    suspend operator fun invoke(playlistId: NodeId, type: PlaylistType) =
        if (type == PlaylistType.Favourite) {
            val order = getCloudSortOrder()
            videoSectionRepository.getFavouritePlaylist(order)
        } else {
            videoSectionRepository.getVideoPlaylistById(playlistId)
        }
}