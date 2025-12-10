package mega.privacy.android.data.mapper.node

import mega.privacy.android.domain.entity.NodeLocation
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Mapper to determine the node location for a node
 */
class NodeLocationMapper @Inject constructor() {

    /**
     * Determine the node location for a node
     *
     * @param node The node to determine location for
     * @param rootParent The root parent node
     * @param getRootNode Function to get the root node
     * @param getRubbishBinNode Function to get the rubbish bin node
     * @return The node location
     */
    suspend operator fun invoke(
        node: MegaNode,
        rootParent: MegaNode,
        getRootNode: suspend () -> MegaNode?,
        getRubbishBinNode: suspend () -> MegaNode?,
    ): NodeLocation = when {
        node.isInShare -> {
            NodeLocation.IncomingSharesRoot
        }

        rootParent.isInShare -> {
            NodeLocation.IncomingShares
        }

        rootParent.handle == getRootNode()?.handle -> {
            val isNodeInCDRoot = node.parentHandle == rootParent.handle
            if (isNodeInCDRoot) {
                NodeLocation.CloudDriveRoot
            } else {
                NodeLocation.CloudDrive
            }
        }

        rootParent.handle == getRubbishBinNode()?.handle -> {
            NodeLocation.RubbishBin
        }

        else -> {
            NodeLocation.CloudDrive
        }
    }
}

