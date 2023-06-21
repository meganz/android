package mega.privacy.android.data.repository

import mega.privacy.android.domain.entity.FolderVersionInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaUser

/**
 * MegaNode repository
 *
 */
interface MegaNodeRepository {

    /**
     * Moves a [MegaNode] to a new [MegaNode] while updating its name if set
     *
     * @param nodeToMove the [MegaNode] to move
     * @param newNodeParent the [MegaNode] that [nodeToMove] will be moved to
     * @param newNodeName the new name for [nodeToMove] if it's not null, if it's null the name will be the same
     *
     * @return the [NodeId] handle of the new [MegaNode] that was moved
     */
    suspend fun moveNode(
        nodeToMove: MegaNode,
        newNodeParent: MegaNode,
        newNodeName: String?,
    ): NodeId

    /**
     * Get folder version info
     *
     * @return info
     */
    @Throws(MegaException::class)
    suspend fun getRootFolderVersionInfo(): FolderVersionInfo

    /**
     * Get the root node
     *
     * @return A node corresponding to the root node, null if cannot be retrieved
     */
    suspend fun getRootNode(): MegaNode?

    /**
     * Get the inbox node
     *
     * @return A node corresponding to the Inbox node, null if cannot be retrieved
     */
    suspend fun getInboxNode(): MegaNode?

    /**
     * check whether the node is in inbox or not
     *
     * @return Boolean
     */
    suspend fun isNodeInInbox(megaNode: MegaNode): Boolean

    /**
     * Get the rubbish root node
     *
     * @return A node corresponding to the rubbish bin node, null if cannot be retrieved
     */
    suspend fun getRubbishBinNode(): MegaNode?

    /**
     * Moves a MegaNode referenced by its handle [NodeId] to a the rubbish bin
     * @param nodeToMove the node's handle [NodeId] that we want to move to the rubbish bin
     */
    suspend fun moveNodeToRubbishBinByHandle(nodeToMove: NodeId)

    /**
     * Check is megaNode in rubbish bin
     *
     * @param node MegaNode
     * @return if node is in rubbish bin
     */
    suspend fun isInRubbish(node: MegaNode): Boolean

    /**
     * Get the parent node of a MegaNode
     *
     * @param node
     * @return the parent node of the node, null if node doesn't exist or
     *         is the root node
     */
    suspend fun getParentNode(node: MegaNode): MegaNode?

    /**
     * Get children of a parent node
     *
     * @param parentNode parent node
     * @param order order for the returned list
     * @return Children nodes of a parent node
     */
    suspend fun getChildrenNode(parentNode: MegaNode, order: SortOrder): List<MegaNode>

    /**
     * Get the node corresponding to a handle
     *
     * @param handle
     */
    suspend fun getNodeByHandle(handle: Long): MegaNode?

    /**
     * Get the node corresponding to a handle
     *
     * @param handle
     */
    suspend fun getPublicNodeByHandle(handle: Long): MegaNode?

    /**
     * Get the MegaNode by path
     *
     * @param path
     * @param megaNode Base node if the path is relative
     * @return megaNode in the path or null
     */
    suspend fun getNodeByPath(path: String?, megaNode: MegaNode?): MegaNode?

    /**
     * Get a list of all incoming shares
     *
     * @param order sort order
     * @return List of MegaNode that other users are sharing with this account
     */
    suspend fun getIncomingSharesNode(order: SortOrder): List<MegaNode>

    /**
     * Get the owner of this node or null if is not an incoming shared node
     * @param node that is being shared
     * @param recursive if true root node of [node] will be checked, if false the [node] itself will be checked
     * @return the owner of this node or null if is not an incoming shared node
     */
    suspend fun getUserFromInShare(node: MegaNode, recursive: Boolean): MegaUser?

    /**
     * Authorize and return a MegaNode can be downloaded with any instance of MegaApi
     *
     * @param handle the handle of the node to authorize
     * @return a MegaNode that can be downloaded with any instance of MegaApi,
     *         null if can't be authorized
     */
    suspend fun authorizeNode(handle: Long): MegaNode?

    /**
     * Get a list with all public links
     *
     * Valid value for order are: MegaApi::ORDER_NONE, MegaApi::ORDER_DEFAULT_ASC,
     * MegaApi::ORDER_DEFAULT_DESC, MegaApi::ORDER_LINK_CREATION_ASC,
     * MegaApi::ORDER_LINK_CREATION_DESC
     *
     * @param order sort order
     * @return List of MegaNode corresponding of a public link
     */
    suspend fun getPublicLinks(order: SortOrder): List<MegaNode>

    /**
     * Check if a MegaNode is pending to be shared with another User. This situation
     * happens when a node is to be shared with a User which is not a contact yet.
     *
     * @param node Node to check
     * @return true is the MegaNode is pending to be shared, otherwise false
     */
    suspend fun isPendingShare(node: MegaNode): Boolean

    /**
     * Checks if Inbox node has children.
     *
     * @return True if Inbox has children, false otherwise.
     */
    suspend fun hasInboxChildren(): Boolean

    /**
     * Check access error extended
     *
     * @param node
     * @param level
     *
     * - [MegaShare.ACCESS_UNKNOWN]
     * - [MegaShare.ACCESS_READ]
     * - [MegaShare.ACCESS_READWRITE]
     * - [MegaShare.ACCESS_FULL]
     * - [MegaShare.ACCESS_OWNER]
     *
     * @return success or failed
     */
    suspend fun checkAccessErrorExtended(node: MegaNode, level: Int): MegaException

    /**
     * Provides searched nodes InShares from query
     * @param query String to be searched
     * @param megaCancelToken [MegaCancelToken]
     */
    suspend fun searchInShares(
        query: String,
        megaCancelToken: MegaCancelToken,
        order: SortOrder,
    ): List<MegaNode>

    /**
     * Provides searched nodes from OutShares from query
     * @param query String to be searched
     * @param megaCancelToken [MegaCancelToken]
     */
    suspend fun searchOutShares(
        query: String,
        megaCancelToken: MegaCancelToken,
        order: SortOrder,
    ): List<MegaNode>


    /**
     * Get a list with the active and pending outbound sharings for a MegaNode
     * @param nodeId the [NodeId] of the node to get the outbound sharings
     * @return a list of [MegaShare] of the outbound sharings of the node
     */
    suspend fun getOutShares(nodeId: NodeId): List<MegaShare>?

    /**
     * Provides searched nodes from Link from query
     * @param query String to be searched
     * @param megaCancelToken [MegaCancelToken]
     */
    suspend fun searchLinkShares(
        query: String,
        megaCancelToken: MegaCancelToken,
        order: SortOrder,
        isFirstLevelNavigation: Boolean,
    ): List<MegaNode>

    /**
     * Search node in parent Node
     * @param parentNode [MegaNode]
     * @param query Query string
     * @param order [SortOrder]
     * @param megaCancelToken [MegaNode]
     */
    suspend fun search(
        parentNode: MegaNode,
        query: String,
        order: SortOrder,
        megaCancelToken: MegaCancelToken,
    ): List<MegaNode>

    /**
     * Creates a new share key for the node if there is no share key already created.
     * @param megaNode : [MegaNode] object which needs to be shared
     */
    suspend fun createShareKey(megaNode: MegaNode)
}
