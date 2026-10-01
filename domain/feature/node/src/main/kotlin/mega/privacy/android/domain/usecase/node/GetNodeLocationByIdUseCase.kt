package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import javax.inject.Inject

/**
 * Use case to determine the node location by id
 */
class GetNodeLocationByIdUseCase @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getNodeLocationUseCase: GetNodeLocationUseCase,
) {
    /**
     * Invoke.
     *
     * @param nodeId The node id to determine location for
     * @return [mega.privacy.android.domain.entity.node.NodeLocation]
     */
    suspend operator fun invoke(nodeId: NodeId) =
        getNodeByIdUseCase(nodeId)?.let {
            getNodeLocationUseCase(it)
        }
}