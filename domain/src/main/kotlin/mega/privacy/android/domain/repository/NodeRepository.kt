package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.shares.AccessPermission
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
     * Get a list of all outgoing shares for a given node
     *
     * @param nodeId the [NodeId] of the desired node
     * @return List of [ShareData] for the given node
     */
    suspend fun getNodeOutgoingShares(nodeId: NodeId): List<ShareData>

    /**
     * Provides Unverified incoming shares count from SDK
     *
     * @return List of [ShareData]
     */
    suspend fun getUnverifiedIncomingShares(order: SortOrder): List<ShareData>

    /**
     * Provides Unverified outgoing shares count from SDK
     *
     * @return List of [ShareData]
     */
    suspend fun getUnverifiedOutgoingShares(order: SortOrder): List<ShareData>

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
     * Retrieves the Node Path with the provided Node ID
     *
     * @param nodeId [NodeId]
     * @return The Node Path
     */
    suspend fun getNodePathById(nodeId: NodeId): String

    /**
     * Get node children
     *
     * @param folderNode
     * @return
     */
    suspend fun getNodeChildren(folderNode: FolderNode): List<UnTypedNode>

    /**
     * Get node children
     *
     * @param nodeId [NodeId]
     * @param order [SortOrder]
     * @return
     */
    suspend fun getNodeChildren(nodeId: NodeId, order: SortOrder?): List<UnTypedNode>

    /**
     * Get the number of versions of the node, including the current version
     * @param handle the handle of the node
     * @return the number of versions of the node or 0 if the file is not found
     */
    suspend fun getNumVersions(handle: Long): Int

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
     * Get the access level of the node
     * @param nodeId [NodeId]
     * @return the [AccessPermission] enum value for this node
     */
    suspend fun getNodeAccessPermission(nodeId: NodeId): AccessPermission?

    /**
     * Stop sharing a node
     * @param nodeId the [NodeId] of the node we want to stop sharing
     */
    suspend fun stopSharingNode(nodeId: NodeId)

    /**
     * Add, update or remove(access == UNKNOWN) a node's outgoing shared access for one users
     * In case of errors a ShareAccessNotSetException is thrown
     * @param nodeId the [NodeId] of the node we want to change permission
     * @param accessPermission [AccessPermission] that will be set
     * @param email of the user we want to set the permission for this node
     */
    suspend fun setShareAccess(nodeId: NodeId, accessPermission: AccessPermission, email: String)

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

    /**
     * Gets invalid handle
     */
    suspend fun getInvalidHandle(): Long

    /**
     * Creates a new share key for the node if there is no share key already created and returns a lambda that can be used to set permissions to this node
     * @param node [TypedNode] whose key will be created
     * @return a suspending lambda to add permissions to the node
     */
    suspend fun createShareKey(node: TypedNode): (suspend (AccessPermission, userEmail: String) -> Unit)?

    /**
     * Get root node.
     *
     * @return The node if found else null
     */
    suspend fun getRootNode(): Node?

    /**
     * Remove InShares nodes from specific user
     *
     * @param email email of selected user
     */
    suspend fun removedInSharedNodesByEmail(email: String)

    /**
     * Gets list of InShares for the specific user
     *
     * @param email email of the selected user
     * @return [UnTypedNode] returns list of nodes or empty
     */
    suspend fun getInShares(email: String): List<UnTypedNode>


    /**
     * Get FileTypeInfo given NodeId
     *
     * @param nodeId
     * @return FileTypeInfo if found else null
     */
    suspend fun getFileTypeInfo(nodeId: NodeId): FileTypeInfo?


    /**
     * Get default node handle for a folder
     *
     * @param folderName
     * @return [NodeId] if found else null
     */
    suspend fun getDefaultNodeHandle(folderName: String): NodeId?


    /**
     * Checks if node can be moved to target node
     *
     * @param nodeId
     * @param targetNodeId
     *
     * @return true or false
     */
    suspend fun checkNodeCanBeMovedToTargetNode(nodeId: NodeId, targetNodeId: NodeId): Boolean

    /**
     * Copy a [Node] and move it to a new [Node] while updating its name if set
     *
     * @param nodeToCopy the [NodeId] to copy
     * @param newNodeParent the [NodeId] that [nodeToCopy] will be moved to
     * @param newNodeName the new name for [nodeToCopy] once it is moved to [newNodeParent] if it's not null, if it's null the name will be the same
     *
     * @return the [NodeId] handle of the new [Node] that was copied
     */
    suspend fun copyNode(
        nodeToCopy: NodeId,
        newNodeParent: NodeId,
        newNodeName: String?,
    ): NodeId

    /**
     * Move a [Node] to a new [Node] while updating its name if set
     *
     * @param nodeToMove the [NodeId] to move
     * @param newNodeParent the [NodeId] that [nodeToMove] will be moved to
     * @param newNodeName the new name for [nodeToMove] once it is moved to [newNodeParent] if it's not null, if it's null the name will be the same
     *
     * @return the [NodeId] handle of the [Node] that was moved
     */
    suspend fun moveNode(
        nodeToMove: NodeId,
        newNodeParent: NodeId,
        newNodeName: String?,
    ): NodeId
}
