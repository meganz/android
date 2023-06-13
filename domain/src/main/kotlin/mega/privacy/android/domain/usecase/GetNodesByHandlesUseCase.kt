package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * The use case for getting nodes by handles
 */
class GetNodesByHandlesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Get nodes by handles
     *
     * @param handles handle list
     * @return nodes
     */
    suspend operator fun invoke(handles: List<Long>) =
        nodeRepository.getNodesByHandles(handles).map { addNodeType(it) }
}