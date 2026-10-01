package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting nodes by handles
 */
class GetVideoNodesByHandlesUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Get video nodes by handles
     *
     * @param handles handle list
     * @return video nodes
     */
    suspend operator fun invoke(handles: List<Long>) =
        mediaPlayerRepository.getVideoNodesByHandles(handles).filter { node ->
            node.type is VideoFileTypeInfo
        }
}