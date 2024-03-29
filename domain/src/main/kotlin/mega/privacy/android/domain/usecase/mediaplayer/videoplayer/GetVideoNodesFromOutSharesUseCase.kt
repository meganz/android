package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting video nodes from out shares
 */
class GetVideoNodesFromOutSharesUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Getting video nodes from out shares
     *
     * @param order list order
     * @return video nodes
     */
    suspend operator fun invoke(lastHandle: Long, order: SortOrder) =
        mediaPlayerRepository.getVideoNodesFromOutShares(lastHandle, order)
}