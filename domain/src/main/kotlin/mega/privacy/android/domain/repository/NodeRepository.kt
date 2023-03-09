package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.user.UserId

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
     * check whether the node is in inbox or not
     *
     * @return Boolean
     */
    suspend fun isNodeInInbox(handle: Long): Boolean

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
     * Get the number of versions in node's history
     * @param handle the handle of the node
     * @return the number of history versions or 0 if the file is not found or has no versions
     */
    suspend fun getNodeHistoryNumVersions(handle: Long): Int

    /**
     * Get the history versions of the node
     * @param handle [NodeId] the handle of the node
     * @return the history versions of the node, including current one
     */
    suspend fun getNodeHistoryVersions(handle: NodeId): List<UnTypedNode>

    /**
     * Get [FolderTreeInfo] of the required folder
     * @param folderNode [FolderNode]
     *
     * @return info [FolderTreeInfo]
     */
    suspend fun getFolderTreeInfo(folderNode: FolderNode): FolderTreeInfo

    /**
     * Deletes a MegaNode's history version referenced by its handle [NodeId]
     * @param nodeVersionToDelete [NodeId] handle of the node version we want to delete
     */
    suspend fun deleteNodeVersionByHandle(nodeVersionToDelete: NodeId)

    /**
     * Monitor node updates
     *
     * @return a flow of all global node updates
     */
    fun monitorNodeUpdates(): Flow<NodeUpdate>

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

    /**
     * Convert Base 64 string to handle
     */
    suspend fun convertBase64ToHandle(base64: String): Long

    /**
     * Get offline node information
     *
     * @param nodeHandle
     * @return Offline node information if found
     */
    suspend fun getOfflineNodeInformation(nodeHandle: Long): OfflineNodeInformation?

    /**
     * Get the [UserId] of the owner of the node with [nodeId] if it's a inShare node
     * @param nodeId [NodeId]
     * @param recursive  if true it checks the root of the node with [nodeId],
     * if false it only returns the owner if this [nodeId] represents the root of an in share folder node,
     */
    suspend fun getOwnerIdFromInShare(nodeId: NodeId, recursive: Boolean): UserId?

    /**
     * Monitor update upgrade security events
     *
     * @return
     */
    fun monitorSecurityUpgrade(): Flow<Boolean>

    /**
     * Set upgrade security
     *
     * @param isSecurityUpgrade
     */
    suspend fun setUpgradeSecurity(isSecurityUpgrade: Boolean)

    /**
     * Load offline nodes
     *
     * @param path         Node path
     * @param searchQuery  search query for database
     * @return List of [OfflineNodeInformation]
     */
    suspend fun loadOfflineNodes(
        path: String,
        searchQuery: String?,
    ): List<OfflineNodeInformation>
}
