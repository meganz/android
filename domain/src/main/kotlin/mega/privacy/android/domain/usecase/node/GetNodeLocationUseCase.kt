package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeLocation
import mega.privacy.android.domain.entity.node.NodeSourceType
import javax.inject.Inject


/**
 * Use case to get [NodeLocation] from a Node.
 */
class GetNodeLocationUseCase @Inject constructor(
    private val isNodeInCloudDriveUseCase: IsNodeInCloudDriveUseCase,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val getAncestorsIdsUseCase: GetAncestorsIdsUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(node: Node): NodeLocation {
        val nodeSourceType = when {
            isNodeInCloudDriveUseCase(node.id.longValue) -> NodeSourceType.CLOUD_DRIVE
            isNodeInRubbishBinUseCase(node.id) -> NodeSourceType.RUBBISH_BIN
            else -> NodeSourceType.INCOMING_SHARES
        }
        val ancestorIds = getAncestorsIdsUseCase(node)
            .dropLast(if (nodeSourceType == NodeSourceType.INCOMING_SHARES) 0 else 1)

        return NodeLocation(
            node = node,
            nodeSourceType = nodeSourceType,
            ancestorIds = ancestorIds,
        )
    }
}