package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.FolderTypeData
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.user.UserId

/**
 * Node repository
 *
 */
interface NodeRepository {

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
     * Provides all outgoing shares from SDK with proper sorting and filtering
     *
     * @return List of [ShareData]
     */
    suspend fun getAllOutgoingShares(order: SortOrder): List<ShareData>

    /**
     * Provides both unverified and verified incoming shares from SDK with proper sorting and filtering
     *
     * @return List of [ShareData]
     */
    suspend fun getAllIncomingShares(order: SortOrder): List<ShareData>

    /**
     * check whether the node is in rubbish bin or not
     *
     * @return Boolean
     */
    suspend fun isNodeInRubbishBin(nodeId: NodeId): Boolean

    /**
     * check whether the node is in Backups or not
     *
     * @return Boolean
     */
    suspend fun isNodeInBackups(handle: Long): Boolean

    /**
     * check whether the node is in cloud drive or not
     *
     * @return Boolean
     */
    suspend fun isNodeInCloudDrive(handle: Long): Boolean

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
     * Get parent node id
     *
     * @param nodeId
     * @return
     */
    suspend fun getParentNodeId(nodeId: NodeId): NodeId?

    /**
     * Checks if a Node exists
     *
     * @param nodeId
     * @return true if node exists
     */
    suspend fun doesNodeExist(nodeId: NodeId): Boolean

    /**
     * Get node by its serialized data
     *
     * @param serializedData
     * @return The node if can be un-serialized else null
     */
    suspend fun getNodeFromSerializedData(serializedData: String): UnTypedNode?

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
     * @param nodeId [NodeId]
     * @param order [SortOrder]
     * @return
     */
    @Deprecated("Use getTypedNodesById")
    suspend fun getNodeChildren(nodeId: NodeId, order: SortOrder? = null): List<UnTypedNode>

    /**
     * Get node children
     *
     * @param nodeId [NodeId]
     * @param order [SortOrder]
     * @param folderTypeData [FolderTypeData] Optional data for folder type determination
     * @return
     */
    suspend fun getTypedNodesById(
        nodeId: NodeId,
        order: SortOrder? = null,
        folderTypeData: FolderTypeData? = null,
    ): List<TypedNode>

    /**
     * Get node children in chunks for progressive loading
     *
     * This method returns a Flow that emits:
     * 1. Initial batch of nodes immediately for fast UI display
     * 2. Remaining nodes is emitted later on
     *
     * @param nodeId [NodeId] The parent node ID
     * @param order [SortOrder] Optional sorting order
     * @param initialBatchSize [Int] Size of initial batch (default: 1000)
     * @param folderTypeData [FolderTypeData] Optional data for folder type determination
     * @return Flow of pairs containing typed node lists and hasMore flag for progressive loading
     */
    suspend fun getTypedNodesByIdInChunks(
        nodeId: NodeId,
        order: SortOrder? = null,
        initialBatchSize: Int = 1000,
        folderTypeData: FolderTypeData? = null,
    ): Flow<Pair<List<TypedNode>, Boolean>>

    /**
     * Get node children file types
     * @param nodeId [NodeId]
     * @param order [SortOrder]
     * @return list of [FileTypeInfo]
     */
    suspend fun getNodeChildrenFileTypes(
        nodeId: NodeId,
        order: SortOrder? = null,
    ): List<FileTypeInfo>

    /**
     * Get the history versions of the node
     * @param handle [NodeId] the handle of the node
     * @return the history versions of the node, including current one
     */
    suspend fun getNodeHistoryVersions(handle: NodeId): List<UnTypedNode>

    /**
     * Get [FolderTreeInfo] of the required folder
     * @param folderNode [TypedFolderNode]
     *
     * @return info [FolderTreeInfo]
     */
    suspend fun getFolderTreeInfo(folderNode: TypedFolderNode): FolderTreeInfo

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
     * monitor offline node updates
     */
    fun monitorOfflineNodeUpdates(): Flow<List<Offline>>

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
     * Save offline node information.
     *
     * @param offlineNodeInformation [OfflineNodeInformation]
     * @param parentOfflineInformationId the id of the node's parent offline information
     */
    suspend fun saveOfflineNodeInformation(
        offlineNodeInformation: OfflineNodeInformation,
        parentOfflineInformationId: Long?,
    ): Long

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
     * Start offline files sync worker
     */
    suspend fun startOfflineSyncWorker()

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
     * Gets invalid handle
     */
    suspend fun getInvalidHandle(): Long

    /**
     * Checks if a node is valid
     */
    suspend fun isValidNode(nodeId: NodeId): Boolean

    /**
     * Creates a new share key for the node if there is no share key already created and returns a lambda that can be used to set permissions to this node
     * @param node [FolderNode] whose key will be created
     * @return a suspending lambda to add permissions to the node
     */
    suspend fun createShareKey(node: FolderNode): (suspend (AccessPermission, userEmail: String) -> Unit)?

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
     * @param nodeToCopySerializedData optional node serialized data when a node from link needs to be copied
     * @param newNodeParent the [NodeId] that [nodeToCopy] will be moved to
     * @param newNodeName the new name for [nodeToCopy] once it is moved to [newNodeParent] if it's not null, if it's null the name will be the same
     *
     * @return the [NodeId] handle of the new [Node] that was copied
     */
    suspend fun copyNode(
        nodeToCopy: NodeId,
        nodeToCopySerializedData: String? = null,
        newNodeParent: NodeId,
        newNodeName: String?,
    ): NodeId

    /**
     * Copy a [TypedNode] and move it to a new [Node] while updating its name if set
     *
     * @param nodeToCopy the [TypedNode] to copy
     * @param newNodeParent the [NodeId] that [nodeToCopy] will be moved to
     * @param newNodeName the new name for [nodeToCopy] once it is moved to [newNodeParent] if it's not null, if it's null the name will be the same
     *
     * @return the [NodeId] handle of the new [Node] that was copied
     */
    suspend fun copyNode(
        nodeToCopy: TypedNode,
        newNodeParent: NodeId,
        newNodeName: String?,
    ): NodeId

    /**
     * Copy a public [Node] and move it to a new [Node] while updating its name if set
     *
     * @param publicNodeToCopy the [Node] to copy
     * @param newNodeParent the [NodeId] that [publicNodeToCopy] will be moved to
     * @param newNodeName the new name for [publicNodeToCopy] once it is moved to [newNodeParent] if it's not null, if it's null the name will be the same
     *
     * @return the [NodeId] handle of the new [Node] that was copied
     */
    suspend fun copyPublicNode(
        publicNodeToCopy: Node,
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

    /**
     * Get the fingerprint of a file by path
     *
     * @param filePath
     */
    suspend fun getFingerprint(filePath: String): String?

    /**
     * Get the parent node of a Node
     *
     * @param nodeId [NodeId]
     * @return the parent node of the node, null if node doesn't exist or
     *         is the root node
     */
    suspend fun getParentNode(nodeId: NodeId): UnTypedNode?

    /**
     * Get the root parent node of a Node
     *
     * @param nodeId [NodeId]
     * @return the root parent node of the node
     */
    suspend fun getRootParentNode(nodeId: NodeId): UnTypedNode?

    /**
     * Retrieves the list of Nodes having the same Original Fingerprint
     *
     * @param originalFingerprint The Original Fingerprint to search for all Nodes
     * @param parentNodeId The parent Node Id. if non-null, then it will only return the list of
     * Nodes under the parent Node. Otherwise, it will search for all Nodes in the account
     *
     * @return The list of Nodes having the same Original Fingerprint under the Parent Node.
     * If no Parent Node is specified, it will return the list of Nodes from the entire account
     */
    suspend fun getNodesByOriginalFingerprint(
        originalFingerprint: String,
        parentNodeId: NodeId?,
    ): List<UnTypedNode>

    /**
     * Get [UnTypedNode] by fingerprint and parent node
     * @param fingerprint
     * @param parentNodeId [NodeId]
     * @return [UnTypedNode]
     */
    suspend fun getNodeByFingerprintAndParentNode(
        fingerprint: String,
        parentNodeId: NodeId,
    ): UnTypedNode?

    /**
     * Get [UnTypedNode] by fingerprint only
     * @param fingerprint
     * @return [UnTypedNode]
     */
    suspend fun getNodeByFingerprint(fingerprint: String): UnTypedNode?

    /**
     * Get GPS coordinates from mega node
     *
     * @param nodeId [NodeId]
     *
     * @return a pair with latitude and longitude coordinates
     */
    suspend fun getNodeGPSCoordinates(nodeId: NodeId): Pair<Double, Double>

    /**
     * Get the child node with the provided name
     *
     * @param parentNodeId
     * @param name
     * @return mega node or null if doesn't exist
     */
    suspend fun getChildNode(parentNodeId: NodeId?, name: String?): UnTypedNode?

    /**
     * Sets the original fingerprint of a [Node]
     *
     * @param nodeId the [NodeId] to attach the [originalFingerprint] to
     * @param originalFingerprint the fingerprint of the file before modification
     */
    suspend fun setOriginalFingerprint(
        nodeId: NodeId,
        originalFingerprint: String,
    )

    /**
     * Get node by handle
     *
     * @param handle
     */
    suspend fun getNodeByHandle(handle: Long, attemptFromFolderApi: Boolean = false): UnTypedNode?

    /**
     * Get file attached to a chat message by its chat and message id
     *
     * @param chatId
     * @param messageId
     * @param messageIndex: The index of the file in message attachments, usually 0 since there's usually only one file. Keeping this because the SDK, in theory, allows for multiple files per message.
     */
    suspend fun getNodeFromChatMessage(
        chatId: Long,
        messageId: Long,
        messageIndex: Int = 0,
    ): FileNode?

    /**
     * Get list of files attached to a chat message by its chat and message id
     *
     * @param chatId
     * @param messageId
     */
    suspend fun getNodesFromChatMessage(
        chatId: Long,
        messageId: Long,
    ): List<FileNode>


    /**
     * Get rubbish node
     *
     * @return [UnTypedNode]?
     */
    suspend fun getRubbishNode(): UnTypedNode?

    /**
     * Get the Backups node
     *
     * @return [UnTypedNode]?
     */
    suspend fun getBackupsNode(): UnTypedNode?

    /**
     * MegaApiFolder gets root node
     *
     * @return [UnTypedNode]?
     */
    suspend fun getRootNodeFromMegaApiFolder(): UnTypedNode?

    /**
     * MegaApiFolder gets parent node by handle
     *
     * @param parentHandle node handle
     * @return [UnTypedNode]?
     */
    suspend fun getParentNodeFromMegaApiFolder(parentHandle: Long): UnTypedNode?

    /**
     * Deletes a MegaNode referenced by its handle [NodeId] ONLY if it's already in the rubbish bin
     * @param nodeToDelete the node's handle [NodeId] that we want to delete
     * @throws IllegalArgumentException if the node does not exist or is not in the rubbish bin
     */
    suspend fun deleteNodeByHandle(nodeToDelete: NodeId)


    /**
     * Export a MegaNode referenced by its [NodeId]
     *
     * @param nodeToExport the node's [NodeId] that we want to export
     * @param expireTime the time in seconds since epoch to set as expiry date
     * @return the [String] The link if the request finished with success, error if not
     */
    suspend fun exportNode(
        nodeToExport: NodeId,
        expireTime: Long?,
    ): String

    /**
     * Export node
     *
     * @param node [TypedNode]
     * @return the [String] The public link of the node
     */
    suspend fun exportNode(
        node: TypedNode,
    ): String

    /**
     * Launches a request to stop sharing a file/folder
     *
     * @param nodeToDisable the node's [NodeId] to stop sharing
     */
    suspend fun disableExport(nodeToDisable: NodeId)

    /**
     * Set the GPS coordinates of image files as a node attribute.
     *
     * To remove the existing coordinates, set both the latitude and longitude to
     * the value MegaNode::INVALID_COORDINATE.
     *
     * @param nodeId    Node id that will receive the information.
     * @param latitude  Latitude in signed decimal degrees notation
     * @param longitude Longitude in signed decimal degrees notation
     */
    suspend fun setNodeCoordinates(nodeId: NodeId, latitude: Double, longitude: Double)

    /**
     * Get parent user email from any folder from incoming shares
     *
     * @param nodeId node handle from which we need to get the user email who shared the folder
     * @return null if the node is not found
     */
    suspend fun getIncomingShareParentUserEmail(nodeId: NodeId): String?

    /**
     * Check if a node has an access level
     *
     * @param nodeId  Node to check
     * @param level Access level to check
     *              Valid values for this parameter are:
     *              - AccessPermission.READ
     *              - AccessPermission.READWRITE
     *              - AccessPermission.FUL
     *              - AccessPermission.OWNER
     * @return true if node has the required access
     *         false if node does not have the required access permission
     */
    suspend fun checkIfNodeHasTheRequiredAccessLevelPermission(
        nodeId: NodeId,
        level: AccessPermission,
    ): Boolean

    /**
     * Get all offline Node stored in database
     * @return list of [OfflineNodeInformation]
     */
    suspend fun getAllOfflineNodes(): List<OfflineNodeInformation>

    /**
     * Get offline Nodes from parent id
     * @param parentId
     * @return list of [OfflineNodeInformation]
     */
    suspend fun getOfflineNodesByParentId(parentId: Int): List<OfflineNodeInformation>

    /**
     * Get offline folder info
     * @param parentId
     * @return [OfflineFolderInfo]
     */
    suspend fun getOfflineFolderInfo(parentId: Int): OfflineFolderInfo?

    /**
     * Get offline node from id
     * @param id
     * @return [OfflineNodeInformation]
     */
    suspend fun getOfflineNodeById(id: Int): OfflineNodeInformation?

    /**
     * Remove offline Node by ID
     */
    suspend fun removeOfflineNodeById(id: Int)

    /**
     * Remove offline Node by IDs
     */
    suspend fun removeOfflineNodeByIds(ids: List<Int>)

    /**
     * Get node label
     * @param label Int
     * @return [NodeLabel]
     */
    fun getNodeLabel(label: Int): NodeLabel?

    /**
     * Set label for node
     * @param nodeId [NodeId]
     * @param label Int
     */
    suspend fun setNodeLabel(nodeId: NodeId, label: NodeLabel)

    /**
     * Resets the label for node
     * @param nodeId [NodeId]
     */
    suspend fun resetNodeLabel(nodeId: NodeId)

    /**
     * Update Node to favorite
     */
    suspend fun updateFavoriteNode(nodeId: NodeId, isFavorite: Boolean)

    /**
     * Update Node to sensitive
     */
    suspend fun updateNodeSensitive(nodeId: NodeId, isSensitive: Boolean)

    /**
     * Clear offline
     */
    suspend fun clearOffline()

    /**
     * Moves a MegaNode referenced by its handle [NodeId] to a the rubbish bin
     * @param nodeId the node's handle [NodeId] that we want to move to the rubbish bin
     */
    suspend fun moveNodeToRubbishBinByHandle(nodeId: NodeId)

    /**
     * Get contact verification warning enabled flag
     */
    suspend fun getContactVerificationEnabledWarning(): Boolean

    /**
     * Gets list of NodeLabel
     * @return list of [NodeLabel]
     */
    fun getNodeLabelList(): List<NodeLabel>

    /**
     * Leaves Share folder
     * @param nodeToLeaveShare [NodeId]
     */
    suspend fun leaveShareByHandle(nodeToLeaveShare: NodeId)

    /**
     * Share Node with Email with permission
     * @param nodeId [NodeId]
     * @param email Users' email
     * @param accessPermission [AccessPermission]
     */
    suspend fun shareFolder(nodeId: NodeId, email: String, accessPermission: AccessPermission)

    /**
     * Gets my binary user handle
     * @return user handle [Long]
     */
    suspend fun getMyUserHandleBinary(): Long

    /**
     * Retrieves the list of Nodes having the same Fingerprint
     *
     * @param fingerprint The fingerprint to search for all Nodes
     *
     * @return The list of Nodes having the same Fingerprint from the entire account
     */
    suspend fun getNodesByFingerprint(fingerprint: String): List<UnTypedNode>

    /**
     * Gets owner of node
     * @param nodeId [NodeId]
     * @return owner node [Long]
     */
    suspend fun getOwnerNodeHandle(nodeId: NodeId): Long?

    /**
     * Get local link
     *
     * @param node
     * @return local link [String]
     */
    suspend fun getLocalLink(node: TypedNode): String?

    /**
     * Create a Folder
     *
     * @param name the name of the folder
     * @param parentNodeId Parent node id under which the folder should be created
     *                   If null, the folder will be created in the root folder
     * @return the handle of the new folder
     */
    suspend fun createFolder(name: String, parentNodeId: NodeId?): NodeId

    /**
     * Checks if the folder node contains any files in any of its child nodes
     * @param node the folder node to check
     */
    suspend fun isEmptyFolder(node: TypedNode): Boolean

    /**
     * Set description for a node
     *
     * to remove description of a node we need to pass null as description
     * @param nodeHandle [NodeId]
     * @param description [String]
     */
    suspend fun setNodeDescription(nodeHandle: NodeId, description: String?)

    /**
     * Get offline node information by query
     *
     * When parentId is -1 and searchQuery is set, the search in entire table is performed
     *
     * @param query
     * @param parentId
     * @return list of offline nodes information
     */
    suspend fun getOfflineNodesByQuery(query: String, parentId: Int): List<OfflineNodeInformation>

    /**
     * Update node tag
     *
     * @param nodeHandle [String]
     * @param oldTag [String]
     * @param newTag [String]
     */
    suspend fun updateNodeTag(nodeHandle: NodeId, oldTag: String, newTag: String)

    /**
     * Add node tag
     *
     * @param nodeHandle [String]
     * @param tag [String]
     */
    suspend fun addNodeTag(nodeHandle: NodeId, tag: String)

    /**
     * Remove node tag
     *
     * @param nodeHandle [String]
     * @param tag [String]
     */
    suspend fun removeNodeTag(nodeHandle: NodeId, tag: String)

    /**
     * Check if parent node contains any sensitive descendant
     *
     * @param nodeId [NodeId]
     */
    suspend fun hasSensitiveDescendant(nodeId: NodeId): Boolean

    /**
     * Check if node is sensitive inherited
     *
     * @param nodeId [NodeId]
     */
    suspend fun hasSensitiveInherited(nodeId: NodeId): Boolean

    /**
     * Get all node tags
     *
     * @param searchString [String]
     */
    suspend fun getAllNodeTags(searchString: String): List<String>?

    /**
     * Move or Remove the nodes that used to be part of backup.
     *
     * @param deconfiguredBackupRoot The [NodeId] of the Sync to move or remove
     * @param backupDestination The [NodeId] that [deconfiguredBackupRoot] will be moved to.
     * If INVALID_HANDLE, files will be permanently deleted, otherwise files will be moved there.
     *
     * @return the [NodeId] handle of the Sync that was moved
     */
    suspend fun moveOrRemoveDeconfiguredBackupNodes(
        deconfiguredBackupRoot: NodeId,
        backupDestination: NodeId,
    ): NodeId

    /**
     * Check whether the node is synced or not
     *
     * @return True if node is synced or False otherwise
     */
    suspend fun isNodeSynced(nodeId: NodeId): Boolean

    /**
     * Get all synced node IDs
     *
     * @return Set of node IDs that are synced
     */
    suspend fun getAllSyncedNodeIds(): Set<NodeId>

    /**
     * Remove all versions of a nodes in app
     */
    suspend fun removeAllVersions()

    /**
     * Clean the Rubbish Bin in the MEGA account
     *
     */
    suspend fun cleanRubbishBin()

    /**
     * Get root node id
     *
     * @return The root node id if found else null
     */
    suspend fun getRootNodeId(): NodeId?

    /**
     * Get node name by id
     *
     * @param nodeId
     * @return The node name if found else null
     */
    suspend fun getNodeNameById(nodeId: NodeId): String?
}
