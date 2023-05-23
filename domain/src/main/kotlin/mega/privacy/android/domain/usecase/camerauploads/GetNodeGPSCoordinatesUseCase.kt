package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get Node's GPS Coordinates
 */
class GetNodeGPSCoordinatesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * invoke
     *
     * @param nodeId [NodeId]
     * @return latitude [Double] and longitude [Double] as [Pair]
     */
    suspend operator fun invoke(nodeId: NodeId) = nodeRepository.getNodeGPSCoordinates(nodeId)
}
