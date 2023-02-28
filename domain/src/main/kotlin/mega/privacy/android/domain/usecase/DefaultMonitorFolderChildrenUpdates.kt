package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Default implementation of [MonitorFolderChildrenUpdates]
 */
class DefaultMonitorFolderChildrenUpdates @Inject constructor(
    private val nodeRepository: NodeRepository,
) : MonitorFolderChildrenUpdates {
    override fun invoke(folder: FolderNode): Flow<NodeUpdate> =
        nodeRepository.monitorNodeUpdates().mapNotNull { update ->
            update.changes.filter {
                var parent = nodeRepository.getNodeById(it.key.parentId)
                while (parent != null) {
                    if (parent.id == folder.id) {
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