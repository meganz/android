package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

class DefaultMonitorNodeUpdatesById @Inject constructor(
    private val nodeRepository: NodeRepository,
) : MonitorNodeUpdatesById {
    override fun invoke(nodeId: NodeId): Flow<List<NodeChanges>> =
        nodeRepository.monitorNodeUpdates().mapNotNull { update ->
            update.changes.entries.firstOrNull { it.key.id == nodeId }?.value
        }
}