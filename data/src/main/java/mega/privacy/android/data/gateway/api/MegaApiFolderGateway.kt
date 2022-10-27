package mega.privacy.android.data.gateway.api

import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequestListenerInterface

/**
 * Mega api folder gateway
 *
 * The gateway interface to the Mega Api folder functionality
 */
interface MegaApiFolderGateway {
    /**
     * Authentication token that can be used to identify the user account.
     */
    var accountAuth: String

    /**
     * Authorize and return a MegaNode can be downloaded with any instance of MegaApi
     *
     * @param handle the handle of the node to authorize
     * @return a MegaNode that can be downloaded with any instance of MegaApi,
     *         null if can't be authorized
     */
    suspend fun authorizeNode(handle: Long): MegaNode?

    /**
     * Get MegaNode by node handle
     * @param nodeHandle node handle
     * @return MegaNode
     */
    suspend fun getMegaNodeByHandle(nodeHandle: Long): MegaNode?

    /**
     * Returns a MegaNode that can be downloaded with any instance of MegaApi
     *
     * @param node MegaNode to authorize
     * @return Authorized node, or NULL if the node can't be authorized
     */
    suspend fun authorizeNode(node: MegaNode): MegaNode?

    /**
     * Returns a URL to a node in the local HTTP proxy server
     *
     * @param node Node to generate the local HTTP link
     * @return URL to the node in the local HTTP proxy server, otherwise NULL
     */
    suspend fun httpServerGetLocalLink(node: MegaNode): String?

    /**
     * Check if the HTTP proxy server is running
     *
     * @return 0 if the server is not running. Otherwise the port in which it's listening to
     */
    suspend fun httpServerIsRunning(): Int

    /**
     * Start an HTTP proxy server in specified port
     *
     * @return True if the server is ready, false if the initialization failed
     */
    suspend fun httpServerStart(): Boolean

    /**
     * Set the maximum buffer size for the internal buffer
     *
     * @param bufferSize Maximum buffer size (in bytes) or a number <= 0 to use the
     *                   internal default value
     */
    suspend fun httpServerSetMaxBufferSize(bufferSize: Int)

    /**
     * Stop the HTTP proxy server
     */
    suspend fun httpServerStop()

    /**
     * Root node of the account
     *
     * All accounts have a root node, therefore if it is null the account has not been logged in or
     * initialised yet for some reason.
     *
     */
    suspend fun getRootNode(): MegaNode?

    /**
     * Get children nodes by megaNodeList
     * @param parent parent node
     * @param order order for the returned list
     * @return children nodes list
     */
    suspend fun getChildren(parent: MegaNode, order: Int, ): List<MegaNode>

    /**
     * Get thumbnail from server
     *
     * @param node
     * @param thumbnailFilePath thumbnail file path
     * @param listener
     */
    fun getThumbnail(
        node: MegaNode,
        thumbnailFilePath: String,
        listener: MegaRequestListenerInterface? = null,
    )
}