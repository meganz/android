package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeUpdate

/**
 * Monitor updates of the children of a given folder
 */
fun interface MonitorFolderChildrenUpdates {
    /**
     * Invoke
     *
     * @return a flow of changes of the children of a given folder
     */
    operator fun invoke(folder: FolderNode): Flow<NodeUpdate>
}