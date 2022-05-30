package mega.privacy.android.app.data.gateway.api

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.data.model.GlobalUpdate
import nz.mega.sdk.MegaLoggerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaUser

/**
 * Mega api gateway
 *
 * The gateway interface to the Mega Api functionality
 *
 */
interface MegaApiGateway {
    /**
     * Is Multi factor auth available
     *
     * @return true if available, else false
     */
    fun multiFactorAuthAvailable(): Boolean

    /**
     * Is Multi factor auth enabled
     *
     * @param email
     * @param listener
     */
    fun multiFactorAuthEnabled(email: String?, listener: MegaRequestListenerInterface?)

    /**
     * Cancel account
     *
     * @param listener
     */
    fun cancelAccount(listener: MegaRequestListenerInterface?)

    /**
     * Registered email address for the account
     */
    val accountEmail: String?

    /**
     * Is business account
     */
    val isBusinessAccount: Boolean

    /**
     * Is master business account
     */
    val isMasterBusinessAccount: Boolean

    /**
     * Is ephemeral plus plus account.
     */
    val isEphemeralPlusPlus: Boolean

    /**
     * Authentication token that can be used to identify the user account.
     */
    val accountAuth: String

    /**
     * Root node of the account
     *
     * All accounts have a root node, therefore if it is null the account has not been logged in or
     * initialised yet for some reason.
     *
     */
    val rootNode: MegaNode?

    /**
     * Global updates
     */
    val globalUpdates: Flow<GlobalUpdate>

    /**
     * Get favourites
     * @param node Node and its children that will be searched for favourites. Search all nodes if null
     * @param count if count is zero return all favourite nodes, otherwise return only 'count' favourite nodes
     * @param listener MegaRequestListener to track this request
     */
    fun getFavourites(node: MegaNode?, count: Int, listener: MegaRequestListenerInterface?)

    /**
     * Get MegaNode by node handle
     * @param nodeHandle node handle
     * @return MegaNode
     */
    fun getMegaNodeByHandle(nodeHandle: Long): MegaNode

    /**
     * Check the node if has version
     * @param node node that is checked
     * @return true is has version
     */
    fun hasVersion(node: MegaNode): Boolean

    /**
     * Get children nodes by node
     * @param parentNode parent node
     * @return children nodes list
     */
    fun getChildrenByNode(parentNode: MegaNode): ArrayList<MegaNode>

    /**
     * Get child folder number of current folder
     * @param node current folder node
     * @return child folder number
     */
    fun getNumChildFolders(node: MegaNode): Int

    /**
     * Get child files number of current folder
     * @param node current folder node
     * @return child files number
     */
    fun getNumChildFiles(node: MegaNode): Int


    /**
     * Set auto accept contacts from link
     *
     * @param disableAutoAccept pass true to stop auto accepting contacts
     * @param listener
     */
    fun setAutoAcceptContactsFromLink(
            disableAutoAccept: Boolean,
            listener: MegaRequestListenerInterface
    )

    /**
     * Is auto accept contacts from link enabled
     *
     * @param listener
     */
    fun isAutoAcceptContactsFromLinkEnabled(listener: MegaRequestListenerInterface)


    /**
     * Get folder info
     *
     * @param node
     * @param listener
     */
    fun getFolderInfo(node: MegaNode?, listener: MegaRequestListenerInterface)

    /**
     * Add logger
     *
     * @param logger
     */
    fun addLogger(logger: MegaLoggerInterface)

    /**
     * Remove logger
     *
     * @param logger
     */
    fun removeLogger(logger: MegaLoggerInterface)


    /**
     * Set logging level
     *
     * @param logLevel
     */
    fun setLogLevel(logLevel: Int)

    /**
     * Set use https only
     *
     * @param enabled
     */
    fun setUseHttpsOnly(enabled: Boolean)

    /**
     * Get logged in user
     *
     * @return the current user if logged in, otherwise null
     */
    suspend fun getLoggedInUser(): MegaUser?

    /**
     * Get thumbnail from server
     *
     * @param node
     * @param thumbnailFilePath thumbnail file path
     * @param listener
     */
    fun getThumbnail(node: MegaNode, thumbnailFilePath: String, listener: MegaRequestListenerInterface)

    /**
     * Converts the handle of a node to a Base64-encoded string
     *
     * @param handle Node handle to be converted
     * @return Base64-encoded node handle
     */
    fun handleToBase64(handle: Long): String

    /**
     * Registers push notifications.
     *
     * @param deviceType    Type of device.
     * @param newToken      New push token.
     * @param listener      Listener.
     */
    fun registerPushNotifications(deviceType: Int, newToken: String, listener: MegaRequestListenerInterface)

    /**
     * Performs a fast login.
     *
     * @param session   Required for fast login.
     * @param listener  Listener.
     */
    fun fastLogin(session: String, listener: MegaRequestListenerInterface)

    /**
     * Performs fetch nodes.
     *
     * @param listener  Listener.
     */
    fun fetchNodes(listener: MegaRequestListenerInterface)
}