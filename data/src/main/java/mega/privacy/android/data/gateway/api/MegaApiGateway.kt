package mega.privacy.android.data.gateway.api

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.model.RequestEvent
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaLoggerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaPushNotificationSettings
import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElementList
import nz.mega.sdk.MegaSetList
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaStringList
import nz.mega.sdk.MegaStringMap
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferData
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
     * Get Invalid Backup type
     */
    fun getInvalidBackupType(): Int

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
     * Upload a file or folder.
     *
     * This method should be used ONLY to share by chat a local file
     *
     * @param localPath The local path of the file or folder
     * @param parentNode The parent node for the file or folder
     * @param fileName The custom file name for the file or folder. Leave the parameter as "null"
     * if there are no changes
     * seconds since the epoch
     * @param appData The custom app data to save, which can be nullable
     * @param isSourceTemporary Whether the temporary file or folder that is created for upload
     * should be deleted or not
     * @param listener The [MegaTransferListenerInterface] to track the upload
     */
    fun startUploadForChat(
        localPath: String,
        parentNode: MegaNode,
        fileName: String?,
        appData: String?,
        isSourceTemporary: Boolean,
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
    suspend fun isMasterBusinessAccount(): Boolean

    /**
     * Get business status
     *
     * @return business status for the current account
     *
     *      public static final int BUSINESS_STATUS_EXPIRED = -1;
     *      public static final int BUSINESS_STATUS_INACTIVE = 0;
     *      public static final int BUSINESS_STATUS_ACTIVE = 1;
     *      public static final int BUSINESS_STATUS_GRACE_PERIOD = 2;
     */
    suspend fun getBusinessStatus(): Int

    /**
     * Is ephemeral plus plus account.
     */
    val isEphemeralPlusPlus: Boolean

    /**
     * Authentication token that can be used to identify the user account.
     */
    suspend fun getAccountAuth(): String?

    /**
     * Fingerprint of the signing key of the current account
     */
    val myCredentials: String?

    /**
     * Current session key.
     */
    val dumpSession: String?

    /**
     * User business status
     */
    val businessStatus: Int

    /**
     * Checks whether MEGA Achievements are enabled for the open account
     * @return True if enabled, false otherwise.
     */
    val isAchievementsEnabled: Boolean

    /**
     * Are upload transfers paused
     */
    suspend fun areUploadTransfersPaused(): Boolean

    /**
     * Are download transfers paused
     */
    suspend fun areDownloadTransfersPaused(): Boolean

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
    suspend fun getSdkVersion(): String?

    /**
     * Global updates
     */
    val globalUpdates: Flow<GlobalUpdate>

    /**
     * Global transfer
     */
    val globalTransfer: Flow<GlobalTransfer>

    /**
     * Global [RequestEvent] for all requests processed within this gateway.
     */
    val globalRequestEvents: Flow<RequestEvent>

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
     * Cancels all [MegaTransfer] downloads
     *
     * @param listener a [MegaRequestListenerInterface] for callback purposes. It can be nullable
     */
    fun cancelAllDownloadTransfers(listener: MegaRequestListenerInterface?)

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
     * Backups node of the account
     *
     * @return The Backups node if exists, null otherwise.
     */
    suspend fun getBackupsNode(): MegaNode?

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
     * Retry all pending requests.
     * <p>
     * When requests fails they wait some time before being retried. That delay grows exponentially if the request
     * fails again. For this reason, and since this request is very lightweight, it's recommended to call it with
     * the default parameters on every user interaction with the application. This will prevent very big delays
     * completing requests.
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
        collisionCheck: Int,
        collisionResolution: Int,
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
    @Deprecated(
        "This has been deprecated in favour of the below sendEvent",
        replaceWith = ReplaceWith("sendEvent(eventId, message, addJourneyId, viewId)")
    )
    suspend fun sendEvent(eventID: Int, message: String)

    /**
     * Send events to the stats server
     *
     * The associated request type with this request is MegaRequest::TYPE_SEND_EVENT
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNumber - Returns the event type
     * - MegaRequest::getText - Returns the event message
     * - MegaRequest::getFlag - Returns the addJourneyId flag
     * - MegaRequest::getSessionKey - Returns the ViewID
     *
     * @param eventId      Event type
     *                     Event types are restricted to the following ranges:
     *                     - MEGAcmd:   [98900, 99000)
     *                     - MEGAchat:  [99000, 99199)
     *                     - Android:   [99200, 99300)
     *                     - iOS:       [99300, 99400)
     *                     - MEGA SDK:  [99400, 99500)
     *                     - MEGAsync:  [99500, 99600)
     *                     - Webclient: [99600, 99800]
     * @param message      Event message
     * @param addJourneyId True if JourneyID should be included. Otherwise, false.
     * @param viewId       ViewID value (C-string null-terminated) to be sent with the event.
     *                     This value should have been generated with MegaApi::generateViewId method.
     * @deprecated This function is for internal usage of MEGA apps for debug purposes. This info
     * is sent to MEGA servers.
     */
    suspend fun sendEvent(eventId: Int, message: String, addJourneyId: Boolean, viewId: String?)

    /**
     * Generate an unique ViewID
     *
     * The caller gets the ownership of the object.
     *
     * A ViewID consists of a random generated id, encoded in hexadecimal as 16 characters of a null-terminated string.
     * @return the ViewId.
     */
    suspend fun generateViewId(): String

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
    suspend fun getUserAvatarColor(megaUser: MegaUser): String?

    /**
     * Get the default color for the avatar
     *
     * @param userHandle
     * @return The RGB color as a string with 3 components in hex: #RGB. Ie. "#FF6A19"
     */
    suspend fun getUserAvatarColor(userHandle: Long): String?

    /**
     * Get user avatar
     *
     * @param user
     * @param destinationPath destination path file
     * @param listener
     */
    fun getUserAvatar(
        user: MegaUser,
        destinationPath: String,
        listener: MegaRequestListenerInterface,
    )

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
     * Checks whether the Node is in Backups or not
     *
     * @param node The [MegaNode]
     * @return true if the Node is in Backups, and false if otherwise
     */
    suspend fun isInBackups(node: MegaNode): Boolean

    /**
     * Check is megaNode in Cloud drive
     *
     * @param node MegaNode
     * @return True in, else not in
     */
    suspend fun isInCloudDrive(node: MegaNode): Boolean

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
     * Move a transfer to the top of the transfer queue
     * <p>
     * If the transfer is successfully moved, onTransferUpdate will be called
     * for the corresponding listeners of the moved transfer and the new priority
     * of the transfer will be available using MegaTransfer::getPriority
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_MOVE_TRANSFER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getTransferTag - Returns the tag of the transfer to move
     * - MegaRequest::getFlag - Returns true (it means that it's an automatic move)
     * - MegaRequest::getNumber - Returns MegaTransfer::MOVE_TYPE_TOP
     *
     * @param transferTag Tag of the transfer to move
     * @param listener    MegaRequestListener to track this request
     */
    fun moveTransferToFirstByTag(transferTag: Int, listener: MegaRequestListenerInterface)

    /**
     * Move a transfer to the top of the transfer queue
     * <p>
     * If the transfer is successfully moved, onTransferUpdate will be called
     * for the corresponding listeners of the moved transfer and the new priority
     * of the transfer will be available using MegaTransfer::getPriority
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_MOVE_TRANSFER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getTransferTag - Returns the tag of the transfer to move
     * - MegaRequest::getFlag - Returns true (it means that it's an automatic move)
     * - MegaRequest::getNumber - Returns MegaTransfer::MOVE_TYPE_TOP
     *
     * @param transferTag Tag of the transfer to move
     * @param listener    MegaRequestListener to track this request
     */
    fun moveTransferToLastByTag(transferTag: Int, listener: MegaRequestListenerInterface)

    /**
     * Move a transfer before another one in the transfer queue
     * <p>
     * If the transfer is successfully moved, onTransferUpdate will be called
     * for the corresponding listeners of the moved transfer and the new priority
     * of the transfer will be available using MegaTransfer::getPriority
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_MOVE_TRANSFER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getTransferTag - Returns the tag of the transfer to move
     * - MegaRequest::getFlag - Returns false (it means that it's a manual move)
     * - MegaRequest::getNumber - Returns the tag of the transfer with the target position
     *
     * @param transferTag     Tag of the transfer to move
     * @param prevTransferTag Tag of the transfer with the target position
     * @param listener        MegaRequestListener to track this request
     */
    fun moveTransferBeforeByTag(
        transferTag: Int,
        prevTransferTag: Int,
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
     * Checks if node can be moved to target node
     *
     * @param node
     * @param targetNode
     *
     * - [MegaShare.ACCESS_UNKNOWN]
     * - [MegaShare.ACCESS_READ]
     * - [MegaShare.ACCESS_READWRITE]
     * - [MegaShare.ACCESS_FULL]
     * - [MegaShare.ACCESS_OWNER]
     *
     * @return success or failed
     */
    fun checkMoveErrorExtended(node: MegaNode, targetNode: MegaNode): MegaError

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
     * Get the owner of this node or null if is not an incoming shared node
     * @param node that is being shared
     * @param recursive if true root node of [node] will be checked, if false the [node] itself will be checked
     * @return the owner of this node or null if is not an incoming shared node
     */
    suspend fun getUserFromInShare(node: MegaNode, recursive: Boolean): MegaUser?

    /**
     * Get a list with all active and pending outbound sharings
     *
     * @param order Sorting order to use
     * @return List of MegaShare objects
     */
    suspend fun getOutShares(order: Int): List<MegaShare>

    /**
     * Get a list with the active and pending outbound sharings for a MegaNode
     *
     * @param megaNode the node to fetch
     * @return List of MegaShare objects
     */
    suspend fun getOutShares(megaNode: MegaNode): List<MegaShare>

    /**
     * Get unverified incoming shares
     *
     * @param order : Sort order
     * @return List of [MegaShare]
     */
    suspend fun getUnverifiedIncomingShares(order: Int): List<MegaShare>

    /**
     * Get verified incoming shares
     *
     * @param order : Sort order
     * @return List of [MegaShare]
     */
    suspend fun getVerifiedIncomingShares(order: Int?): List<MegaShare>

    /**
     * Create a new MegaSet item
     *
     * @param name the name of the set
     * @param type the type of the set
     * @param listener [MegaRequestListenerInterface]
     */
    fun createSet(name: String, type: Int, listener: MegaRequestListenerInterface)

    /**
     * Create a new element for the set
     *
     * @param sid the ID of the set
     * @param node the node handle of the node which will be assigned as the set's new element
     * @param listener MegaRequestListener to track this request
     */
    fun createSetElement(sid: Long, node: Long, listener: MegaRequestListenerInterface)

    /**
     * Request creation of multiple Elements for a Set
     *
     * The associated request type with this request is MegaRequest::TYPE_PUT_SET_ELEMENTS
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getTotalBytes - Returns the id of the Set
     * - MegaRequest::getMegaHandleList - Returns a list containing the file handles corresponding to the new Elements
     * - MegaRequest::getMegaStringList - Returns a list containing the names corresponding to the new Elements
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getMegaSetElementList - Returns a list containing only the new Elements
     * - MegaRequest::getMegaIntegerList - Returns a list containing error codes for all requested Elements
     *
     * On the onRequestFinish error, the error code associated to the MegaError can be:
     * - MegaError::API_ENOENT - Set could not be found.
     * - MegaError::API_EINTERNAL - Received answer could not be read or decrypted.
     * - MegaError::API_EARGS - Malformed (from API).
     * - MegaError::API_EACCESS - Permissions Error (from API).
     *
     * @param sid the id of the Set that will own the new Elements
     * @param nodes the handles of the file-nodes that will be represented by the new Elements
     * @param names the names that should be given to the new Elements (param names must be either null or have
     * the same size() as param nodes)
     * @param listener MegaRequestListener to track this request
     */
    fun createSetElements(
        sid: Long,
        nodes: MegaHandleList,
        names: MegaStringList?,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Remove an element from a set
     *
     * @param sid the ID of the set
     * @param eid the SetElement ID that will be removed
     * @param listener MegaRequestListener to track this request
     */
    suspend fun removeSetElement(sid: Long, eid: Long, listener: MegaRequestListenerInterface)

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
     * Generate a public link of a Set in MEGA
     *
     * The associated request type with this request is MegaRequest::TYPE_EXPORT_SET
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns id of the Set used as parameter
     * - MegaRequest::getFlag - Returns a boolean set to true representing the call was
     * meant to enable/create the export
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getMegaSet - MegaSet including the public id
     * - MegaRequest::getLink - Public link
     *
     * MegaError::API_OK results in onSetsUpdate being triggered as well
     *
     * If the MEGA account is a business account and it's status is expired, onRequestFinish will
     * be called with the error code MegaError::API_EBUSINESSPASTDUE.
     *
     * @param sid      MegaHandle to get the public link
     * @param listener MegaRequestListener to track this request
     */
    fun exportSet(sid: Long, listener: MegaRequestListenerInterface)

    /**
     * Stop sharing a Set
     *
     * The associated request type with this request is MegaRequest::TYPE_EXPORT_SET
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns id of the Set used as parameter
     * - MegaRequest::getFlag - Returns a boolean set to false representing the call was
     * meant to disable the export
     *
     * MegaError::API_OK results in onSetsUpdate being triggered as well
     *
     * If the MEGA account is a business account and it's status is expired, onRequestFinish will
     * be called with the error code MegaError::API_EBUSINESSPASTDUE.
     *
     * @param sid      Set MegaHandle to stop sharing
     * @param listener MegaRequestListener to track this request
     */
    fun disableExportSet(sid: Long, listener: MegaRequestListenerInterface)

    /**
     * Gets a MegaNode for the foreign MegaSetElement that can be used to download the Element
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_EXPORTED_SET_ELEMENT
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getPublicMegaNode - Returns the MegaNode (ownership transferred)
     *
     * On the onRequestFinish error, the error code associated to the MegaError can be:
     * - MegaError::API_EACCESS - Public Set preview mode is not enabled
     * - MegaError::API_EARGS - MegaHandle for SetElement provided as param doesn't match any Element
     * in previewed Set
     *
     * If the MEGA account is a business account and it's status is expired, onRequestFinish will
     * be called with the error code MegaError::API_EBUSINESSPASTDUE.
     *
     * @param eid      MegaHandle of target SetElement from Set in preview mode
     * @param listener MegaRequestListener to track this request
     */
    fun getPreviewElementNode(eid: Long, listener: MegaRequestListenerInterface)

    /**
     * Request to fetch a public/exported Set and its Elements.
     *
     * The associated request type with this request is MegaRequest::TYPE_FETCH_SET
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getLink - Returns the link used for the public Set fetch request
     *
     * In addition to fetching the Set (including Elements), SDK's instance is set
     * to preview mode for the public Set. This mode allows downloading of foreign
     * SetElements included in the public Set.
     *
     * To disable the preview mode and release resources used by the preview Set,
     * use MegaApi::stopPublicSetPreview
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getMegaSet - Returns the Set
     * - MegaRequest::getMegaSetElementList - Returns the list of Elements
     *
     * On the onRequestFinish error, the error code associated to the MegaError can be:
     * - MegaError::API_ENOENT - Set could not be found.
     * - MegaError::API_EINTERNAL - Received answer could not be read or decrypted.
     * - MegaError::API_EARGS - Malformed (from API).
     * - MegaError::API_EACCESS - Permissions Error (from API).
     *
     * If the MEGA account is a business account and it's status is expired, onRequestFinish will
     * be called with the error code MegaError::API_EBUSINESSPASTDUE.
     *
     * @param publicSetLink Public link to a Set in MEGA
     * @param listener      MegaRequestListener to track this request
     */
    fun fetchPublicSet(publicSetLink: String, listener: MegaRequestListenerInterface)

    /**
     * Stops public Set preview mode for current SDK instance
     *
     * MegaApi instance is no longer useful until a new login
     */
    fun stopPublicSetPreview()

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
     * Returns settings for push notifications
     *
     * The SDK retains the ownership of the returned value. It will be valid until
     * the MegaRequest object is deleted.
     *
     * This value is valid for these requests in onRequestFinish when the
     * error code is MegaError::API_OK:
     * - MegaApi::getPushNotificationSettings - Returns settings for push notifications
     *
     * @return Object with settings for push notifications
     */
    fun getPushNotificationSettings(listener: MegaRequestListenerInterface)

    /**
     * Set push notification settings
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_PUSH_SETTINGS
     * - MegaRequest::getMegaPushNotificationSettings - Returns settings for push notifications
     *
     * @param settings MegaPushNotificationSettings with the new settings
     * @param listener MegaRequestListener to track this request
     */
    fun setPushNotificationSettings(
        settings: MegaPushNotificationSettings,
        listener: MegaRequestListenerInterface,
    )

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
     * Number of pending uploads
     */
    val numberOfPendingUploads: Int

    /**
     *
     * Number of pending downloads.
     */
    val numberOfPendingDownloads: Int

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
     * @brief Check if the logged in account is considered new
     *
     * This function will NOT return a valid value until the callback onEvent with
     * type MegaApi::EVENT_MISC_FLAGS_READY is received. You can also rely on the completion of
     * a fetchnodes to check this value.
     *
     * @return True if account is considered new. Otherwise, false.
     */
    suspend fun isAccountNew(): Boolean

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
     * Check if the master key has been exported
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_PWD_REMINDER
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getAccess - Returns true if the master key has been exported
     * <p>
     * If the corresponding user attribute is not set yet, the request will fail with the
     * error code MegaError::API_ENOENT.
     * @param listener MegaRequestListener to track this request
     */
    fun isMasterKeyExported(listener: MegaRequestListenerInterface?)

    /**
     * Get the secret code of the account to enable multi-factor authentication
     * @param listener as [MegaRequestListenerInterface]
     */
    fun getMultiFactorAuthCode(listener: MegaRequestListenerInterface?)

    /**
     * Enable multi-factor authentication for the account
     * The MegaApi object must be logged into an account to successfully use this function.
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_MULTI_FACTOR_AUTH_SET
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getFlag - Returns true
     * - MegaRequest::getPassword - Returns the pin sent in the first parameter
     * @param pin      Valid pin code for multi-factor authentication
     * @param listener MegaRequestListener to track this request
     */
    fun enableMultiFactorAuth(pin: String, listener: MegaRequestListenerInterface?)

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
     * Set a private attribute of the current user
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type
     * - MegaRequest::getMegaStringMap - Returns the new value for the attribute
     *
     * You can remove existing records/keypairs from the following attributes:
     *  - MegaApi::ATTR_ALIAS
     *  - MegaApi::ATTR_DEVICE_NAMES
     *  - MegaApi::USER_ATTR_APPS_PREFS
     *  - MegaApi::USER_ATTR_CC_PREFS
     * by adding a keypair into MegaStringMap whit the key to remove and an empty C-string null terminated as value.
     *
     * @param type Attribute type
     *
     * Valid values are:
     *
     * MegaApi::USER_ATTR_AUTHRING = 3
     * Get the authentication ring of the user (private)
     * MegaApi::USER_ATTR_LAST_INTERACTION = 4
     * Get the last interaction of the contacts of the user (private)
     * MegaApi::USER_ATTR_KEYRING = 7
     * Get the key ring of the user: private keys for Cu25519 and Ed25519 (private)
     * MegaApi::USER_ATTR_RICH_PREVIEWS = 18
     * Get whether user generates rich-link messages or not (private)
     * MegaApi::USER_ATTR_RUBBISH_TIME = 19
     * Set number of days for rubbish-bin cleaning scheduler (private non-encrypted)
     * MegaApi::USER_ATTR_GEOLOCATION = 22
     * Set whether the user can send geolocation messages (private)
     * MegaApi::ATTR_ALIAS = 27
     * Set the list of users's aliases (private)
     * MegaApi::ATTR_DEVICE_NAMES = 30
     * Set the list of device names (private)
     * MegaApi::ATTR_APPS_PREFS = 38
     * Set the apps prefs (private)
     *
     * @param value New attribute value
     * @param listener MegaRequestListener to track this request
     */
    fun setUserAttribute(type: Int, value: MegaStringMap, listener: MegaRequestListenerInterface)

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
     * Get information about a recovery link created by MegaApi::resetPassword.
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_QUERY_RECOVERY_LINK
     * Valid data in the MegaRequest object received on all callbacks:
     * - MegaRequest::getLink - Returns the recovery link
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getEmail - Return the email associated with the link
     * - MegaRequest::getFlag - Return whether the link requires masterkey to reset password.
     *
     * @param link     Recovery link (recover)
     * @param listener MegaRequestListener to track this request
     */
    fun queryResetPasswordLink(link: String, listener: MegaRequestListenerInterface)

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

    /**
     * Search query in inshares
     * @param query Querry String
     * @param megaCancelToken [MegaCancelToken]
     * @param order [SortOrder]
     */
    suspend fun searchOnInShares(
        query: String,
        megaCancelToken: MegaCancelToken,
        order: Int,
    ): List<MegaNode>

    /**
     * Search query in Outshares
     * @param query Querry String
     * @param megaCancelToken [MegaCancelToken]
     * @param order [SortOrder]
     */
    suspend fun searchOnOutShares(
        query: String,
        megaCancelToken: MegaCancelToken,
        order: Int,
    ): List<MegaNode>

    /**
     * Search query in Linkshares
     * @param query Querry String
     * @param megaCancelToken [MegaCancelToken]
     * @param order [SortOrder]
     */
    suspend fun searchOnLinkShares(
        query: String,
        megaCancelToken: MegaCancelToken,
        order: Int,
    ): List<MegaNode>

    /**
     * Search query in node
     *
     * @param parent [MegaNode]
     * @param query Query to be searched
     * @param megaCancelToken [MegaCancelToken]
     * @param order [SortOrder]
     */
    suspend fun search(
        parent: MegaNode,
        query: String,
        megaCancelToken: MegaCancelToken,
        order: Int,
    ): List<MegaNode>

    /**
     * Creates a new share key for the node if there is no share key already created.
     *
     * @param megaNode : [MegaNode] object which needs to be shared
     * @param listener : Listener to track this request
     */
    fun openShareDialog(
        megaNode: MegaNode,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Update cryptographic security
     *
     * @param listener : Listener to track this request
     */
    fun upgradeSecurity(listener: MegaRequestListenerInterface)

    /**
     * Sets the secure flag to true or false while sharing a node
     *
     * @param enable : Boolean value
     */
    @Deprecated("This API is for testing purpose, will be deleted later")
    fun setSecureFlag(enable: Boolean)

    /**
     * Get sms allowed state
     *
     * @return current sms allowed state: 2 = Opt-in and unblock SMS allowed.  1 = Only unblock SMS allowed.  0 = No SMS allowed
     */
    suspend fun getSmsAllowedState(): Int

    /**
     * Logs in to a MEGA account.
     *
     * The associated request type with this request is MegaRequest::TYPE_LOGIN.
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getEmail - Returns the first parameter
     * - MegaRequest::getPassword - Returns the second parameter
     * <p>
     * If the email/password aren't valid the error code provided in onRequestFinish is
     * MegaError::API_ENOENT.
     *
     * @param email    Email of the user
     * @param password Password
     * @param listener MegaRequestListener to track this request
     */
    fun login(email: String, password: String, listener: MegaRequestListenerInterface)

    /**
     * Log in to a MEGA account with multi-factor authentication enabled
     *
     * The associated request type with this request is MegaRequest::TYPE_LOGIN.
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getEmail - Returns the first parameter
     * - MegaRequest::getPassword - Returns the second parameter
     * - MegaRequest::getText - Returns the third parameter
     *
     * If the email/password aren't valid the error code provided in onRequestFinish is
     * MegaError::API_ENOENT.
     *
     * @param email    Email of the user
     * @param password Password
     * @param pin      Pin code for multi-factor authentication
     * @param listener MegaRequestListener to track this request
     */
    fun multiFactorAuthLogin(
        email: String,
        password: String,
        pin: String,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Get the path of a MegaNode
     *
     * @param node MegaNode for which the path will be returned
     * @return The path of the node
     */
    suspend fun getNodePath(node: MegaNode): String?

    /**
     * Returns the access level of the sharing
     *
     * Possible return values are:
     * - ACCESS_UNKNOWN = -1
     * It means that the access level is unknown
     *
     * - ACCESS_READ = 0
     * The user can read the folder only
     *
     * - ACCESS_READWRITE = 1
     * The user can read and write the folder
     *
     * - ACCESS_FULL = 2
     * The user has full permissions over the folder
     *
     * - ACCESS_OWNER = 3
     * The user is the owner of the folder
     *
     * @param megaNode [MegaNode]
     * @return The access level of the sharing
     */
    fun getAccess(megaNode: MegaNode): Int

    /**
     * Stop sharing a file/folder node
     *
     *
     * @param megaNode [MegaNode] to stop sharing
     */
    fun stopSharingNode(megaNode: MegaNode)

    /**
     * Add, update or remove(access == ACCESS_UNKNOWN) a node's outgoing shared access for a user
     * @param megaNode the [MegaNode] that will be affected
     * @param email of the user we want to change access
     * @param accessLevel, can be one of the following:
     *
     * * - ACCESS_UNKNOWN = -1
     * It means that the access will be removed
     *
     * - ACCESS_READ = 0
     * The user can read the folder only
     *
     * - ACCESS_READWRITE = 1
     * The user can read and write the folder
     *
     * - ACCESS_FULL = 2
     * The user has full permissions over the folder
     *  @param listener    MegaRequestListener to track this request
     */
    fun setShareAccess(
        megaNode: MegaNode,
        email: String,
        accessLevel: Int,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Set/Remove the avatar of the MEGA account
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getFile - Returns the source path (optional)
     *
     * @param srcFilePath Source path of the file that will be set as avatar.
     *                    If NULL, the existing avatar will be removed (if any).
     *                    In case the avatar never existed before, removing the avatar returns MegaError::API_ENOENT
     * @param listener    MegaRequestListener to track this request
     */
    fun setAvatar(srcFilePath: String?, listener: MegaRequestListenerInterface)

    /**
     *  Notify the user has successfully skipped the password check
     *  @param listener    MegaRequestListener to track this request
     */
    fun skipPasswordReminderDialog(listener: MegaRequestListenerInterface)

    /**
     * Notify the user wants to totally disable the password check
     * @param listener    MegaRequestListener to track this request
     */
    fun blockPasswordReminderDialog(listener: MegaRequestListenerInterface)

    /**
     * Notify the user has successfully checked his password
     * @param listener    MegaRequestListener to track this request
     */
    fun successPasswordReminderDialog(listener: MegaRequestListenerInterface)

    /**
     * Set user Alias Name
     *
     * @param userHandle User Handle
     * @param name updated nick name
     * @param listener mega request listener interface
     */
    fun setUserAlias(userHandle: Long, name: String?, listener: MegaRequestListenerInterface)

    /**
     * Get information about transfer queues
     *
     * @return Information about transfer queues
     */
    suspend fun getTransferData(): MegaTransferData?

    /**
     * Remove a contact to the MEGA account
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_REMOVE_CONTACT
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getEmail - Returns the email of the contact
     *
     * @param user     MegaUser of the contact (see MegaApi::getContact)
     * @param listener MegaRequestListener to track this request
     */
    fun removeContact(user: MegaUser, listener: MegaRequestListenerInterface?)

    /**
     * @param backupId backup id identifying the backup
     * @param status   backup state
     * @param progress backup progress
     * @param ups      Number of pending upload transfers
     * @param downs    Number of pending download transfers
     * @param ts       Last action timestamp
     * @param lastNode Last node handle to be synced
     * @param listener MegaRequestListener to track this request
     *                 Send heartbeat associated with an existing backup
     *                 <p>
     *                 The client should call this method regularly for every registered backup, in order to
     *                 inform about the status of the backup.
     *                 <p>
     *                 Progress, last timestamp and last node are not always meaningful (ie. when the Camera
     *                 Uploads starts a new batch, there isn't a last node, or when the CU up to date and
     *                 inactive for long time, the progress doesn't make sense). In consequence, these parameters
     *                 are optional. They will not be sent to API if they take the following values:
     *                 - lastNode = INVALID_HANDLE
     *                 - lastTs = -1
     *                 - progress = -1
     *                 <p>
     *                 The associated request type with this request is MegaRequest::TYPE_BACKUP_PUT_HEART_BEAT
     *                 Valid data in the MegaRequest object received on callbacks:
     *                 - MegaRequest::getParentHandle - Returns the backupId
     *                 - MegaRequest::getAccess - Returns the backup state
     *                 - MegaRequest::getNumDetails - Returns the backup substate
     *                 - MegaRequest::getParamType - Returns the number of pending upload transfers
     *                 - MegaRequest::getTransferTag - Returns the number of pending download transfers
     *                 - MegaRequest::getNumber - Returns the last action timestamp
     *                 - MegaRequest::getNodeHandle - Returns the last node handle to be synced
     */
    fun sendBackupHeartbeat(
        backupId: Long, status: Int, progress: Int, ups: Int, downs: Int,
        ts: Long, lastNode: Long, listener: MegaRequestListenerInterface?,
    )

    /**
     * @param backupId    backup id identifying the backup to be updated
     * @param backupType  back up type requested for the service
     * @param targetNode  MEGA folder to hold the backups
     * @param localFolder Local path of the folder
     * @param state       backup state
     * @param subState    backup subState
     * @param listener    MegaRequestListener to track this request
     *                    Update the information about a registered backup for Backup Centre
     *                    <p>
     *                    Possible types of backups:
     *                    BACKUP_TYPE_INVALID = -1,
     *                    BACKUP_TYPE_CAMERA_UPLOADS = 3,
     *                    BACKUP_TYPE_MEDIA_UPLOADS = 4,   // Android has a secondary CU
     *                    <p>
     *                    Params that keep the same value are passed with invalid value to avoid to send to the server
     *                    Invalid values:
     *                    - type: BACKUP_TYPE_INVALID
     *                    - nodeHandle: UNDEF
     *                    - localFolder: nullptr
     *                    - deviceId: nullptr
     *                    - state: -1
     *                    - subState: -1
     *                    - extraData: nullptr
     *                    <p>
     *                    If you want to update the backup name, use \c MegaApi::setBackupName.
     *                    <p>
     *                    The associated request type with this request is MegaRequest::TYPE_BACKUP_PUT
     *                    Valid data in the MegaRequest object received on callbacks:
     *                    - MegaRequest::getParentHandle - Returns the backupId
     *                    - MegaRequest::getTotalBytes - Returns the backup type
     *                    - MegaRequest::getNodeHandle - Returns the target node of the backup
     *                    - MegaRequest::getFile - Returns the path of the local folder
     *                    - MegaRequest::getAccess - Returns the backup state
     *                    - MegaRequest::getNumDetails - Returns the backup substate
     *                    - MegaRequest::getListener - Returns the MegaRequestListener to track this request
     */
    fun updateBackup(
        backupId: Long, backupType: Int, targetNode: Long, localFolder: String?,
        backupName: String?, state: Int, subState: Int,
        listener: MegaRequestListenerInterface?,
    )

    /**
     * Set the GPS coordinates of image files as a node attribute.
     * <p>
     * To remove the existing coordinates, set both the latitude and longitude to
     * the value MegaNode::INVALID_COORDINATE.
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_NODE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node that receive the attribute
     * - MegaRequest::getFlag - Returns true (official attribute)
     * - MegaRequest::getParamType - Returns MegaApi::NODE_ATTR_COORDINATES
     * - MegaRequest::getNumDetails - Returns the longitude, scaled to integer in the range of [0, 2^24]
     * - MegaRequest::getTransferTag() - Returns the latitude, scaled to integer in the range of [0, 2^24)
     * <p>
     * If the MEGA account is a business account and it's status is expired, onRequestFinish will
     * be called with the error code MegaError::API_EBUSINESSPASTDUE.
     *
     * @param nodeId    Handle associated with a node that will receive the information.
     * @param latitude  Latitude in signed decimal degrees notation
     * @param longitude Longitude in signed decimal degrees notation
     * @param listener  MegaRequestListener to track this request
     */
    fun setCoordinates(
        nodeId: NodeId,
        latitude: Double,
        longitude: Double,
        listener: MegaRequestListenerInterface?,
    )

    /**
     * Check if the app should show the password reminder dialog to the user
     *
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_PWD_REMINDER
     *
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getFlag - Returns true if the password reminder dialog should be shown
     *
     *
     * If the corresponding user attribute is not set yet, the request will fail with the
     * error code MegaError::API_ENOENT but the value of MegaRequest::getFlag will still
     * be valid.
     *
     * @param atLogout True if the check is being done just before a logout
     * @param listener MegaRequestListener to track this request
     */
    fun shouldShowPasswordReminderDialog(
        atLogout: Boolean,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Is foreign node
     *
     * @param handle
     * @return true if foreign node
     */
    suspend fun isForeignNode(handle: Long): Boolean

    /**
     * @param backupType  back up type requested for the service
     * @param targetNode  MEGA folder to hold the backups
     * @param localFolder Local path of the folder
     * @param backupName  Name of the backup
     * @param state       state
     * @param subState    subState
     * @param listener    MegaRequestListener to track this request
     *                    Registers a backup to display in Backup Centre
     *                    <p>
     *                    Apps should register backups, like CameraUploads, in order to be listed in the
     *                    BackupCentre. The client should send heartbeats to indicate the progress of the
     *                    backup (see \c MegaApi::sendBackupHeartbeats).
     *                    <p>
     *                    Possible types of backups:
     *                    BACKUP_TYPE_CAMERA_UPLOADS = 3,
     *                    BACKUP_TYPE_MEDIA_UPLOADS = 4,   // Android has a secondary CU
     *                    <p>
     *                    Note that the backup name is not registered in the API as part of the data of this
     *                    backup. It will be stored in a user's attribute after this request finished. For
     *                    more information, see \c MegaApi::setBackupName and MegaApi::getBackupName.
     *                    <p>
     *                    The associated request type with this request is MegaRequest::TYPE_BACKUP_PUT
     *                    Valid data in the MegaRequest object received on callbacks:
     *                    - MegaRequest::getParentHandle - Returns the backupId
     *                    - MegaRequest::getNodeHandle - Returns the target node of the backup
     *                    - MegaRequest::getName - Returns the backup name of the remote location
     *                    - MegaRequest::getAccess - Returns the backup state
     *                    - MegaRequest::getFile - Returns the path of the local folder
     *                    - MegaRequest::getTotalBytes - Returns the backup type
     *                    - MegaRequest::getNumDetails - Returns the backup substate
     *                    - MegaRequest::getFlag - Returns true
     *                    - MegaRequest::getListener - Returns the MegaRequestListener to track this request
     */

    fun setBackup(
        backupType: Int, targetNode: Long, localFolder: String, backupName: String,
        state: Int, subState: Int, listener: MegaRequestListenerInterface,
    )

    /**
     * @param backupId backup id identifying the backup to be removed
     * @param listener MegaRequestListener to track this request
     *                 Unregister a backup already registered for the Backup Centre
     *                 <p>
     *                 This method allows to remove a backup from the list of backups displayed in the
     *                 Backup Centre. @see \c MegaApi::setBackup.
     *                 <p>
     *                 The associated request type with this request is MegaRequest::TYPE_BACKUP_REMOVE
     *                 Valid data in the MegaRequest object received on callbacks:
     *                 - MegaRequest::getParentHandle - Returns the backupId
     *                 - MegaRequest::getListener - Returns the MegaRequestListener to track this request
     */
    fun removeBackup(backupId: Long, listener: MegaRequestListenerInterface)

    /**
     * Fetch information about all registered backups for Backup Centre
     *
     * The associated request type with this request is MegaRequest::TYPE_BACKUP_INFO
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getListener - Returns the MegaRequestListener to track this request
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getMegaBackupInfoList - Returns information about all registered backups
     *
     * @param listener MegaRequestListener to track this request
     */
    fun getBackupInfo(listener: MegaRequestListenerInterface)

    /**
     * Reconnect and retry all transfers.
     */
    suspend fun reconnect()

    /**
     * Create a cancel token
     */
    fun createCancelToken(): MegaCancelToken


    /**
     * Export a MegaNode
     *
     * @param node the MegaNode to export
     * @param expireTime the time in seconds since epoch to set as expiry date
     * @param listener MegaRequestListener to track this request
     */
    fun exportNode(
        node: MegaNode,
        expireTime: Long?,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Returns the name previously set for a device
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_DEVICE_NAMES
     * - MegaRequest::getText - Returns passed device id (or the value returned by getDeviceId()
     * if deviceId was initially passed as null).
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getName - Returns device name.
     *
     * @param deviceId The id of the device to get the name for. If null, the value returned
     * by getDeviceId() will be used instead.
     * @param listener MegaRequestListener to track this request
     */
    fun getDeviceName(deviceId: String, listener: MegaRequestListenerInterface?)

    /**
     * Sets name for specified device
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_DEVICE_NAMES
     * - MegaRequest::getName - Returns device name.
     * - MegaRequest::getText - Returns passed device id (or the value returned by getDeviceId()
     * if deviceId was initially passed as null).
     *
     * @param deviceId The id of the device to set the name for. If null, the value returned
     * by getDeviceId() will be used instead.
     * @param deviceName String with device name
     * @param listener MegaRequestListener to track this request
     */
    fun setDeviceName(deviceId: String, deviceName: String, listener: MegaRequestListenerInterface?)

    /**
     * Returns the id of this device
     *
     * You take the ownership of the returned value.
     *
     * @return The id of this device
     */
    fun getDeviceId(): String?

    /**
     * Get if AB test flag is active
     *
     * @param flag the AB test flag name for API without 'ab_' prefix, which will added by SDK code
     */
    fun getABTestValue(flag: String): Long

    /**
     * Get banner quota time
     */
    suspend fun getBannerQuotaTime(): Long

    /**
     * Launches a request to stop sharing a file/folder
     *
     * @param node          MegaNode to stop sharing
     * @param listener      MegaRequestListener to track this request
     */
    fun disableExport(
        node: MegaNode,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Encrypt public link with password
     *
     *
     * The associated request type with this request is MegaRequest::TYPE_PASSWORD_LINK
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getLink - Returns the public link to be encrypted
     * - MegaRequest::getPassword - Returns the password to encrypt the link
     * - MegaRequest::getFlag - Returns true
     *
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getText - Encrypted public link
     *
     * @param link     Public link to be encrypted, including encryption key for the link
     * @param password Password to encrypt the link
     * @param listener MegaRequestListener to track this request
     */
    fun encryptLinkWithPassword(
        link: String,
        password: String,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Current upload speed
     */
    val currentUploadSpeed: Int

    /**
     * Set the GPS coordinates of image files as a node attribute.
     *
     * To remove the existing coordinates, set both the latitude and longitude to
     * the value MegaNode::INVALID_COORDINATE.
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_NODE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node that receive the attribute
     * - MegaRequest::getFlag - Returns true (official attribute)
     * - MegaRequest::getParamType - Returns MegaApi::NODE_ATTR_COORDINATES
     * - MegaRequest::getNumDetails - Returns the longitude, scaled to integer in the range of [0, 2^24]
     * - MegaRequest::getTransferTag() - Returns the latitude, scaled to integer in the range of [0, 2^24)
     *
     * If the MEGA account is a business account and it's status is expired, onRequestFinish will
     * be called with the error code MegaError::API_EBUSINESSPASTDUE.
     *
     * @param node      Node that will receive the information.
     * @param latitude  Latitude in signed decimal degrees notation
     * @param longitude Longitude in signed decimal degrees notation
     */
    suspend fun setNodeCoordinates(node: MegaNode, latitude: Double, longitude: Double)

    /**
     * Create a thumbnail for an image
     *
     * @param imagePath Image path
     * @param destinationPath   Destination path for the thumbnail (including the file name)
     * @return True if the thumbnail was successfully created, otherwise false.
     */

    suspend fun createThumbnail(imagePath: String, destinationPath: String): Boolean

    /**
     * Create a preview for an image
     *
     * @param imagePath Image path
     * @param destinationPath   Destination path for the preview (including the file name)
     * @return True if the preview was successfully created, otherwise false.
     */

    suspend fun createPreview(imagePath: String, destinationPath: String): Boolean

    /**
     * Pause/resume all transfers
     *
     *
     * The associated request type with this request is MegaRequest::TYPE_PAUSE_TRANSFERS
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getFlag - Returns the first parameter
     *
     * @param pause    true to pause all transfers / false to resume all transfers
     * @param listener MegaRequestListener to track this request
     */
    fun pauseTransfers(pause: Boolean, listener: MegaRequestListenerInterface)

    /**
     * Set the thumbnail of a MegaNode
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_FILE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node
     * - MegaRequest::getFile - Returns the source path
     * - MegaRequest::getParamType - Returns MegaApi::ATTR_TYPE_THUMBNAIL
     *
     * @param node MegaNode handle to set the thumbnail
     * @param srcFilePath Source path of the file that will be set as thumbnail
     * @param listener    MegaRequestListener to track this request
     */
    fun setThumbnail(
        node: MegaNode,
        srcFilePath: String,
        listener: MegaRequestListenerInterface?,
    )

    /**
     * Set the preview of a MegaNode
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_FILE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node
     * - MegaRequest::getFile - Returns the source path
     * - MegaRequest::getParamType - Returns MegaApi::ATTR_TYPE_PREVIEW
     *
     * @param node        MegaNode to set the preview
     * @param srcFilePath Source path of the file that will be set as preview
     * @param listener    MegaRequestListener to track this request
     */
    fun setPreview(
        node: MegaNode,
        srcFilePath: String,
        listener: MegaRequestListenerInterface?,
    )

    /**
     * Pause/resume a transfer
     *
     *
     * The request finishes with MegaError::API_OK if the state of the transfer is the
     * desired one at that moment. That means that the request succeed when the transfer
     * is successfully paused or resumed, but also if the transfer was already in the
     * desired state and it wasn't needed to change anything.
     *
     *
     * Resumed transfers don't necessarily continue just after the resumption. They
     * are tagged as queued and are processed according to its position on the request queue.
     *
     *
     * The associated request type with this request is MegaRequest::TYPE_PAUSE_TRANSFER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getTransferTag - Returns the tag of the transfer to pause or resume
     * - MegaRequest::getFlag - Returns true if the transfer has to be pause or false if it has to be resumed
     *
     * @param transferTag Tag of the transfer to pause or resume
     * @param pause       True to pause the transfer or false to resume it
     * @param listener    MegaRequestListener to track this request
     */
    fun pauseTransferByTag(transferTag: Int, pause: Boolean, listener: MegaRequestListenerInterface)

    /**
     * Get Verify contact verification warning enabled flag from api
     *
     * This method will get a flag for the user if user has set to enable to see warning
     * if any sharing is coming un verified contact
     */
    suspend fun getContactVerificationWarningEnabled(): Boolean

    /**
     * Create Ephemeral++ account
     *
     * This kind of account allows to join chat links and to keep the session in the device
     * where it was created.
     *
     * The associated request type with this request is MegaRequest::TYPE_CREATE_ACCOUNT.
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getName - Returns the firstname of the user
     * - MegaRequest::getText - Returns the lastname of the user
     * - MegaRequest::getParamType - Returns the value MegaApi:CREATE_EPLUSPLUS_ACCOUNT
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getSessionKey - Returns the session id to resume the process
     *
     * If this request succeeds, a new ephemeral++ account will be created for the new user.
     * The app may resume the create-account process by using MegaApi::resumeCreateAccountEphemeralPlusPlus.
     *
     * @note This account should be confirmed in same device it was created
     *
     * @param firstName Firstname of the user
     * @param lastName Lastname of the user
     * @param listener MegaRequestListener to track this request
     */
    fun createEphemeralAccountPlusPlus(
        firstName: String,
        lastName: String,
        listener: MegaRequestListenerInterface,
    )

    /**
     * Make a name suitable for a file name in the local filesystem
     *
     * This function escapes (%xx) forbidden characters in the local filesystem if needed.
     * You can revert this operation using MegaApi::unescapeFsIncompatible
     *
     * If no dstPath is provided or filesystem type it's not supported this method will
     * escape characters contained in the following list: \/:?\"<>|*
     * Otherwise it will check forbidden characters for local filesystem type
     *
     * The input string must be UTF8 encoded. The returned value will be UTF8 too.
     *
     * You take the ownership of the returned value
     *
     * @param fileName Name to convert (UTF8)
     * @param dstPath  Destination path
     * @return Converted name (UTF8)
     */
    suspend fun escapeFsIncompatible(fileName: String, dstPath: String): String?

    /**
     * Current download speed
     */
    val currentDownloadSpeed: Int

    /**
     * Total downloaded bytes
     */
    @Deprecated(
        "This value is deprecated in SDK. " +
                "Replace with the corresponding value get from ActiveTransfers when ready"
    )
    val totalDownloadedBytes: Long

    /**
     * Total download bytes
     */
    @Deprecated(
        "This value is deprecated in SDK. " +
                "Replace with the corresponding value get from ActiveTransfers when ready"
    )
    val totalDownloadBytes: Long

    /**
     * Total downloads
     */
    @Deprecated(
        "This value is deprecated in SDK. " +
                "Replace with the corresponding value get from ActiveTransfers when ready"
    )
    val totalDownloads: Int

    /**
     * Get psa
     *
     * @param listener
     */
    fun getPsa(listener: MegaRequestListenerInterface)

    /**
     * Set psa handled
     *
     * @param psaId
     */
    suspend fun setPsaHandled(psaId: Int)

    /**
     * Set label to Node
     * @param node [MegaNode]
     * @param label Int
     */
    suspend fun setNodeLabel(node: MegaNode, label: Int)

    /**
     * Reset label from node
     * @param node [MegaNode]
     */
    suspend fun resetNodeLabel(node: MegaNode)

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

    /**
     * Copy a mega push notifications settings
     *
     * @param pushNotificationSettings the mega push notification to copy
     * @return the copied push notification settings
     */
    suspend fun copyMegaPushNotificationsSettings(pushNotificationSettings: MegaPushNotificationSettings): MegaPushNotificationSettings?

    /**
     * Create mega push notifications settings
     *
     * @return the created push notification settings
     */
    fun createInstanceMegaPushNotificationSettings(): MegaPushNotificationSettings

    /**
     * Creates a [MegaNode] from its serialized data
     *
     * @param serializedData [String]
     * @return the [MegaNode]
     */
    fun unSerializeNode(serializedData: String): MegaNode?

    /**
     * Check if the sending of geolocation messages is enabled
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_GEOLOCATION
     * <p>
     * Sending a Geolocation message is enabled if the MegaRequest object, received in onRequestFinish,
     * has error code MegaError::API_OK. In other cases, send geolocation messages is not enabled and
     * the application has to answer before send a message of this type.
     *
     * @param listener MegaRequestListener to track this request
     */
    fun isGeolocationEnabled(listener: MegaRequestListenerInterface)

    /**
     * Enable the sending of geolocation messages
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_GEOLOCATION
     *
     * @param listener MegaRequestListener to track this request
     */
    fun enableGeolocation(listener: MegaRequestListenerInterface)

    /**
     * check if Cookie Banner is enabled on SDK
     *
     * @return true if Cookie Banner is enabled, false otherwise
     */
    fun isCookieBannerEnabled(): Boolean


    /**
     * Fetch miscellaneous flags when not logged in
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_GET_MISC_FLAGS.
     * <p>
     * When onRequestFinish is called with MegaError::API_OK, the miscellaneous flags are available.
     * If you are logged in into an account, the error code provided in onRequestFinish is
     * MegaError::API_EACCESS.
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
    fun getMiscFlags(listener: OptionalMegaRequestListenerInterface)

    /**
     * Get cookie settings from SDK
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
    fun getCookieSettings(listener: OptionalMegaRequestListenerInterface)

    /**
     * Set cookie settings from SDK
     *
     * @param bitSetToDecimal Int with cookie settings
     * @param listener MegaRequestListenerInterface to track this request
     */
    fun setCookieSettings(bitSetToDecimal: Int, listener: OptionalMegaRequestListenerInterface)

    /**
     * Check if the app should show the rich link warning dialog to the user
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_RICH_PREVIEWS
     * - MegaRequest::getNumDetails - Returns one
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getFlag - Returns true if it is necessary to show the rich link warning
     * - MegaRequest::getNumber - Returns the number of times that user has indicated that doesn't want
     * modify the message with a rich link. If number is bigger than three, the extra option "Never"
     * must be added to the warning dialog.
     * - MegaRequest::getMegaStringMap - Returns the raw content of the attribute: [<key><value>]*
     * <p>
     * If the corresponding user attribute is not set yet, the request will fail with the
     * error code MegaError::API_ENOENT, but the value of MegaRequest::getFlag will still be valid (true).
     *
     * @param listener MegaRequestListener to track this request
     */
    fun shouldShowRichLinkWarning(listener: MegaRequestListenerInterface)

    /**
     * Check if rich previews are automatically generated
     * <p>
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_RICH_PREVIEWS
     * - MegaRequest::getNumDetails - Returns zero
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getFlag - Returns true if generation of rich previews is enabled
     * - MegaRequest::getMegaStringMap - Returns the raw content of the attribute: [<key><value>]*
     * <p>
     * If the corresponding user attribute is not set yet, the request will fail with the
     * error code MegaError::API_ENOENT, but the value of MegaRequest::getFlag will still be valid (false).
     *
     * @param listener MegaRequestListener to track this request
     */
    fun isRichPreviewsEnabled(listener: MegaRequestListenerInterface)

    /**
     * Set the number of times "Not now" option has been selected in the rich link warning dialog
     *
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_RICH_PREVIEWS
     *
     * @param value    Number of times "Not now" option has been selected
     * @param listener MegaRequestListener to track this request
     */
    fun setRichLinkWarningCounterValue(value: Int, listener: MegaRequestListenerInterface)

    /**
     * Enable or disable the generation of rich previews
     *
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_RICH_PREVIEWS
     *
     * @param enable   True to enable the generation of rich previews
     * @param listener MegaRequestListener to track this request
     */
    fun enableRichPreviews(enable: Boolean, listener: MegaRequestListenerInterface)
}
