package mega.privacy.android.app.data.gateway.api

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.data.model.GlobalTransfer
import mega.privacy.android.app.data.model.GlobalUpdate
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaLoggerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferListenerInterface
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert

/**
 * Mega api gateway
 *
 * @constructor Create empty Mega api gateway
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
     * Create support ticket
     *
     * @param ticketContent
     * @param listener
     */
    fun createSupportTicket(
        ticketContent: String,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Start upload for support
     *
     * @param path of file to upload
     * @param listener
     */
    fun startUploadForSupport(
        path: String,
        listener: MegaTransferListenerInterface,
    )

    /**
     * Handle for the account
     */
    val myUserHandle: Long

    /**
     * [MegaUser] for the account
     */
    val myUser: MegaUser?

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
     * Are transfers paused (downloads and uploads)
     */
    suspend fun areTransfersPaused(): Boolean

    /**
     * Are upload transfers paused
     */
    suspend fun areUploadTransfersPaused(): Boolean

    /**
     * Root node of the account
     *
     * All accounts have a root node, therefore if it is null the account has not been logged in or
     * initialised yet for some reason.
     *
     */
    suspend fun getRootNode(): MegaNode?

    /**
     * Get the parent node of a MegaNode
     *
     * @param node
     * @return the parent node of the node, null if node doesn't exist or
     *         is the root node
     */
    suspend fun getParentNode(node: MegaNode): MegaNode?

    /**
     * Get the child node with the provided name
     *
     * @param parentNode
     * @param name
     * @return mega node or null if doesn't exist
     */
    suspend fun getChildNode(parentNode: MegaNode?, name: String?): MegaNode?

    /**
     * Rubbish bin node of the account
     *
     * All accounts have a rubbish bin node, therefore if it is null the account has not been logged in or
     * initialised yet for some reason.
     *
     */
    suspend fun getRubbishBinNode(): MegaNode?

    /**
     * Sdk version
     */
    suspend fun getSdkVersion(): String

    /**
     * Global updates
     */
    val globalUpdates: Flow<GlobalUpdate>

    /**
     * Global transfer
     */
    val globalTransfer: Flow<GlobalTransfer>

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
    suspend fun getMegaNodeByHandle(nodeHandle: Long): MegaNode?

    /**
     * Get the fingerprint of a file by path
     *
     * @param filePath
     */
    suspend fun getFingerprint(filePath: String): String?

    /**
     * Get MegaNode by original fingerprint
     * @param originalFingerprint
     * @param parentNode MegaNode
     * @return MegaNodeList
     */
    suspend fun getNodesByOriginalFingerprint(
        originalFingerprint: String,
        parentNode: MegaNode?,
    ): MegaNodeList?

    /**
     * Get MegaNode by fingerprint and parent node
     * @param fingerprint
     * @param parentNode MegaNode
     * @return MegaNode
     */
    suspend fun getNodeByFingerprintAndParentNode(
        fingerprint: String,
        parentNode: MegaNode?,
    ): MegaNode?

    /**
     * Get MegaNode by fingerprint only
     * @param fingerprint
     * @return MegaNode
     */
    suspend fun getNodeByFingerprint(fingerprint: String): MegaNode?

    /**
     * Check the node if has version
     * @param node node that is checked
     * @return true is has version
     */
    fun hasVersion(node: MegaNode): Boolean

    /**
     * Get children nodes by node
     * @param parentNode parent node
     * @param order order for the returned list, if null the default order is applied
     * @return children nodes list
     */
    suspend fun getChildrenByNode(parentNode: MegaNode, order: Int? = null): List<MegaNode>

    /**
     * Get a list of all incoming shares
     *
     * @param order sort order, if null the default order is applied
     * @return List of MegaNode that other users are sharing with this account
     */
    suspend fun getIncomingSharesNode(order: Int?): List<MegaNode>

    /**
     * Get a list of all outgoing shares
     *
     * @param order sort order, if null the default order is applied
     * @return List of MegaNode of all active and pending outbound shared by current user
     */
    suspend fun getOutgoingSharesNode(order: Int?): List<MegaShare>

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
    suspend fun getPublicLinks(order: Int?): List<MegaNode>

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
        listener: MegaRequestListenerInterface,
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
     * Set node favourite as a node attribute.
     *
     * @param node      Node that will receive the information.
     * @param favourite if true set node as favourite, otherwise remove the attribute
     */
    fun setNodeFavourite(node: MegaNode?, favourite: Boolean)

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
    fun getThumbnail(
        node: MegaNode,
        thumbnailFilePath: String,
        listener: MegaRequestListenerInterface? = null,
    )

    /**
     * Converts the handle of a node to a Base64-encoded string
     *
     * @param handle Node handle to be converted
     * @return Base64-encoded node handle
     */
    fun handleToBase64(handle: Long): String

    /**
     * Converts a Base64-encoded node handle to a handle.
     *
     * @param base64Handle Base64-encoded node handle.
     * @return Node handle.
     */
    fun base64ToHandle(base64Handle: String): Long

    /**
     * Cancel transfer
     *
     * @param transfer to be cancelled
     */
    fun cancelTransfer(transfer: MegaTransfer)

    /**
     * Gets the number of unread user alerts for the logged in user.
     *
     * @return Number of unread user alerts.
     */
    suspend fun getNumUnreadUserAlerts(): Int

    /**
     * Inbox node of the account
     *
     * @return The Inbox node if exists, null otherwise.
     */
    suspend fun getInboxNode(): MegaNode?

    /**
     * Checks if the provided node has children.
     *
     * @param node  The MegaNode to check.
     * @return True if the node has children, false otherwise.
     */
    suspend fun hasChildren(node: MegaNode): Boolean

    /**
     * Registers push notifications.
     *
     * @param deviceType    Type of device.
     * @param newToken      New push token.
     * @param listener      Listener.
     */
    fun registerPushNotifications(
        deviceType: Int,
        newToken: String,
        listener: MegaRequestListenerInterface,
    )

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

    /**
     * Retries all pending requests.
     */
    fun retryPendingConnections()

    /**
     * Gets all transfers of a specific type (downloads or uploads).
     * If the parameter isn't MegaTransfer::TYPE_DOWNLOAD or MegaTransfer::TYPE_UPLOAD
     * this function returns an empty list.
     *
     * @param type MegaTransfer::TYPE_DOWNLOAD or MegaTransfer::TYPE_UPLOAD
     * @return List with transfers of the desired type.
     */
    suspend fun getTransfers(type: Int): List<MegaTransfer>

    /**
     * Starts a download.
     *
     * @param node        MegaNode that identifies the file or folder.
     * @param localPath   Destination path for the file or folder.
     * @param fileName    Custom file name for the file or folder in local destination
     * @param appData     Custom app data to save in the MegaTransfer object.
     * @param startFirst  Puts the transfer on top of the download queue.
     * @param cancelToken MegaCancelToken to be able to cancel a folder/file download process.
     * @param listener    MegaTransferListener to track this transfer.
     */
    fun startDownload(
        node: MegaNode,
        localPath: String,
        fileName: String?,
        appData: String?,
        startFirst: Boolean,
        cancelToken: MegaCancelToken?,
        listener: MegaTransferListenerInterface?,
    )

    /**
     * Get user email
     *
     * @param userHandle
     * @param callback
     */
    fun getUserEmail(userHandle: Long, callback: MegaRequestListenerInterface)

    /**
     * Get contact
     *
     * @param email
     * @return Mega user associated with the email address
     */
    suspend fun getContact(email: String): MegaUser?

    /**
     * Get user alerts
     *
     * @return all user alerts
     */
    suspend fun getUserAlerts(): List<MegaUserAlert>

    /**
     * Send a MEGA Stats event
     *
     * @param eventID
     * @param message
     */
    suspend fun sendEvent(eventID: Int, message: String)

    /**
     * Acknowledge user alerts
     */
    suspend fun acknowledgeUserAlerts()

    /**
     * Get incoming contact requests
     *
     * @return all incoming contact requests or null
     */
    suspend fun getIncomingContactRequests(): ArrayList<MegaContactRequest>?

    /**
     * get user avatar color
     *
     * @param megaUser
     */
    suspend fun getUserAvatarColor(megaUser: MegaUser): String

    /**
     * Get user avatar
     *
     * @param user
     * @param dstPath destination path file
     *
     * @return true if success
     */
    suspend fun getUserAvatar(user: MegaUser, dstPath: String): Boolean

    /**
     * Allow to search nodes with the specific options, [order] & [type] & [target]
     *
     * @param cancelToken
     * @param order
     * @param type
     * @param target
     * @return Mega list
     */
    suspend fun searchByType(
        cancelToken: MegaCancelToken,
        order: Int,
        type: Int,
        target: Int,
    ): List<MegaNode>

    /**
     * Get children nodes by megaNodeList
     * @param parentNodes parent nodes
     * @param order order for the returned list
     * @return children nodes list
     */
    suspend fun getChildren(
        parentNodes: MegaNodeList,
        order: Int,
    ): List<MegaNode>

    /**
     * Get a list with all public links
     *
     * @return List of MegaNode objects that are shared with everyone via public link
     */
    suspend fun getPublicLinks(): List<MegaNode>

    /**
     * Get preview from server
     *
     * @param node
     * @param previewFilePath preview file path
     * @param listener
     */
    fun getPreview(
        node: MegaNode,
        previewFilePath: String,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Check is megaNode in Rubbish bin
     *
     * @param node MegaNode
     * @return True in, else not in
     */
    suspend fun isInRubbish(node: MegaNode): Boolean

    /**
     * Gets all contacts of this MEGA account.
     *
     * @return List of [MegaUser] with all the contacts.
     */
    suspend fun getContacts(): List<MegaUser>

    /**
     * Checks if credentials are verified for the given user.
     *
     * @param megaUser [MegaUser] of the contact whose credentials want to be checked.
     * @return True if verified, false otherwise.
     */
    suspend fun areCredentialsVerified(megaUser: MegaUser): Boolean

    /**
     * Gets a user alias if exists.
     *
     * @param userHandle User handle.
     * @param listener   Listener.
     */
    fun getUserAlias(userHandle: Long, listener: MegaRequestListenerInterface)

    /**
     * Gets the avatar of a contact if exists.
     *
     * @param email_or_handle Email or user handle (Base64 encoded) to get the attribute.
     * @param path            Path in which the avatar will be stored if exists.
     * @param listener        Listener.
     * @return The path of the avatar if exists.
     */
    fun getContactAvatar(
        email_or_handle: String,
        path: String,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Gets an attribute of any user in MEGA.
     *
     * @param email_or_handle Email or user handle (Base64 encoded) to get the attribute.
     * @param type            Attribute type.
     */
    fun getUserAttribute(email_or_handle: String, type: Int, listener: MegaRequestListenerInterface)

    /**
     * Converts a user handle to a Base64-encoded string.
     *
     * @param userHandle User handle.
     * @return Base64-encoded user handle.
     */
    fun userHandleToBase64(userHandle: Long): String

    /**
     * Gets an attribute of any user in MEGA.
     *
     * @param user Email or user handle (Base64 encoded) to get the attribute.
     * @param type Attribute type.
     */
    fun getUserAttribute(user: MegaUser, type: Int, listener: MegaRequestListenerInterface)
}
