package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Default implementation of [MonitorChildrenUpdates]
 */
class DefaultMonitorChildrenUpdates @Inject constructor(
    private val nodeRepository: NodeRepository,
) : MonitorChildrenUpdates {
    override fun invoke(nodeId: NodeId) =
        nodeRepository.monitorNodeUpdates().mapNotNull { update ->
            update.changes.filter {
                var parent = nodeRepository.getNodeById(it.key.parentId)
                while (parent != null) {
                    if (parent.id == nodeId) {
                        return@filter true
                    }
                    parent = nodeRepository.getNodeById(parent.parentId)
                }
                return@filter false
            }.takeIf { it.isNotEmpty() }?.let {
                NodeUpdate(it)
            }
        }
}