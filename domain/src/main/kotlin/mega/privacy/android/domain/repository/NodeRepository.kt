package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.node.ViewerNode
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import java.io.File

/**
 * Node repository
 *
 */
interface NodeRepository {
    /**
     * Get a list of all outgoing shares
     *
     * @param order sort order, if null the default order is applied
     * @return List of MegaNode of all active and pending outbound shared by current user
     */
    suspend fun getOutgoingSharesNode(order: SortOrder): List<ShareData>


    /**
     * check whether the node is in rubbish bin or not
     *
     * @return Boolean
     */
    suspend fun isNodeInRubbish(handle: Long): Boolean

    /**
     * Get the current backup folder node id
     */
    suspend fun getBackupFolderId(): NodeId

    /**
     * Get node by id
     *
     * @param nodeId
     * @return The node if found else null
     */
    suspend fun getNodeById(nodeId: NodeId): Node?

    /**
     * Get node children
     *
     * @param folderNode
     * @return
     */
    suspend fun getNodeChildren(folderNode: FolderNode): List<UnTypedNode>

    /**
     * Monitor node updates
     *
     * @return a flow of all global node updates
     */
    fun monitorNodeUpdates(): Flow<List<Node>>

    /**
     * Check if node is in rubbish or deleted
     */
    suspend fun isNodeInRubbishOrDeleted(nodeHandle: Long): Boolean

    /**
     * Get offline node information
     *
     * @param nodeId
     * @return Offline node information if found
     */
    suspend fun getOfflineNodeInformation(nodeId: NodeId): OfflineNodeInformation?
}
