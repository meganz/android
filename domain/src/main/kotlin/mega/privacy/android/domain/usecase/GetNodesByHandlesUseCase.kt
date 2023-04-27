package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting nodes by handles
 */
class GetNodesByHandlesUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Get nodes by handles
     *
     * @param handles handle list
     * @return nodes
     */
    suspend operator fun invoke(handles: List<Long>) =
        mediaPlayerRepository.getNodesByHandles(handles).map { addNodeType(it) }
}