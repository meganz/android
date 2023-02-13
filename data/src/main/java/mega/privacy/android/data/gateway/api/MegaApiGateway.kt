package mega.privacy.android.data.gateway.api

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.data.model.GlobalUpdate
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaLoggerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElementList
import nz.mega.sdk.MegaSetList
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferListenerInterface
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import java.io.File

/**
 * Mega api gateway
 *
 * @constructor Create empty Mega api gateway
 */
interface MegaApiGateway {

    /**
     * Get Invalid Handle
     */
    fun getInvalidHandle(): Long

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
     * Upload a file or folder
     *
     * @param localPath The local path of the file or folder
     * @param parentNode The parent node for the file or folder
     * @param fileName The custom file name for the file or folder. Leave the parameter as "null"
     * if there are no changes
     * @param modificationTime The custom modification time for the file or folder, denoted in
     * seconds since the epoch
     * @param appData The custom app data to save, which can be nullable
     * @param isSourceTemporary Whether the temporary file or folder that is created for upload
     * should be deleted or not
     * @param shouldStartFirst Whether the file or folder should be placed on top of the upload
     * queue or not
     * @param cancelToken The token to cancel an ongoing file or folder upload, which can be
     * nullable
     * @param listener The [MegaTransferListenerInterface] to track the upload
     */
    fun startUpload(
        localPath: String,
        parentNode: MegaNode,
        fileName: String?,
        modificationTime: Long,
        appData: String?,
        isSourceTemporary: Boolean,
        shouldStartFirst: Boolean,
        cancelToken: MegaCancelToken?,
        listener: MegaTransferListenerInterface,
    )

    /**
     * Adds a [MegaTransferListenerInterface] to listen for Transfer events
     *
     * @param listener [MegaTransferListenerInterface]
     */
    fun addTransferListener(listener: MegaTransferListenerInterface)

    /**
     * Removes [MegaTransferListenerInterface] to stop listening for Transfer events
     *
     * @param listener [MegaTransferListenerInterface]
     */
    fun removeTransferListener(listener: MegaTransferListenerInterface)

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
     * Fingerprint of the signing key of the current account
     */
    val myCredentials: String?

    /**
     * Current session key.
     */
    val dumpSession: String?

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
     * Get the MegaNode by path
     *
     * @param path
     * @param megaNode Base node if the path is relative
     * @return megaNode in the path or null
     */
    suspend fun getNodeByPath(path: String?, megaNode: MegaNode?): MegaNode?

    /**
     * Get the fingerprint of a file by path
     *
     * @param filePath file path
     * @return fingerprint
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
     * Sets the original fingerprint of a [MegaNode]
     *
     * @param node the [MegaNode] to attach the [originalFingerprint] to
     * @param originalFingerprint the fingerprint of the file before modification
     * @param listener the [MegaRequestListenerInterface] for callback events. It can be nullable
     */
    fun setOriginalFingerprint(
        node: MegaNode,
        originalFingerprint: String,
        listener: MegaRequestListenerInterface?,
    )

    /**
     * Check the node if has version
     * @param node node that is checked
     * @return true is has version
     */
    suspend fun hasVersion(node: MegaNode): Boolean

    /**
     * Get node history num versions
     */
    suspend fun getNumVersions(node: MegaNode): Int

    /**
     * Returns the list of versions of [node]
     */
    suspend fun getVersions(node: MegaNode): List<MegaNode>

    /**
     * Deletes [nodeVersion]
     * @param nodeVersion MegaNode
     * @param listener a [MegaRequestListenerInterface] for callback purposes
     */
    fun deleteVersion(
        nodeVersion: MegaNode,
        listener: MegaRequestListenerInterface,
    )

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
     * Check if a MegaNode is pending to be shared with another User. This situation
     * happens when a node is to be shared with a User which is not a contact yet.
     *
     * @param node Node to check
     * @return true is the MegaNode is pending to be shared, otherwise false
     */
    suspend fun isPendingShare(node: MegaNode): Boolean

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
    suspend fun getNumChildFolders(node: MegaNode): Int

    /**
     * Get child files number of current folder
     * @param node current folder node
     * @return child files number
     */
    suspend fun getNumChildFiles(node: MegaNode): Int


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
     * Cancels a [MegaTransfer]
     *
     * @param transfer the [MegaTransfer] to cancel
     * @param listener a [MegaRequestListenerInterface] for callback purposes. It can be nullable
     */
    fun cancelTransfer(transfer: MegaTransfer, listener: MegaRequestListenerInterface?)

    /**
     * Cancels all [MegaTransfer] uploads
     *
     * @param listener a [MegaRequestListenerInterface] for callback purposes. It can be nullable
     */
    fun cancelAllUploadTransfers(listener: MegaRequestListenerInterface?)

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
     * Get the transfer with a transfer tag
     * That tag can be got using MegaTransfer::getTag
     * You take the ownership of the returned value
     *
     * @param tag tag to check
     * @return MegaTransfer object with that tag, or NULL if there isn't any
     * active transfer with it
     */
    suspend fun getTransfersByTag(tag: Int): MegaTransfer?

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
     * Get the default color for the avatar
     *
     * @param megaUser
     * @return The RGB color as a string with 3 components in hex: #RGB. Ie. "#FF6A19"
     */
    suspend fun getUserAvatarColor(megaUser: MegaUser): String

    /**
     * Get the default color for the avatar
     *
     * @param userHandle
     * @return The RGB color as a string with 3 components in hex: #RGB. Ie. "#FF6A19"
     */
    suspend fun getUserAvatarColor(userHandle: Long): String

    /**
     * Get user avatar
     *
     * @param user
     * @param destinationPath destination path file
     *
     * @return true if success
     */
    suspend fun getUserAvatar(user: MegaUser, destinationPath: String): Boolean

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
     * Get children nodes by megaNodeList
     * @param parent parent node
     * @param order order for the returned list
     * @return children nodes list
     */
    suspend fun getChildren(parent: MegaNode, order: Int): List<MegaNode>

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
     * Get Full image from server
     *
     * @param node
     * @param fullFile
     * @param highPriority
     * @param listener
     */
    fun getFullImage(
        node: MegaNode,
        fullFile: File,
        highPriority: Boolean,
        listener: MegaTransferListenerInterface,
    )

    /**
     * Check is megaNode in Rubbish bin
     *
     * @param node MegaNode
     * @return True in, else not in
     */
    suspend fun isInRubbish(node: MegaNode): Boolean

    /**
     * Check is megaNode in Inbox
     *
     * @param node MegaNode
     * @return True in, else not in
     */
    suspend fun isInInbox(node: MegaNode): Boolean

    /**
     * Move a transfer to the top of the transfer queue
     *
     * @param transfer Transfer to move
     * @param listener MegaRequestListener to track this request
     */
    suspend fun moveTransferToFirst(transfer: MegaTransfer, listener: MegaRequestListenerInterface)

    /**
     * Move a transfer to the bottom of the transfer queue
     *
     * @param transfer Transfer to move
     * @param listener MegaRequestListener to track this request
     */
    suspend fun moveTransferToLast(transfer: MegaTransfer, listener: MegaRequestListenerInterface)

    /**
     * Move a transfer before another one in the transfer queue
     *
     * @param transfer     Transfer to move
     * @param prevTransfer Transfer with the target position
     * @param listener     MegaRequestListener to track this request
     */
    suspend fun moveTransferBefore(
        transfer: MegaTransfer,
        prevTransfer: MegaTransfer,
        listener: MegaRequestListenerInterface,
    )

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
     * @param emailOrHandle Email or user handle (Base64 encoded) to get the attribute.
     * @param path          Path in which the avatar will be stored if exists.
     * @param listener      Listener.
     * @return The path of the avatar if exists.
     */
    fun getContactAvatar(
        emailOrHandle: String,
        path: String,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Gets an attribute of any user in MEGA.
     *
     * @param emailOrHandle Email or user handle (Base64 encoded) to get the attribute.
     * @param type          Attribute type.
     */
    fun getUserAttribute(emailOrHandle: String, type: Int, listener: MegaRequestListenerInterface)

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

    /**
     * Get the list of recent actions
     *
     * @param days     Age of actions since added/modified nodes will be considered (in days).
     * @param maxNodes Maximum amount of nodes to be considered.
     * @param listener [MegaRequestListenerInterface]
     */
    fun getRecentActionsAsync(
        days: Long,
        maxNodes: Long,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Copy a [MegaNode] and move it to a new [MegaNode] while updating its name if set
     *
     * @param nodeToCopy the [MegaNode] to copy
     * @param newNodeParent the [MegaNode] that [nodeToCopy] will be moved to
     * @param newNodeName the new name for [nodeToCopy] once it is moved to [newNodeParent] if it's not null, if it's null the name will be the same
     * @param listener a [MegaRequestListenerInterface] for callback purposes. It can be nullable
     */
    fun copyNode(
        nodeToCopy: MegaNode,
        newNodeParent: MegaNode,
        newNodeName: String?,
        listener: MegaRequestListenerInterface?,
    )

    /**
     * Moves a [MegaNode] to a new [MegaNode] while updating its name if set
     *
     * @param nodeToMove the [MegaNode] to move
     * @param newNodeParent the [MegaNode] that [nodeToMove] will be moved to
     * @param newNodeName the new name for [nodeToMove] if it's not null, if it's null the name will be the same
     * @param listener a [MegaRequestListenerInterface] for callback purposes. It can be nullable
     */
    fun moveNode(
        nodeToMove: MegaNode,
        newNodeParent: MegaNode,
        newNodeName: String?,
        listener: MegaRequestListenerInterface?,
    )

    /**
     * Deletes the node if it's already in the rubbish bin
     * @param node MegaNode
     * @param listener a [MegaRequestListenerInterface] for callback purposes. It can be nullable
     */
    fun deleteNode(
        node: MegaNode,
        listener: MegaRequestListenerInterface?,
    )

    /**
     * Creates a copy of MegaRecentActionBucket required for its usage in the app.
     *
     * @param bucket The MegaRecentActionBucket received.
     * @return A copy of MegaRecentActionBucket.
     */
    fun copyBucket(bucket: MegaRecentActionBucket): MegaRecentActionBucket

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
    fun checkAccessErrorExtended(node: MegaNode, level: Int): MegaError

    /**
     * Checks whether the user's Business Account is currently active or not
     *
     * @return True if the user's Business Account is currently active, or
     * false if inactive or if the user is not under a Business Account
     */
    suspend fun isBusinessAccountActive(): Boolean

    /**
     * Get pricing
     *
     * @param listener
     */
    fun getPricing(listener: MegaRequestListenerInterface?)

    /**
     * Get payment methods
     *
     * @param listener
     */
    fun getPaymentMethods(listener: MegaRequestListenerInterface?)

    /**
     * Get account details
     */
    fun getAccountDetails(listener: MegaRequestListenerInterface?)

    /**
     * Get specific account details
     *
     * @param storage
     * @param transfer
     * @param pro
     */
    fun getSpecificAccountDetails(
        storage: Boolean,
        transfer: Boolean,
        pro: Boolean,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Get the credit card subscriptions of the account
     *
     * @param listener
     */
    fun creditCardQuerySubscriptions(listener: MegaRequestListenerInterface?)

    /**
     * Get the selected user attribute for the logged in user
     */
    fun getUserAttribute(attributeIdentifier: Int, listener: MegaRequestListenerInterface)

    /**
     * Returns if accounts achievements enabled
     */
    suspend fun areAccountAchievementsEnabled(): Boolean

    /**
     * Get account achievements
     *
     * @param listener : MegaRequestListenerInterface
     */
    fun getAccountAchievements(listener: MegaRequestListenerInterface?)

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
     * Stop the HTTP proxy server
     */
    suspend fun httpServerStop()

    /**
     * Set the maximum buffer size for the internal buffer
     *
     * @param bufferSize Maximum buffer size (in bytes) or a number <= 0 to use the
     *                   internal default value
     */
    suspend fun httpServerSetMaxBufferSize(bufferSize: Int)

    /**
     * Get a list with all public links
     *
     * @param order Sorting order to use
     * @return List of MegaNode objects that are shared with everyone via public link
     */
    suspend fun getPublicLinks(order: Int): List<MegaNode>

    /**
     * Get a list with all inbound sharings
     *
     * @param order Sorting order to use
     * @return List of MegaNode objects that other users are sharing with this account
     */
    suspend fun getInShares(order: Int): List<MegaNode>

    /**
     * Get a list with all inbound sharings from one MegaUser
     *
     * @param user MegaUser sharing folders with this account
     * @return List of MegaNode objects that this user is sharing with this account
     */
    suspend fun getInShares(user: MegaUser): List<MegaNode>

    /**
     * Get a list with all active and pending outbound sharings
     *
     * @param order Sorting order to use
     * @return List of MegaShare objects
     */
    suspend fun getOutShares(order: Int): List<MegaShare>

    /**
     * Returns the rubbish node of the account.
     *
     * @return Rubbish node of the account.
     */
    suspend fun getRubbishNode(): MegaNode

    /**
     * Create a new MegaSet item
     *
     * @param name the name of the set
     * @param listener [MegaRequestListenerInterface]
     */
    fun createSet(name: String, listener: MegaRequestListenerInterface)

    /**
     * Create a new element for the set
     *
     * @param sid the ID of the set
     * @param node the node handle of the node which will be assigned as the set's new element
     * @param listener MegaRequestListener to track this request
     */
    fun createSetElement(sid: Long, node: Long, listener: MegaRequestListenerInterface)

    /**
     * Remove an element from a set
     *
     * @param sid the ID of the set
     * @param eid the SetElement ID that will be removed
     */
    suspend fun removeSetElement(sid: Long, eid: Long)

    /**
     * Get a list of all Sets available for current user.
     * The response value is stored as a MegaSetList.
     * You take the ownership of the returned value
     *
     * @return list of Sets
     */
    suspend fun getSets(): MegaSetList

    /**
     * Get the Set with the given id, for current user.
     * The response value is stored as a MegaSet.
     * You take the ownership of the returned value
     *
     * @param sid the id of the Set to be retrieved
     * @return the requested Set, or null if not found
     */
    suspend fun getSet(sid: Long): MegaSet?

    /**
     * Get all Elements in the Set with given id, for current user.
     * The response value is stored as a MegaSetElementList.
     *
     * @param sid the id of the Set owning the Elements
     * @return all Elements in that Set, or null if not found or none added
     */
    suspend fun getSetElements(sid: Long): MegaSetElementList

    /**
     * Request to remove a Set
     *
     * The associated request type with this request is MegaRequest::TYPE_REMOVE_SET
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParentHandle - Returns id of the Set to be removed
     *
     * On the onRequestFinish error, the error code associated to the MegaError can be:
     * - MegaError::API_ENOENT - Set could not be found.
     * - MegaError::API_EINTERNAL - Received answer could not be read.
     * - MegaError::API_EARGS - Malformed (from API).
     * - MegaError::API_EACCESS - Permissions Error (from API).
     *
     * @param sid the id of the Set to be removed
     * @param listener MegaRequestListener to track this request
     */
    fun removeSet(sid: Long, listener: MegaRequestListenerInterface)

    /**
     * Request to update the name of a Set
     *
     *
     * The associated request type with this request is MegaRequest::TYPE_PUT_SET
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParentHandle - Returns id of the Set to be updated
     * - MegaRequest::getText - Returns new name of the Set
     * - MegaRequest::getParamType - Returns OPTION_SET_NAME
     *
     *
     * On the onRequestFinish error, the error code associated to the MegaError can be:
     * - MegaError::API_ENOENT - Set with the given id could not be found (before or after the request).
     * - MegaError::API_EINTERNAL - Received answer could not be read.
     * - MegaError::API_EARGS - Malformed (from API).
     * - MegaError::API_EACCESS - Permissions Error (from API).
     *
     * @param sid      the id of the Set to be updated
     * @param name     the new name that should be given to the Set
     * @param listener MegaRequestListener to track this request
     */
    fun updateSetName(sid: Long, name: String?, listener: MegaRequestListenerInterface?)

    /**
     * Request to update the name of a Set
     *
     *
     * The associated request type with this request is MegaRequest::TYPE_PUT_SET
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParentHandle - Returns id of the Set to be updated
     * - MegaRequest::getText - Returns new name of the Set
     * - MegaRequest::getParamType - Returns OPTION_SET_NAME
     *
     *
     * On the onRequestFinish error, the error code associated to the MegaError can be:
     * - MegaError::API_ENOENT - Set with the given id could not be found (before or after the request).
     * - MegaError::API_EINTERNAL - Received answer could not be read.
     * - MegaError::API_EARGS - Malformed (from API).
     * - MegaError::API_EACCESS - Permissions Error (from API).
     *
     * @param sid  the id of the Set to be updated
     * @param name the new name that should be given to the Set
     */
    fun updateSetName(sid: Long, name: String?)

    /**
     * Request to update the cover of a Set
     *
     * The associated request type with this request is MegaRequest::TYPE_PUT_SET
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParentHandle - Returns id of the Set to be updated
     * - MegaRequest::getNodeHandle - Returns Element id to be set as the new cover
     * - MegaRequest::getParamType - Returns OPTION_SET_COVER
     *
     * On the onRequestFinish error, the error code associated to the MegaError can be:
     * - MegaError::API_EARGS - Given Element id was not part of the current Set; Malformed (from API).
     * - MegaError::API_ENOENT - Set with the given id could not be found (before or after the request).
     * - MegaError::API_EINTERNAL - Received answer could not be read.
     * - MegaError::API_EACCESS - Permissions Error (from API).
     *
     * @param sid the id of the Set to be updated
     * @param eid the id of the Element to be set as cover
     */
    suspend fun putSetCover(sid: Long, eid: Long)

    /**
     * Remove request listener
     */
    fun removeRequestListener(listener: MegaRequestListenerInterface)

    /**
     * Gets the credentials of a given user.
     *
     * @param user     MegaUser of a contact.
     * @param listener MegaRequestListener to track this request.
     */
    fun getUserCredentials(user: MegaUser, listener: MegaRequestListenerInterface)

    /**
     * Resets credentials of a given user
     *
     * @param user     MegaUser of a contact.
     * @param listener MegaRequestListener to track this request.
     */
    fun resetCredentials(user: MegaUser, listener: MegaRequestListenerInterface)

    /**
     * Verifies credentials of a given user.
     *
     * @param user     MegaUser of a contact.
     * @param listener MegaRequestListener to track this request.
     */
    fun verifyCredentials(user: MegaUser, listener: MegaRequestListenerInterface)

    /**
     * Check the current password availability
     * @param password as password to check
     * @return true if password is the same as current password, else false
     */
    suspend fun isCurrentPassword(password: String): Boolean

    /**
     * Change the given user's password
     * @param newPassword as user's chosen new password
     * @param listener as [MegaRequestListenerInterface]
     */
    fun changePassword(newPassword: String, listener: MegaRequestListenerInterface)

    /**
     * Reset the user's password from a link
     * @param link as reset link
     * @param newPassword as user's chosen new password
     * @param masterKey as user's account master key
     * @param listener as [MegaRequestListenerInterface]
     */
    fun resetPasswordFromLink(
        link: String?,
        newPassword: String,
        masterKey: String?,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Check the given password's strength
     * @param password as password to test
     * @return password strength level from 0 - 4
     */
    suspend fun getPasswordStrength(password: String): Int

    /**
     * Requests the currently available country calling codes
     *
     * @param listener [MegaRequestListenerInterface] to track this request
     */
    fun getCountryCallingCodes(listener: MegaRequestListenerInterface)

    /**
     * Logout of the MEGA account invalidating the session
     *
     * @param listener [MegaRequestListenerInterface] to track this request
     */
    fun logout(listener: MegaRequestListenerInterface?)

    /**
     * Provide a phone number to get verification code.
     *
     * @param phoneNumber the phone number to receive the txt with verification code.
     * @param reVerifyingWhitelisted to check whether to re verify whitelisted
     * @param listener [MegaRequestListenerInterface]    callback of this request.
     */
    fun sendSMSVerificationCode(
        phoneNumber: String,
        reVerifyingWhitelisted: Boolean,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Reset the verified phone number for the account logged in.
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_RESET_SMS_VERIFIED_NUMBER
     * If there's no verified phone number associated for the account logged in, the error code
     * provided in onRequestFinish is MegaError::API_ENOENT.
     *
     * @param listener [MegaRequestListenerInterface] to track this request
     */
    fun resetSmsVerifiedPhoneNumber(listener: MegaRequestListenerInterface?)


    /**
     * Get extended account details
     *
     * @param sessions
     * @param purchases
     * @param transactions
     * @param listener
     */
    fun getExtendedAccountDetails(
        sessions: Boolean,
        purchases: Boolean,
        transactions: Boolean,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Create a contact link
     *
     * @param renew – True to invalidate the previous contact link (if any).
     * @param listener – MegaRequestListener to track this request
     */
    fun contactLinkCreate(renew: Boolean, listener: MegaRequestListenerInterface)

    /**
     * Delete a contact link
     *
     * @param handle   Handle of the contact link to delete
     *                 If the parameter is INVALID_HANDLE, the active contact link is deleted
     * @param listener MegaRequestListener to track this request
     *
     */
    fun contactLinkDelete(handle: Long, listener: MegaRequestListenerInterface)

    /**
     * Returns whether notifications about a chat have to be generated.
     *
     * @param chatId    Chat id
     * @return          True if notifications has to be created, false otherwise.
     */
    fun isChatNotifiable(chatId: Long): Boolean

    /**
     * Invite contact
     *
     * @param email     User email
     * @param listener  MegaRequestListener to track this request
     */
    fun inviteContact(email: String, listener: MegaRequestListenerInterface)

    /**
     * Invite contact
     *
     * @param email     User email
     * @param message   Message
     * @param handle    User handle
     * @param listener  MegaRequestListener to track this request
     */
    fun inviteContact(
        email: String,
        handle: Long,
        message: String?,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Get outgoing contact requests
     *
     * @return list of [MegaContactRequest]
     */
    fun outgoingContactRequests(): ArrayList<MegaContactRequest>

    /**
     * Create a folder in the MEGA account
     *
     *
     * The associated request type with this request is MegaRequest::TYPE_CREATE_FOLDER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParentHandle - Returns the handle of the parent folder
     * - MegaRequest::getName - Returns the name of the new folder
     *
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getNodeHandle - Handle of the new folder
     * - MegaRequest::getFlag - True if target folder (\c parent) was overridden
     *
     *
     * If the MEGA account is a business account and it's status is expired, onRequestFinish will
     * be called with the error code MegaError::API_EBUSINESSPASTDUE.
     *
     * @param name     Name of the new folder
     * @param parent   Parent folder
     * @param listener MegaRequestListener to track this request
     */
    fun createFolder(name: String, parent: MegaNode, listener: MegaRequestListenerInterface)

    /**
     * Set Camera Uploads for both primary and secondary target folder.
     *
     *
     * If only one of the target folders wants to be set, simply pass a INVALID_HANDLE to
     * as the other target folder and it will remain untouched.
     *
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_CAMERA_UPLOADS_FOLDER
     * - MegaRequest::getNodehandle - Returns the provided node handle for primary folder
     * - MegaRequest::getParentHandle - Returns the provided node handle for secondary folder
     *
     * @param primaryFolder   MegaHandle of the node to be used as primary target folder
     * @param secondaryFolder MegaHandle of the node to be used as secondary target folder
     * @param listener        MegaRequestListener to track this request
     */
    fun setCameraUploadsFolders(
        primaryFolder: Long,
        secondaryFolder: Long,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Rename a node in the MEGA account
     *
     * @param node     Node to modify
     * @param newName  New name for the node
     * @param listener MegaRequestListener to track this request
     */
    fun renameNode(node: MegaNode, newName: String, listener: MegaRequestListenerInterface)

    /**
     * Gets a MegaNode that can be downloaded/copied with a chat-authorization
     *
     * During preview of chat-links, you need to call this method to authorize the MegaNode
     * from a node-attachment message, so the API allows to access to it. The parameter to
     * authorize the access can be retrieved from MegaChatRoom::getAuthorizationToken when
     * the chatroom in in preview mode.
     *
     * @param node               MegaNode to authorize
     * @param authorizationToken Authorization token (public handle of the chatroom in B64url encoding)
     * @return Authorized node, or NULL if the node can't be authorized
     */
    fun authorizeChatNode(node: MegaNode, authorizationToken: String): MegaNode?

    /**
     * Submit a purchase receipt for verification
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_SUBMIT_PURCHASE_RECEIPT
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNumber - Returns the payment gateway
     * - MegaRequest::getText - Returns the purchase receipt
     *
     * @param gateway  Payment gateway
     *                 Currently supported payment gateways are:
     *                 - MegaApi::PAYMENT_METHOD_ITUNES = 2
     *                 - MegaApi::PAYMENT_METHOD_GOOGLE_WALLET = 3
     *                 - MegaApi::PAYMENT_METHOD_WINDOWS_STORE = 13
     * @param receipt  Purchase receipt
     * @param listener MegaRequestListener to track this request
     */
    fun submitPurchaseReceipt(
        gateway: Int,
        receipt: String?,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Submit a purchase receipt for verification
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_SUBMIT_PURCHASE_RECEIPT
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNumber - Returns the payment gateway
     * - MegaRequest::getText - Returns the purchase receipt
     * - MegaRequest::getNodeHandle - Returns the last public node handle accessed
     * - MegaRequest::getParamType - Returns the type of lastPublicHandle
     * - MegaRequest::getTransferredBytes - Returns the timestamp of the last access
     *
     * @param gateway              Payment gateway
     *                             Currently supported payment gateways are:
     *                             - MegaApi::PAYMENT_METHOD_ITUNES = 2
     *                             - MegaApi::PAYMENT_METHOD_GOOGLE_WALLET = 3
     *                             - MegaApi::PAYMENT_METHOD_WINDOWS_STORE = 13
     * @param receipt              Purchase receipt
     * @param lastPublicHandle     Last public node handle accessed by the user in the last 24h
     * @param lastPublicHandleType Indicates the type of lastPublicHandle, valid values are:
     *                             - MegaApi::AFFILIATE_TYPE_ID = 1
     *                             - MegaApi::AFFILIATE_TYPE_FILE_FOLDER = 2
     *                             - MegaApi::AFFILIATE_TYPE_CHAT = 3
     *                             - MegaApi::AFFILIATE_TYPE_CONTACT = 4
     * @param lastAccessTimestamp  Timestamp of the last access
     * @param listener             MegaRequestListener to track this request
     */
    fun submitPurchaseReceipt(
        gateway: Int,
        receipt: String?,
        lastPublicHandle: Long,
        lastPublicHandleType: Int,
        lastAccessTimestamp: Long,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Set My Chat Files target folder.
     *
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_MY_CHAT_FILES_FOLDER
     * - MegaRequest::getMegaStringMap - Returns a MegaStringMap.
     * The key "h" in the map contains the nodehandle specified as parameter encoded in B64
     *
     * @param nodeHandle MegaHandle of the node to be used as target folder
     * @param listener   MegaRequestListener to track this request
     */
    fun setMyChatFilesFolder(nodeHandle: Long, listener: MegaRequestListenerInterface)

    /**
     * Check if file versioning is enabled or disabled
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_USER
     * <p>
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the value MegaApi::USER_ATTR_DISABLE_VERSIONS
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getText - "1" for disable, "0" for enable
     * - MegaRequest::getFlag - True if disabled, false if enabled
     * <p>
     * If the option has never been set, the error code will be MegaError::API_ENOENT.
     * In that case, file versioning is enabled by default and MegaRequest::getFlag returns false.
     *
     * @param listener MegaRequestListener to track this request
     */
    fun getFileVersionsOption(listener: MegaRequestListenerInterface)

    /**
     * number of pending uploads
     */
    val numberOfPendingUploads: Int

    /**
     * Enable or disable file versioning
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_USER
     * <p>
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the value MegaApi::USER_ATTR_DISABLE_VERSIONS
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish:
     * - MegaRequest::getText - "1" for disable, "0" for enable
     *
     * @param disable  True to disable file versioning. False to enable it
     * @param listener MegaRequestListener to track this request
     */
    fun setFileVersionsOption(disable: Boolean, listener: MegaRequestListenerInterface)

    /**
     * Is User Logged In
     *
     * @return 0 if not logged in, Otherwise a number > 0
     */
    fun isUserLoggedIn(): Int

    /**
     * Cancels a Transfer by Tag
     *
     * @param transferTag the MegaTransfer Tag to cancel
     * @param listener a [MegaRequestListenerInterface] for callback purposes. It can be nullable
     */
    fun cancelTransferByTag(transferTag: Int, listener: MegaRequestListenerInterface?)

    /**
     * Get contact details
     *
     * @param handle Handle of the contact
     * @param listener MegaRequestListener to track this request
     */
    fun getContactLink(handle: Long, listener: MegaRequestListenerInterface)

    /**
     * Check valid node file
     *
     * @param node The [MegaNode] to check
     * @param nodeFile The [File] to check
     *
     * @return True if the [MegaNode] File is valid
     */
    fun checkValidNodeFile(node: MegaNode, nodeFile: File?): Boolean

    /**
     * Initialize the change of the email address associated to the account.
     *
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_CHANGE_EMAIL_LINK.
     * Valid data in the MegaRequest object received on all callbacks:
     * - MegaRequest::getEmail - Returns the email for the account
     *
     *
     * If this request succeeds, a change-email link will be sent to the specified email address.
     * If no user is logged in, you will get the error code MegaError::API_EACCESS in onRequestFinish().
     *
     *
     * If the MEGA account is a sub-user business account, onRequestFinish will
     * be called with the error code MegaError::API_EMASTERONLY.
     *
     * @param email    The new email to be associated to the account.
     * @param listener MegaRequestListener to track this request
     */
    fun changeEmail(email: String, listener: MegaRequestListenerInterface)

    /**
     * Reset the number of total uploads
     * This function resets the number returned by MegaApi::getTotalUploads
     */
    @Deprecated(
        "Function related to statistics will be reviewed in future updates to\n" +
                "provide more data and avoid race conditions. They could change or be removed in the current form."
    )
    fun resetTotalUploads()

    /**
     * Get Export Master Key
     */
    suspend fun getExportMasterKey(): String?

    /**
     * Set master key exported
     * @param listener as [MegaRequestListenerInterface]
     */
    fun setMasterKeyExported(listener: MegaRequestListenerInterface?)

    /**
     * Set a public attribute of the current user
     *
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type
     * - MegaRequest::getText - Returns the new value for the attribute
     *
     * @param type     Attribute type
     * Valid values are:
     * MegaApi::USER_ATTR_FIRSTNAME = 1
     * Set the firstname of the user (public)
     * MegaApi::USER_ATTR_LASTNAME = 2
     * Set the lastname of the user (public)
     * MegaApi::USER_ATTR_ED25519_PUBLIC_KEY = 5
     * Set the public key Ed25519 of the user (public)
     * MegaApi::USER_ATTR_CU25519_PUBLIC_KEY = 6
     * Set the public key Cu25519 of the user (public)
     * MegaApi::USER_ATTR_RUBBISH_TIME = 19
     * Set number of days for rubbish-bin cleaning scheduler (private non-encrypted)
     *
     *
     * If the MEGA account is a sub-user business account, and the value of the parameter
     * type is equal to MegaApi::USER_ATTR_FIRSTNAME or MegaApi::USER_ATTR_LASTNAME
     * onRequestFinish will be called with the error code MegaError::API_EMASTERONLY.
     * @param value    New attribute value
     * @param listener MegaRequestListener to track this request
     */
    fun setUserAttribute(type: Int, value: String, listener: MegaRequestListenerInterface)

    /**
     * Reset the number of total downloads
     * This function resets the number returned by MegaApi::getTotalDownloads
     */
    @Deprecated(
        "Function related to statistics will be reviewed in future updates to\n" +
                "provide more data and avoid race conditions. They could change or be removed in the current form."
    )
    suspend fun resetTotalDownloads()

    /**
     * Get information about a confirmation link or a new signup link
     *
     * The associated request type with this request is MegaRequest::TYPE_QUERY_SIGNUP_LINK.
     * Valid data in the MegaRequest object received on all callbacks:
     * - MegaRequest::getLink - Returns the confirmation link
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getEmail - Return the email associated with the link
     * - MegaRequest::getName - Returns the name associated with the link (available only for confirmation links)
     * - MegaRequest::getFlag - Returns true if the account was automatically confirmed, otherwise false
     *
     * If MegaRequest::getFlag returns true, the account was automatically confirmed and it's not needed
     * to call MegaApi::confirmAccount. If it returns false, it's needed to call MegaApi::confirmAccount
     * as usual. New accounts (V2, starting from April 2018) do not require a confirmation with the password,
     * but old confirmation links (V1) require it, so it's needed to check that parameter in onRequestFinish
     * to know how to proceed.
     *
     * If already logged-in into a different account, you will get the error code MegaError::API_EACCESS
     * in onRequestFinish.
     * If logged-in into the account that is attempted to confirm and the account is already confirmed, you
     * will get the error code MegaError::API_EEXPIRED in onRequestFinish.
     * In both cases, the MegaRequest::getEmail will return the email of the account that was attempted
     * to confirm, and the MegaRequest::getName will return the name.
     *
     * @param link     Confirmation link (confirm) or new signup link (newsignup)
     * @param listener MegaRequestListener to track this request
     */
    fun querySignupLink(link: String, listener: MegaRequestListenerInterface)

    /**
     * Get MegaNode given the Node File Link
     *
     * @param nodeFileLink  Public link to a file in MEGA
     * @param listener      MegaRequestListener to track this request
     */
    fun getPublicNode(
        nodeFileLink: String,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Cancels all transfers of the same type.
     *
     * The associated request type with this request is MegaRequest::TYPE_CANCEL_TRANSFERS
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the first parameter
     *
     * @param direction Type of transfers to cancel.
     *                  Valid values are:
     *                  - MegaTransfer::TYPE_DOWNLOAD = 0
     *                  - MegaTransfer::TYPE_UPLOAD = 1
     */
    suspend fun cancelTransfers(direction: Int)

    /**
     * Get verified phone number
     *
     * @return verified phone number if present else null
     */
    suspend fun getVerifiedPhoneNumber(): String?

    /**
     * Verify phone number
     *
     * @param pin verification pin
     * @param listener MegaRequestListener to track this request
     */
    fun verifyPhoneNumber(pin: String, listener: MegaRequestListenerInterface)

    /**
     * Logouts of the MEGA account without invalidating the session.
     *
     * The associated request type with this request is MegaRequest::TYPE_LOGOUT
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getFlag - Returns false
     *
     * @param listener MegaRequestListener to track this request
     */
    fun localLogout(listener: MegaRequestListenerInterface)
}
