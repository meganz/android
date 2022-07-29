package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.FolderVersionInfo
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare

/**
 * Files repository
 *
 */
interface FilesRepository {
    /**
     * Get folder version info
     *
     * @return info
     */
    @Throws(MegaException::class)
    suspend fun getRootFolderVersionInfo(): FolderVersionInfo

    /**
     * Monitor node updates
     *
     * @return a flow of all global node updates
     */
    fun monitorNodeUpdates(): Flow<List<MegaNode>>

    /**
     * Get the root node
     *
     * @return A node corresponding to the root node, null if cannot be retrieved
     */
    suspend fun getRootNode(): MegaNode?

    /**
     * Get the rubbish root node
     *
     * @return A node corresponding to the rubbish bin node, null if cannot be retrieved
     */
    suspend fun getRubbishBinNode(): MegaNode?

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
    suspend fun getChildrenNode(parentNode: MegaNode, order: Int? = null): List<MegaNode>

    /**
     * Get the node corresponding to a handle
     *
     * @param handle
     */
    suspend fun getNodeByHandle(handle: Long): MegaNode?


    /**
     * Get a list of all incoming shares
     *
     * @param order sort order, if null the default order is applied
     * @return List of MegaNode that other users are sharing with this account
     */
    suspend fun getIncomingSharesNode(order: Int? = null): List<MegaNode>

    /**
     * Get a list of all outgoing shares
     *
     * @param order sort order, if null the default order is applied
     * @return List of MegaNode of all active and pending outbound shared by current user
     */
    suspend fun getOutgoingSharesNode(order: Int? = null): List<MegaShare>

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
     * @param order sort order, if null the default order is applied
     * @return List of MegaNode corresponding of a public link
     */
    suspend fun getPublicLinks(order: Int?): List<MegaNode>?

    /**
     * Get cloud sort order
     * @return cloud sort order
     */
    suspend fun getCloudSortOrder(): Int

    /**
     * Get camera sort order
     * @return camera sort order
     */
    suspend fun getCameraSortOrder(): Int

    /**
     * Get others sort order
     * @return others sort order
     */
    suspend fun getOthersSortOrder(): Int

    /**
     * Get links cloud sort order
     * @return links cloud sort order
     */
    suspend fun getLinksSortOrder(): Int


    /**
     * Checks if Inbox node has children.
     *
     * @return True if Inbox has children, false otherwise.
     */
    suspend fun hasInboxChildren(): Boolean

    /**
     * Downloads a file node in background.
     *
     * @param node  File node to download.
     * @return The local path of the downloaded file.
     */
    suspend fun downloadBackgroundFile(node: MegaNode): String
}
