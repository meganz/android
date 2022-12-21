package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Get Photos from a folder by  its id
 */
interface GetTypedNodesFromFolder {

    /**
     * Get Photos from a folder by  its id
     *
     * @param folderId NodeId
     * @return Photos in the folder
     */
    operator fun invoke(folderId: NodeId): Flow<List<TypedNode>>
}