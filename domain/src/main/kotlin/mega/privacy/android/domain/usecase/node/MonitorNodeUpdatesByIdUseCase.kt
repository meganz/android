package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
import mega.privacy.android.domain.usecase.contact.MonitorContactNameUpdatesUseCase
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
    private val monitorContactNameUpdatesUseCase: MonitorContactNameUpdatesUseCase,
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
        val effectiveNodeId = resolveEffectiveNodeId(
            nodeId = nodeId,
            nodeSourceType = nodeSourceType
        )
        val offlineNodeIdsToMatch = resolveOfflineNodeIds(
            originalNodeId = nodeId,
            effectiveNodeId = effectiveNodeId
        )
        emitAll(
            merge(
                monitorOnlineNodeUpdates(
                    effectiveNodeId = effectiveNodeId,
                    nodeSourceType = nodeSourceType
                ),
                monitorOfflineNodeUpdates(offlineNodeIdsToMatch),
                monitorRefreshSessionUpdates(),
                monitorContactNameUpdates(nodeSourceType)
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

    /**
     * Resolve node IDs for offline filtering
     * Includes both effectiveNodeId and -1L if the original nodeId is root (-1L)
     * since offline database stores parentId = -1L instead of actual root node id
     */
    private fun resolveOfflineNodeIds(
        originalNodeId: NodeId,
        effectiveNodeId: NodeId,
    ) = buildSet {
        add(effectiveNodeId.longValue)
        if (originalNodeId.longValue == -1L) {
            add(-1L)
        }
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

    /**
     * Check if there are changes in root directory of outgoing shares
     */
    private fun NodeUpdate.isChangesInOutgoingShares(
        node: Node,
        nodeSourceType: NodeSourceType,
    ) = nodeSourceType == NodeSourceType.OUTGOING_SHARES
            && ((node as? FolderNode)?.isShared == true
            || this.changes[node]?.contains(NodeChanges.Outshare) == true)

    private fun monitorOfflineNodeUpdates(nodeIdsToMatch: Set<Long>) =
        monitorOfflineNodeUpdatesUseCase()
            .drop(1) // Skip the first emission (initial load from database)
            .map { offlineList ->
                offlineList.filter { offline ->
                    offline.parentId.toLong() in nodeIdsToMatch ||
                            offline.handle.toLong() in nodeIdsToMatch
                }
            }
            .distinctUntilChanged() // Only emit when the filtered list actually changes
            .map { NodeChanges.Attributes }

    private fun monitorRefreshSessionUpdates() = monitorRefreshSessionUseCase().map {
        NodeChanges.Attributes
    }

    /**
     * Monitor contact name updates only for incoming shares and outgoing shares
     */
    private fun monitorContactNameUpdates(nodeSourceType: NodeSourceType) =
        if (nodeSourceType == NodeSourceType.INCOMING_SHARES || nodeSourceType == NodeSourceType.OUTGOING_SHARES) {
            monitorContactNameUpdatesUseCase().map {
                NodeChanges.Attributes
            }
        } else emptyFlow()
}
