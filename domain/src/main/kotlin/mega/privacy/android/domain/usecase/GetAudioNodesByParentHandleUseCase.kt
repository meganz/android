package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting audio children by parent node handle
 */
class GetAudioNodesByParentHandleUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Get audio children by parent node handle
     *
     * @param parentHandle parent node handle
     * @param order list order
     * @return audio nodes
     */
    suspend operator fun invoke(parentHandle: Long, order: SortOrder) =
        mediaPlayerRepository.getAudioNodesByParentHandle(parentHandle, order)?.map {
            addNodeType(it)
        }
}