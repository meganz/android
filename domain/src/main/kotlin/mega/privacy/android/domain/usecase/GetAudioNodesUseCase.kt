package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting audio nodes
 */
class GetAudioNodesUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Getting audio nodes
     *
     * @param order [SortOrder]
     * @return audio nodes
     */
    suspend operator fun invoke(order: SortOrder) =
        mediaPlayerRepository.getAudioNodes(order).map { addNodeType(it) }
}