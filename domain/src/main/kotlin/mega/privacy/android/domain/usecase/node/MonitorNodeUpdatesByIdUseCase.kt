package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.account.MonitorRefreshSessionUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import javax.inject.Inject

/**
 * Monitor both online and offline node updates by folder node id
 */
class MonitorNodeUpdatesByIdUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorRefreshSessionUseCase: MonitorRefreshSessionUseCase,
) {
    /**
     * Invoke
     *
     * @param nodeId the id of the node to monitor updates for
     * @param nodeSourceType the source type of the node
     * @return a flow of NodeChanges
     */
    @OptIn(FlowPreview::class)
    operator fun invoke(
        nodeId: NodeId,
        nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    ) = flow {
        val effectiveNodeId = resolveEffectiveNodeId(nodeId, nodeSourceType)
        emitAll(
            merge(
                monitorOnlineNodeUpdates(
                    effectiveNodeId = effectiveNodeId,
                    nodeSourceType = nodeSourceType
                ),
                monitorOfflineNodeUpdates(effectiveNodeId),
                monitorRefreshSessionUpdates()
            ).conflate().debounce(500L)
        )
    }

    /**
     * Fetch cloud drive root node id if the provided node id is -1L and the source type is cloud drive
     */
    private suspend fun resolveEffectiveNodeId(
        nodeId: NodeId,
        nodeSourceType: NodeSourceType,
    ) = if (nodeId.longValue == -1L && nodeSourceType == NodeSourceType.CLOUD_DRIVE) {
        getRootNodeUseCase()?.id ?: nodeId
    } else {
        nodeId
    }

    private fun monitorOnlineNodeUpdates(
        effectiveNodeId: NodeId,
        nodeSourceType: NodeSourceType,
    ) = nodeRepository.monitorNodeUpdates()
        .filter { update ->
            update.changes.keys.any { node ->
                node.parentId == effectiveNodeId
                        || node.id == effectiveNodeId
                        || update.isChangesInOutgoingShares(node, nodeSourceType)
            }
        }
        .map {
            val isNodeRemoved = it.changes.keys.any { node ->
                // Folder moved to rubbish bin or removed from incoming shares
                node is FolderNode
                        && node.id == effectiveNodeId
                        && (node.isInRubbishBin || (nodeSourceType == NodeSourceType.INCOMING_SHARES && !node.isIncomingShare))
            }
            if (isNodeRemoved) NodeChanges.Remove else NodeChanges.Attributes
        }

    private fun NodeUpdate.isChangesInOutgoingShares(
        node: Node,
        nodeSourceType: NodeSourceType,
    ) = nodeSourceType == NodeSourceType.OUTGOING_SHARES
            && ((node as? FolderNode)?.isShared == true
            || this.changes[node]?.contains(NodeChanges.Outshare) == true)

    private fun monitorOfflineNodeUpdates(
        effectiveNodeId: NodeId,
    ) = monitorOfflineNodeUpdatesUseCase()
        .mapNotNull { offlineList ->
            val relevantOfflineNodes = offlineList.filter { offline ->
                offline.parentId.toLong() == effectiveNodeId.longValue ||
                        offline.handle.toLong() == effectiveNodeId.longValue
            }
            if (relevantOfflineNodes.isNotEmpty()) NodeChanges.Attributes else null
        }

    private fun monitorRefreshSessionUpdates() = monitorRefreshSessionUseCase().map {
        NodeChanges.Attributes
    }
}
