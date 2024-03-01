package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.repository.VideoSectionRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * The use case for getting all video playlists.
 */
class GetVideoPlaylistsUseCase @Inject constructor(
    private val getCloudSortOrder: GetCloudSortOrder,
    private val videoSectionRepository: VideoSectionRepository,
) {

    /**
     * Get all video playlists.
     */
    suspend operator fun invoke(): List<VideoPlaylist> = getVideoPlaylistsByOrder()

    private suspend fun getVideoPlaylistsByOrder() =
        videoSectionRepository.getVideoPlaylists().let { list ->
            if (list.isNotEmpty()) {
                orderVideoPlaylists(list)
            } else {
                list
            }
        }

    private suspend fun orderVideoPlaylists(playlists: List<VideoPlaylist>) =
        when (getCloudSortOrder()) {
            SortOrder.ORDER_DEFAULT_ASC,
            SortOrder.ORDER_LABEL_DESC,
            SortOrder.ORDER_FAV_DESC,
            SortOrder.ORDER_FAV_ASC,
            SortOrder.ORDER_SIZE_DESC,
            SortOrder.ORDER_SIZE_ASC,
            -> {
                playlists.sortedBy { it.title }
            }

            SortOrder.ORDER_DEFAULT_DESC -> {
                playlists.sortedByDescending { it.title }
            }

            SortOrder.ORDER_MODIFICATION_ASC -> {
                playlists.sortedBy { it.title }.sortedBy { it.creationTime }
            }

            SortOrder.ORDER_MODIFICATION_DESC -> {
                playlists.sortedBy { it.title }.sortedByDescending { it.creationTime }
            }

            else -> playlists
        }
}