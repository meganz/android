package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Get the versions of the node including the current one
 */
fun interface GetNodeVersionsByHandle {
    /**
     * @return a list of the versions of the node referenced by its handle [NodeId] including the current one
     */
    suspend operator fun invoke(nodeId: NodeId): List<Node>?
}