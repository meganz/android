package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.node.Node
import javax.inject.Inject

/**
 * Does path have sufficient space for all these nodes
 */
class DoesPathHaveSufficientSpaceForNodesUseCase @Inject constructor(
    private val totalFileSizeOfNodesUseCase: TotalFileSizeOfNodesUseCase,
    private val doesPathHaveSufficientSpaceUseCase: DoesPathHaveSufficientSpaceUseCase,
) {
    /**
     * Invoke
     *
     * @param destinationPath path where the space will be checked
     * @param nodes the list of nodes to get the required space
     * @return true if path has sufficient space, otherwise false
     */
    suspend operator fun invoke(destinationPath: String, nodes: List<Node>) =
        doesPathHaveSufficientSpaceUseCase(destinationPath, totalFileSizeOfNodesUseCase(nodes))
}