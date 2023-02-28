package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate

/**
 * Monitor updates of the children of a given node.
 * In case of a folder node, it emits changes of its child nodes and versions of this children
 * In case of a file node, it emits changes of its versions (no new versions will be emitted, as they are parents of the node)
 */
fun interface MonitorChildrenUpdates {
    /**
     * Invoke
     *
     * @return a flow of changes of the children of a given node
     */
    operator fun invoke(nodeId: NodeId): Flow<NodeUpdate>
}