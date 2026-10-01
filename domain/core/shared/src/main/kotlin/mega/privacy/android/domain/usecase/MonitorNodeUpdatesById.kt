package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Monitor node updates for a [Node] with an specific [NodeId]
 */
fun interface MonitorNodeUpdatesById {
    /**
     * Invoke
     *
     * @return a flow of changes of the node with [nodeId] [NodeId]
     */
    operator fun invoke(nodeId: NodeId): Flow<List<NodeChanges>>
}