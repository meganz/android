package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting audio nodes from out shares
 */
class GetAudioNodesFromOutSharesUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Getting audio nodes from out shares
     *
     * @param order list order
     * @return audio nodes
     */
    suspend operator fun invoke(lastHandle: Long, order: SortOrder) =
        mediaPlayerRepository.getAudioNodesFromOutShares(lastHandle, order)
}