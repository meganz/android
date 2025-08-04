package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
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
     * @return a flow of NodeChanges
     */
    @OptIn(FlowPreview::class)
    operator fun invoke(nodeId: NodeId) = flow {
        val effectiveNodeId = resolveEffectiveNodeId(nodeId)
        emitAll(
            merge(
                monitorOnlineNodeUpdates(effectiveNodeId),
                monitorOfflineNodeUpdates(effectiveNodeId),
                monitorRefreshSessionUpdates()
            ).conflate().debounce(500L)
        )
    }

    /**
     * Use root node if the provided nodeId is -1, otherwise return the provided nodeId.
     */
    private suspend fun resolveEffectiveNodeId(nodeId: NodeId): NodeId {
        return if (nodeId.longValue == -1L) {
            getRootNodeUseCase()?.id ?: nodeId
        } else {
            nodeId
        }
    }

    private fun monitorOnlineNodeUpdates(effectiveNodeId: NodeId): Flow<NodeChanges> {
        return nodeRepository.monitorNodeUpdates()
            .filter {
                it.changes.keys.any { node ->
                    node.parentId == effectiveNodeId || node.id == effectiveNodeId
                }
            }
            .map {
                val isNodeInRubbishBin = it.changes.keys.any { node ->
                    node is FolderNode && node.id == effectiveNodeId && node.isInRubbishBin
                }
                if (isNodeInRubbishBin) NodeChanges.Remove else NodeChanges.Attributes
            }
    }

    private fun monitorOfflineNodeUpdates(effectiveNodeId: NodeId): Flow<NodeChanges> {
        return monitorOfflineNodeUpdatesUseCase()
            .mapNotNull { offlineList ->
                val relevantOfflineNodes = offlineList.filter { offline ->
                    offline.parentId.toLong() == effectiveNodeId.longValue ||
                            offline.handle.toLong() == effectiveNodeId.longValue
                }
                if (relevantOfflineNodes.isNotEmpty()) NodeChanges.Attributes else null
            }
    }

    private fun monitorRefreshSessionUpdates() = monitorRefreshSessionUseCase().map {
        NodeChanges.Attributes
    }
}
