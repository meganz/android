package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * The use case for updating video playlist title.
 */
class UpdateVideoPlaylistTitleUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository,
) {

    /**
     * Update video playlist title.
     *
     * @param playlistID playlist id
     * @param newTitle new title
     */
    suspend operator fun invoke(playlistID: NodeId, newTitle: String) =
        videoSectionRepository.updateVideoPlaylistTitle(playlistID, newTitle)
}