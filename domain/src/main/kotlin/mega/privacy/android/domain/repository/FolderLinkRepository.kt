package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.FolderInfo
import mega.privacy.android.domain.entity.folderlink.FetchNodeRequestResult
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * FolderLink repository
 */
interface FolderLinkRepository {
    /**
     * Fetch the filesystem in MEGA
     */
    suspend fun fetchNodes(): FetchNodeRequestResult

    /**
     * Update last public handle in local storage
     */
    suspend fun updateLastPublicHandle(nodeHandle: Long)

    /**
     * Log in to a public folder using a folder link
     *
     * @param folderLink Public link to a folder in MEGA
     */
    suspend fun loginToFolder(folderLink: String): FolderLoginStatus

    /**
     * Get the node for given handle
     *
     * @param handle Base 64 handle of the node
     */
    suspend fun getFolderLinkNode(handle: String): UnTypedNode

    /**
     * Get the root node
     */
    suspend fun getRootNode(): UnTypedNode?

    /**
     * Get the parent node
     *
     * @param nodeId Handle of the node of which to get the parent
     */
    suspend fun getParentNode(nodeId: NodeId): UnTypedNode?

    /**
     * Get a child node
     *
     * @param nodeId of the node, it should be a folder link child node
     */
    suspend fun getChildNode(nodeId: NodeId): UnTypedNode?

    /**
     * Get children nodes by handle
     */
    suspend fun getNodeChildren(handle: Long, order: Int? = null): List<UnTypedNode>

    /**
     * Get [FolderInfo] of a public link
     * @param folderLink link of the folder. If link is invalid, exception will be thrown.
     */
    suspend fun getPublicLinkInformation(folderLink: String): FolderInfo

    /**
     * Get [ImageNode]s of a public link
     * @param handle handle of the folder. If handle is invalid, exception will be thrown.
     * @param order order of the nodes. If null, default order will be used.
     */
    suspend fun getFolderLinkImageNodes(handle: Long, order: Int? = null): List<ImageNode>
}