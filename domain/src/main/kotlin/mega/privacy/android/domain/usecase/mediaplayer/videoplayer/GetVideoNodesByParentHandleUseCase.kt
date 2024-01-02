package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting video children by parent node handle
 */
class GetVideoNodesByParentHandleUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Get video children by parent node handle
     *
     * @param parentHandle parent node handle
     * @param order list order
     * @return video nodes
     */
    suspend operator fun invoke(parentHandle: Long, order: SortOrder) =
        mediaPlayerRepository.getVideoNodesByParentHandle(parentHandle, order)
}