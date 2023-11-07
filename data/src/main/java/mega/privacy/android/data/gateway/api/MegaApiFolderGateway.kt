package mega.privacy.android.data.gateway.api

import nz.mega.sdk.MegaCancelToken
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
    suspend fun setAccountAuth(authentication: String?)

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

    /**
     * Fetch the filesystem in MEGA
     *
     * @param listener MegaRequestListener to track this request
     */
    fun fetchNodes(listener: MegaRequestListenerInterface)

    /**
     * Log in to a public folder using a folder link
     *
     * @param folderLink Public link to a folder in MEGA
     * @param listener   MegaRequestListener to track this request
     */
    fun loginToFolder(folderLink: String, listener: MegaRequestListenerInterface)

    /**
     * Remove request listener
     *
     * @param listener MegaRequestListener to remove
     */
    fun removeRequestListener(listener: MegaRequestListenerInterface)

    /**
     * Get child folder number of current folder
     * @param node current folder node
     * @return child folder number
     */
    suspend fun getNumChildFolders(node: MegaNode): Int

    /**
     * Get child files number of current folder
     * @param node current folder node
     * @return child files number
     */
    suspend fun getNumChildFiles(node: MegaNode): Int

    /**
     * Get children nodes by node
     * @param parentNode parent node
     * @param order order for the returned list, if null the default order is applied
     * @return children nodes list
     */
    suspend fun getChildrenByNode(parentNode: MegaNode, order: Int? = null): List<MegaNode>

    /**
     * Get the parent node of a MegaNode
     *
     * @param node
     * @return the parent node of the node, null if node doesn't exist
     */
    suspend fun getParentNode(node: MegaNode): MegaNode?

    /**
     *
     * Allow to search nodes with the following options:
     * - Search given a parent node of the tree to explore
     * - Search recursively
     * - Containing a search string in their name
     * - Filter by the type of the node
     * - Order the returned list
     *
     * @param parentNode parentNode
     * @param searchString containing a search string in their name
     * @param cancelToken use for cancel search
     * @param recursive is search recursively
     * @param order
     * @param type type of nodes requested in the search
     *
     * @return List of nodes that match with the search parameters
     */
    suspend fun searchByType(
        parentNode: MegaNode,
        searchString: String,
        cancelToken: MegaCancelToken,
        recursive: Boolean,
        order: Int,
        type: Int,
    ): List<MegaNode>

    /**
     * Retrieve basic information about a folder link
     * This function retrieves basic information from a folder link, like the
     * number of files / folders and the name of the folder. For folder links containing
     * a lot of files/folders, this function is more efficient than a fetchNodes.
     *
     * Valid data in the MegaRequest object received on all callbacks:
     * - MegaRequest::getLink() - Returns the public link to the folder
     * Valid data in the MegaRequest object received in onRequestFinish when the error code is MegaError::API_OK:
     * - MegaRequest::getMegaFolderInfo() - Returns information about the contents of the folder
     * - MegaRequest::getNodeHandle() - Returns the public handle of the folder
     * - MegaRequest::getParentHandle() - Returns the handle of the owner of the folder
     * - MegaRequest::getText() - Returns the name of the folder. If there's no name, it returns the special status string "CRYPTO_ERROR". If the length of the name is zero, it returns the special status string "BLANK".
     *
     * On the onRequestFinish error, the error code associated to the MegaError can be:
     * - MegaError::API_EARGS - If the link is not a valid folder link - MegaError::API_EKEY - If the public link does not contain the key or it is invalid
     *
     * @param megaFolderLink – Public link to a folder in MEGA
     * @param listener – MegaRequestListener to track this request
     */
    fun getPublicLinkInformation(megaFolderLink: String, listener: MegaRequestListenerInterface)

    /**
     * Enable / disable the public key pinning
     *
     * Public key pinning is enabled by default for all sensible communications.
     * It is strongly discouraged to disable this feature.
     *
     * @param enable True to keep public key pinning enabled, false to disable it
     */
    suspend fun setPublicKeyPinning(enable: Boolean)

    /**
     * Change the API URL
     *
     * This function allows to change the API URL.
     * It's only useful for testing or debugging purposes.
     *
     * @param apiURL     New API URL
     * @param disablePkp True to disable public key pinning for this URL
     */
    suspend fun changeApiUrl(apiURL: String, disablePkp: Boolean)
}