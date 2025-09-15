package mega.privacy.android.data.facade

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.IgnoredRequestListener
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.model.RequestEvent
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaEvent
import nz.mega.sdk.MegaFlag
import nz.mega.sdk.MegaGlobalListenerInterface
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaLoggerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaPushNotificationSettings
import nz.mega.sdk.MegaPushNotificationSettingsAndroid
import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRecentActionBucketList
import nz.mega.sdk.MegaRecentActionBucketListAndroid
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaSearchFilter
import nz.mega.sdk.MegaSearchPage
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaSetElementList
import nz.mega.sdk.MegaSetList
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaStringList
import nz.mega.sdk.MegaStringMap
import nz.mega.sdk.MegaSyncList
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferData
import nz.mega.sdk.MegaTransferListenerInterface
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mega api facade
 *
 * Implements [MegaApiGateway] and provides a facade over [MegaApiAndroid]
 *
 * @property megaApi
 */
@Singleton
internal class MegaApiFacade @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationScope private val sharingScope: CoroutineScope,
) : MegaApiGateway {

    override fun getWaitingReason() = megaApi.isWaiting

    override fun getInvalidHandle(): Long = MegaApiAndroid.INVALID_HANDLE

    override fun getInvalidAffiliateType(): Int = MegaApiAndroid.AFFILIATE_TYPE_INVALID

    override fun getInvalidBackupType() = MegaApiAndroid.BACKUP_TYPE_INVALID

    override fun multiFactorAuthAvailable(): Boolean = megaApi.multiFactorAuthAvailable()

    override fun multiFactorAuthEnabled(email: String?, listener: MegaRequestListenerInterface?) {
        megaApi.multiFactorAuthCheck(email, listener)
    }

    override fun cancelAccount(listener: MegaRequestListenerInterface?) {
        megaApi.cancelAccount(listener)
    }

    override fun createSupportTicket(
        ticketContent: String,
        listener: MegaRequestListenerInterface,
    ) {
        megaApi.createSupportTicket(ticketContent, ANDROID_SUPPORT_ISSUE, listener)
    }

    override fun getNodesFromMegaNodeList(nodeList: MegaNodeList): List<MegaNode> =
        MegaApiJava.nodeListToArray(nodeList)?.toList() ?: emptyList()

    override fun startUpload(
        localPath: String,
        parentNode: MegaNode,
        fileName: String?,
        modificationTime: Long?,
        appData: String?,
        isSourceTemporary: Boolean,
        shouldStartFirst: Boolean,
        cancelToken: MegaCancelToken?,
        listener: MegaTransferListenerInterface,
    ) {
        megaApi.startUpload(
            localPath,
            parentNode,
            fileName,
            modificationTime ?: INVALID_CUSTOM_MOD_TIME,
            appData,
            isSourceTemporary,
            shouldStartFirst,
            cancelToken,
            listener,
        )
    }

    override fun addTransferListener(listener: MegaTransferListenerInterface) =
        megaApi.addTransferListener(listener)

    override fun removeTransferListener(listener: MegaTransferListenerInterface) =
        megaApi.removeTransferListener(listener)

    override fun startUploadForSupport(
        path: String,
        listener: MegaTransferListenerInterface,
    ) {
        megaApi.startUploadForSupport(path, false, listener)
    }

    override val myUser: MegaUser?
        get() = megaApi.myUser
    override val myUserHandle: Long
        get() = megaApi.myUserHandleBinary
    override val accountEmail: String?
        get() = megaApi.myEmail
    override val isBusinessAccount: Boolean
        get() = megaApi.isBusinessAccount

    override val isEphemeralPlusPlus: Boolean
        get() = megaApi.isEphemeralPlusPlus

    override suspend fun getAccountAuth(): String? = megaApi.accountAuth
    override val myCredentials: String?
        get() = megaApi.myCredentials
    override val dumpSession: String?
        get() = megaApi.dumpSession()
    override val businessStatus: Int
        get() = megaApi.businessStatus
    override val isAchievementsEnabled: Boolean
        get() = megaApi.isAchievementsEnabled

    override suspend fun isMasterBusinessAccount(): Boolean = megaApi.isMasterBusinessAccount

    override suspend fun getBusinessStatus(): Int = megaApi.businessStatus

    override suspend fun areUploadTransfersPaused(): Boolean =
        megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)

    override suspend fun areDownloadTransfersPaused(): Boolean =
        megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD)

    override suspend fun getRootNode(): MegaNode? = megaApi.rootNode

    override suspend fun getRubbishBinNode(): MegaNode? = megaApi.rubbishNode

    override suspend fun getSdkVersion(): String? = megaApi.version

    override val globalRequestEvents = callbackFlow {
        val listener = object : MegaRequestListenerInterface {
            override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
                trySend(RequestEvent.OnRequestFinish(request, e))
            }

            override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
                trySend(RequestEvent.OnRequestStart(request))
            }

            override fun onRequestTemporaryError(
                api: MegaApiJava,
                request: MegaRequest,
                e: MegaError,
            ) {
                trySend(RequestEvent.OnRequestTemporaryError(request, e))
            }

            override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
                trySend(RequestEvent.OnRequestUpdate(request))
            }

        }
        megaApi.addRequestListener(listener)
        awaitClose { megaApi.removeRequestListener(listener) }
    }.shareIn(
        sharingScope,
        SharingStarted.WhileSubscribed()
    )

    override val globalUpdates: Flow<GlobalUpdate> = callbackFlow {
        val listener = object : MegaGlobalListenerInterface {
            override fun onUsersUpdate(
                api: MegaApiJava,
                users: ArrayList<MegaUser>?,
            ) {
                Timber.d("Global update onUsersUpdate")
                trySend(GlobalUpdate.OnUsersUpdate(users))
            }

            override fun onUserAlertsUpdate(
                api: MegaApiJava,
                userAlerts: ArrayList<MegaUserAlert>?,
            ) {
                Timber.d("Global update onUserAlertsUpdate")
                trySend(GlobalUpdate.OnUserAlertsUpdate(userAlerts))
            }

            override fun onNodesUpdate(
                api: MegaApiJava,
                nodeList: ArrayList<MegaNode>?,
            ) {
                Timber.d("Global update onNodesUpdate")
                trySend(GlobalUpdate.OnNodesUpdate(nodeList))
            }

            override fun onAccountUpdate(api: MegaApiJava) {
                Timber.d("Global update onAccountUpdate")
                trySend(GlobalUpdate.OnAccountUpdate)
            }

            override fun onContactRequestsUpdate(
                api: MegaApiJava,
                requests: ArrayList<MegaContactRequest>?,
            ) {
                Timber.d("Global update onContactRequestsUpdate")
                trySend(GlobalUpdate.OnContactRequestsUpdate(requests))
            }

            override fun onEvent(api: MegaApiJava, event: MegaEvent?) {
                Timber.d("Global update onEvent")
                if (event?.type == MegaEvent.EVENT_RELOADING) {
                    trySend(GlobalUpdate.OnReloadNeeded)
                }
                trySend(GlobalUpdate.OnEvent(event))
            }

            override fun onSetsUpdate(api: MegaApiJava, sets: ArrayList<MegaSet>?) {
                Timber.d("Global update onSetsUpdate")
                trySend(GlobalUpdate.OnSetsUpdate(sets))
            }

            override fun onSetElementsUpdate(
                api: MegaApiJava,
                elements: ArrayList<MegaSetElement>?,
            ) {
                Timber.d("Global update onSetElementsUpdate")
                trySend(GlobalUpdate.OnSetElementsUpdate(elements))
            }

            override fun onGlobalSyncStateChanged(api: MegaApiJava) {
                Timber.d("Global update onGlobalSyncStateChanged")
                trySend(GlobalUpdate.OnGlobalSyncStateChanged)
            }
        }

        megaApi.addGlobalListener(listener)

        awaitClose { megaApi.removeGlobalListener(listener) }
    }.shareIn(
        sharingScope,
        SharingStarted.WhileSubscribed()
    )

    override val globalTransfer: Flow<GlobalTransfer> = callbackFlow {
        val listener = OptionalMegaTransferListenerInterface(
            onTransferStart = { transfer ->
                trySend(GlobalTransfer.OnTransferStart(transfer))
            },
            onTransferFinish = { transfer, error ->
                trySend(GlobalTransfer.OnTransferFinish(transfer, error))
            },
            onTransferUpdate = { transfer ->
                trySend(GlobalTransfer.OnTransferUpdate(transfer))
            },
            onTransferTemporaryError = { transfer, error ->
                trySend(GlobalTransfer.OnTransferTemporaryError(transfer, error))
            },
            onTransferData = { transfer, buffer ->
                trySend(GlobalTransfer.OnTransferData(transfer, buffer))
            },
            onFolderTransferUpdate = {
                    transfer,
                    stage,
                    folderCount,
                    createdFolderCount,
                    fileCount,
                    currentFolder,
                    currentFileLeafName,
                ->
                trySend(
                    GlobalTransfer.OnFolderTransferUpdate(
                        transfer,
                        stage,
                        folderCount,
                        createdFolderCount,
                        fileCount,
                        currentFolder,
                        currentFileLeafName
                    )
                )
            },
        )

        addTransferListener(listener)

        awaitClose {
            removeTransferListener(listener)
        }
    }.buffer(Channel.Factory.UNLIMITED).shareIn(sharingScope, SharingStarted.WhileSubscribed())

    override fun getFavourites(
        node: MegaNode?,
        count: Int,
        listener: MegaRequestListenerInterface?,
    ) {
        megaApi.getFavourites(node, count, listener)
    }

    override suspend fun getMegaNodeByHandle(nodeHandle: Long): MegaNode? =
        megaApi.getNodeByHandle(nodeHandle)

    override suspend fun getNodeByPath(path: String?, megaNode: MegaNode?): MegaNode? =
        megaApi.getNodeByPath(path, megaNode)

    override suspend fun getFingerprint(filePath: String): String? =
        megaApi.getFingerprint(filePath)

    override suspend fun getNodesByOriginalFingerprint(
        originalFingerprint: String,
        parentNode: MegaNode?,
    ): MegaNodeList? = megaApi.getNodesByOriginalFingerprint(originalFingerprint, parentNode)

    override suspend fun getNodeByFingerprintAndParentNode(
        fingerprint: String,
        parentNode: MegaNode?,
    ): MegaNode? = megaApi.getNodeByFingerprint(fingerprint, parentNode)

    override suspend fun getNodeByFingerprint(fingerprint: String): MegaNode? =
        megaApi.getNodeByFingerprint(fingerprint)

    override fun setOriginalFingerprint(
        node: MegaNode,
        originalFingerprint: String,
        listener: MegaRequestListenerInterface?,
    ) {
        megaApi.setOriginalFingerprint(node, originalFingerprint, listener)
    }

    override suspend fun hasVersion(node: MegaNode): Boolean = megaApi.hasVersions(node)

    override suspend fun getNumVersions(node: MegaNode): Int = megaApi.getNumVersions(node)

    override suspend fun getVersions(node: MegaNode): List<MegaNode> = megaApi.getVersions(node)

    override fun deleteVersion(
        nodeVersion: MegaNode,
        listener: MegaRequestListenerInterface,
    ) = megaApi.removeVersion(nodeVersion, listener)

    override suspend fun getParentNode(node: MegaNode): MegaNode? = megaApi.getParentNode(node)

    override suspend fun getChildNode(parentNode: MegaNode?, name: String?): MegaNode? =
        megaApi.getChildNode(parentNode, name)

    override suspend fun getIncomingSharesNode(order: Int?): List<MegaNode> =
        if (order == null)
            megaApi.inShares
        else
            megaApi.getInShares(order)

    override suspend fun getOutgoingSharesNode(order: Int?): List<MegaShare> =
        if (order == null)
            megaApi.outShares
        else
            megaApi.getOutShares(order)

    override suspend fun getUnverifiedIncomingShares(order: Int): List<MegaShare> =
        megaApi.getUnverifiedIncomingShares(order)

    override suspend fun getVerifiedIncomingShares(order: Int?): List<MegaShare> =
        if (order == null)
            megaApi.inSharesList
        else
            megaApi.getInSharesList(order)

    override suspend fun isPendingShare(node: MegaNode): Boolean = megaApi.isPendingShare(node)

    override suspend fun getPublicLinks(order: Int?): List<MegaNode> =
        if (order == null)
            megaApi.publicLinks
        else
            megaApi.getPublicLinks(order)


    override suspend fun getNumChildFolders(node: MegaNode): Int = megaApi.getNumChildFolders(node)

    override suspend fun getNumChildFiles(node: MegaNode): Int = megaApi.getNumChildFiles(node)

    override fun setContactLinksOption(enable: Boolean, listener: MegaRequestListenerInterface) =
        megaApi.setContactLinksOption(enable, listener)

    override fun getContactLinksOption(listener: MegaRequestListenerInterface) =
        megaApi.getContactLinksOption(listener)

    override fun getFolderInfo(node: MegaNode?, listener: MegaRequestListenerInterface) =
        megaApi.getFolderInfo(node, listener)

    override fun setNodeFavourite(node: MegaNode?, favourite: Boolean) {
        megaApi.setNodeFavourite(node, favourite)
    }

    override fun setNodeSensitive(
        node: MegaNode?,
        sensitive: Boolean,
        listener: MegaRequestListenerInterface?,
    ) {
        megaApi.setNodeSensitive(node, sensitive, listener)
    }

    override suspend fun isSensitiveInherited(node: MegaNode): Boolean {
        val parentNode = megaApi.getParentNode(node) ?: return false
        return megaApi.isSensitiveInherited(parentNode)
    }

    override fun addLogger(logger: MegaLoggerInterface) = MegaApiAndroid.addLoggerObject(logger)

    override fun removeLogger(logger: MegaLoggerInterface) =
        MegaApiAndroid.removeLoggerObject(logger)

    override fun setLogLevel(logLevel: Int) = MegaApiAndroid.setLogLevel(logLevel)

    override fun setUseHttpsOnly(enabled: Boolean) = megaApi.useHttpsOnly(enabled)

    override suspend fun getLoggedInUser(): MegaUser? = megaApi.myUser

    override fun getThumbnail(
        node: MegaNode,
        thumbnailFilePath: String,
        listener: MegaRequestListenerInterface?,
    ) {
        if (listener == null) {
            megaApi.getThumbnail(node, thumbnailFilePath)
        } else {
            megaApi.getThumbnail(node, thumbnailFilePath, listener)
        }
    }

    override fun handleToBase64(handle: Long): String = MegaApiAndroid.handleToBase64(handle)

    override fun base64ToHandle(base64Handle: String): Long =
        MegaApiAndroid.base64ToHandle(base64Handle)

    override fun cancelTransfer(transfer: MegaTransfer, listener: MegaRequestListenerInterface?) {
        if (listener != null) {
            megaApi.cancelTransfer(transfer, listener)
        } else {
            megaApi.cancelTransfer(transfer)
        }
    }

    override suspend fun getNumUnreadUserAlerts(): Int = megaApi.numUnreadUserAlerts

    override suspend fun getBackupsNode(): MegaNode? = megaApi.vaultNode

    override suspend fun hasChildren(node: MegaNode): Boolean = megaApi.hasChildren(node)

    override fun registerPushNotifications(
        deviceType: Int,
        newToken: String,
        listener: MegaRequestListenerInterface,
    ) = megaApi.registerPushNotifications(deviceType, newToken, listener)

    override fun fastLogin(session: String, listener: MegaRequestListenerInterface) =
        megaApi.fastLogin(session, listener)

    override fun fetchNodes(listener: MegaRequestListenerInterface) =
        megaApi.fetchNodes(listener)

    override fun retryPendingConnections() = megaApi.retryPendingConnections()

    override suspend fun getTransfers(type: Int): List<MegaTransfer> =
        megaApi.getTransfers(type) ?: emptyList()

    override suspend fun getTransferByTag(tag: Int): MegaTransfer? = megaApi.getTransferByTag(tag)

    override suspend fun getTransferByUniqueId(id: Long): MegaTransfer? =
        megaApi.getTransferByUniqueId(id)

    override fun startDownload(
        node: MegaNode,
        localPath: String,
        fileName: String?,
        appData: String?,
        startFirst: Boolean,
        cancelToken: MegaCancelToken?,
        collisionCheck: Int,
        collisionResolution: Int,
        listener: MegaTransferListenerInterface?,
    ) = megaApi.startDownload(
        node,
        localPath,
        fileName,
        appData,
        startFirst,
        cancelToken,
        collisionCheck,
        collisionResolution,
        listener
    )

    override fun getUserEmail(userHandle: Long, callback: MegaRequestListenerInterface) =
        megaApi.getUserEmail(userHandle, callback)

    override suspend fun getContact(emailOrBase64Handle: String): MegaUser? =
        megaApi.getContact(emailOrBase64Handle)

    override suspend fun getUserAlerts(): List<MegaUserAlert> = megaApi.userAlerts

    /**
     * This is marked as deprecated in SDK because this function is for internal usage of MEGA apps
     * for debug purposes. This info is sent to MEGA servers.
     */
    @Suppress("DEPRECATION")
    override suspend fun sendEvent(
        eventId: Int,
        message: String,
        addJourneyId: Boolean,
        viewId: String?,
    ) = megaApi.sendEvent(eventId, message, addJourneyId, viewId)

    override suspend fun generateViewId(): String = megaApi.generateViewId()

    override suspend fun getUserAvatarColor(megaUser: MegaUser): String? =
        megaApi.getUserAvatarColor(megaUser)

    override suspend fun getUserAvatarColor(userHandle: Long): String? =
        megaApi.getUserAvatarColor(userHandleToBase64(userHandle))

    override fun getUserAvatar(
        user: MegaUser,
        destinationPath: String,
        listener: MegaRequestListenerInterface,
    ) = megaApi.getUserAvatar(user, destinationPath, listener)

    override suspend fun acknowledgeUserAlerts() {
        megaApi.acknowledgeUserAlerts()
    }

    override suspend fun getIncomingContactRequests(): ArrayList<MegaContactRequest> =
        megaApi.incomingContactRequests

    override suspend fun getContactRequestByHandle(requestHandle: Long): MegaContactRequest? =
        megaApi.getContactRequestByHandle(requestHandle)

    override fun replyReceivedContactRequest(
        contactRequest: MegaContactRequest,
        action: Int,
        listener: MegaRequestListenerInterface,
    ) = megaApi.replyContactRequest(contactRequest, action, listener)

    override fun sendInvitedContactRequest(
        email: String,
        message: String,
        action: Int,
        listener: MegaRequestListenerInterface,
    ) = megaApi.inviteContact(email, message, action, listener)

    override suspend fun getPublicLinks(): List<MegaNode> = megaApi.publicLinks

    override fun getPreview(
        node: MegaNode,
        previewFilePath: String,
        listener: MegaRequestListenerInterface,
    ) = megaApi.getPreview(node, previewFilePath, listener)

    override fun getFullImage(
        node: MegaNode,
        fullFile: File,
        highPriority: Boolean,
        listener: MegaTransferListenerInterface,
    ) {
        megaApi.startDownload(
            node,
            fullFile.absolutePath,
            fullFile.name,
            AppDataTypeConstants.BackgroundTransfer.sdkTypeValue,
            highPriority,
            null,
            MegaTransfer.COLLISION_CHECK_FINGERPRINT,
            MegaTransfer.COLLISION_RESOLUTION_NEW_WITH_N,
            listener
        )
    }

    override suspend fun isInRubbish(node: MegaNode): Boolean = megaApi.isInRubbish(node)

    override suspend fun isInBackups(node: MegaNode): Boolean = megaApi.isInVault(node)

    override suspend fun isInCloudDrive(node: MegaNode): Boolean = megaApi.isInCloud(node)

    override suspend fun moveTransferToLast(
        transfer: MegaTransfer,
        listener: MegaRequestListenerInterface,
    ) = megaApi.moveTransferToLast(transfer, listener)

    override suspend fun moveTransferBefore(
        transfer: MegaTransfer,
        prevTransfer: MegaTransfer,
        listener: MegaRequestListenerInterface,
    ) = megaApi.moveTransferBefore(transfer, prevTransfer, listener)

    override suspend fun moveTransferToFirst(
        transfer: MegaTransfer,
        listener: MegaRequestListenerInterface,
    ) = megaApi.moveTransferToFirst(transfer, listener)

    override fun moveTransferToLastByTag(
        transferTag: Int,
        listener: MegaRequestListenerInterface,
    ) = megaApi.moveTransferToLastByTag(transferTag, listener)

    override fun moveTransferBeforeByTag(
        transferTag: Int,
        prevTransferTag: Int,
        listener: MegaRequestListenerInterface,
    ) = megaApi.moveTransferBeforeByTag(transferTag, prevTransferTag, listener)

    override fun moveTransferToFirstByTag(
        transferTag: Int,
        listener: MegaRequestListenerInterface,
    ) = megaApi.moveTransferToFirstByTag(transferTag, listener)

    override suspend fun isBusinessAccountActive(): Boolean = megaApi.isBusinessAccountActive

    companion object {
        private const val ANDROID_SUPPORT_ISSUE = 10
        private const val INVALID_CUSTOM_MOD_TIME = -1L
    }

    override suspend fun getContacts(): List<MegaUser> = megaApi.contacts

    override suspend fun areCredentialsVerified(megaUser: MegaUser): Boolean =
        megaApi.areCredentialsVerified(megaUser)

    override fun getUserAlias(userHandle: Long, listener: MegaRequestListenerInterface) =
        megaApi.getUserAlias(userHandle, listener)

    override fun getContactAvatar(
        emailOrHandle: String,
        path: String,
        listener: MegaRequestListenerInterface,
    ) = megaApi.getUserAvatar(emailOrHandle, path, listener)

    override fun getUserAttribute(
        emailOrHandle: String,
        type: Int,
        listener: MegaRequestListenerInterface,
    ) = megaApi.getUserAttribute(emailOrHandle, type, listener)

    override fun userHandleToBase64(userHandle: Long): String =
        MegaApiJava.userHandleToBase64(userHandle)

    override fun getUserAttribute(
        user: MegaUser,
        type: Int,
        listener: MegaRequestListenerInterface,
    ) = megaApi.getUserAttribute(user, type, listener)

    override fun getRecentActionsAsync(
        days: Long,
        maxNodes: Long,
        excludeSensitives: Boolean,
        listener: MegaRequestListenerInterface,
    ) = megaApi.getRecentActionsAsync(days, maxNodes, excludeSensitives, listener)

    override fun copyNode(
        nodeToCopy: MegaNode,
        newNodeParent: MegaNode,
        newNodeName: String?,
        listener: MegaRequestListenerInterface?,
    ) {
        when {
            newNodeName == null && listener == null -> {
                megaApi.copyNode(nodeToCopy, newNodeParent)
            }

            newNodeName != null && listener == null -> {
                megaApi.copyNode(nodeToCopy, newNodeParent, newNodeName)
            }

            newNodeName == null && listener != null -> {
                megaApi.copyNode(nodeToCopy, newNodeParent, listener)
            }

            else /*newNodeName != null && listener != null*/ -> {
                megaApi.copyNode(nodeToCopy, newNodeParent, newNodeName, listener)
            }
        }
    }

    override fun moveNode(
        nodeToMove: MegaNode,
        newNodeParent: MegaNode,
        newNodeName: String?,
        listener: MegaRequestListenerInterface?,
    ) {
        when {
            newNodeName == null && listener == null -> {
                megaApi.moveNode(nodeToMove, newNodeParent)
            }

            newNodeName != null && listener == null -> {
                megaApi.moveNode(nodeToMove, newNodeParent, newNodeName)
            }

            newNodeName == null && listener != null -> {
                megaApi.moveNode(nodeToMove, newNodeParent, listener)
            }

            else /*newNodeName != null && listener != null*/ -> {
                megaApi.moveNode(nodeToMove, newNodeParent, newNodeName, listener)
            }
        }
    }

    override fun deleteNode(
        node: MegaNode,
        listener: MegaRequestListenerInterface?,
    ) {
        listener?.let {
            megaApi.remove(node, it)
        } ?: run {
            megaApi.remove(node)
        }
    }

    override fun copyBucketList(bucketList: MegaRecentActionBucketList): MegaRecentActionBucketList =
        MegaRecentActionBucketListAndroid.copy(bucketList)

    override fun copyBucket(bucket: MegaRecentActionBucket): MegaRecentActionBucket =
        megaApi.copyBucket(bucket)

    override fun checkAccessErrorExtended(node: MegaNode, level: Int): MegaError =
        megaApi.checkAccessErrorExtended(node, level)

    override fun checkMoveErrorExtended(node: MegaNode, targetNode: MegaNode): MegaError =
        megaApi.checkMoveErrorExtended(node, targetNode)

    override fun getPricing(listener: MegaRequestListenerInterface?) {
        megaApi.getPricing(listener)
    }

    override fun getPaymentMethods(listener: MegaRequestListenerInterface?) {
        megaApi.getPaymentMethods(listener)
    }

    override fun getAccountDetails(listener: MegaRequestListenerInterface?) {
        megaApi.getAccountDetails(listener)
    }

    override fun getSpecificAccountDetails(
        storage: Boolean,
        transfer: Boolean,
        pro: Boolean,
        listener: MegaRequestListenerInterface,
    ) {
        megaApi.getSpecificAccountDetails(storage, transfer, pro, listener)
    }

    override fun creditCardQuerySubscriptions(listener: MegaRequestListenerInterface?) {
        megaApi.creditCardQuerySubscriptions(listener)
    }

    override fun getUserAttribute(
        attributeIdentifier: Int,
        listener: MegaRequestListenerInterface,
    ) {
        megaApi.getUserAttribute(attributeIdentifier, listener)
    }

    override suspend fun areAccountAchievementsEnabled(): Boolean = megaApi.isAchievementsEnabled

    override fun getAccountAchievements(listener: MegaRequestListenerInterface?) =
        megaApi.getAccountAchievements(listener)

    override suspend fun authorizeNode(node: MegaNode): MegaNode? = megaApi.authorizeNode(node)

    override suspend fun httpServerGetLocalLink(node: MegaNode): String? =
        megaApi.httpServerGetLocalLink(node)

    override suspend fun httpServerIsRunning() = megaApi.httpServerIsRunning()

    override suspend fun httpServerStart() = megaApi.httpServerStart()

    override suspend fun httpServerStop() = megaApi.httpServerStop()

    override suspend fun httpServerSetMaxBufferSize(bufferSize: Int) =
        megaApi.httpServerSetMaxBufferSize(bufferSize)

    override suspend fun getPublicLinks(order: Int): List<MegaNode> = megaApi.getPublicLinks(order)

    override suspend fun getInShares(order: Int): List<MegaNode> = megaApi.getInShares(order)

    override suspend fun getInShares(user: MegaUser): List<MegaNode> = megaApi.getInShares(user)

    override suspend fun getUserFromInShare(node: MegaNode, recursive: Boolean): MegaUser? =
        megaApi.getUserFromInShare(node, recursive)

    override suspend fun getOutShares(order: Int): List<MegaShare> = megaApi.getOutShares(order)

    override suspend fun getOutShares(megaNode: MegaNode): List<MegaShare> =
        megaApi.getOutShares(megaNode)

    override fun createSet(name: String, type: Int, listener: MegaRequestListenerInterface) =
        megaApi.createSet(name, type, listener)

    override fun createSetElement(sid: Long, node: Long, listener: MegaRequestListenerInterface) =
        megaApi.createSetElement(sid, node, "", listener)

    override fun createSetElements(
        sid: Long,
        nodes: MegaHandleList,
        names: MegaStringList?,
        listener: MegaRequestListenerInterface,
    ) = megaApi.createSetElements(sid, nodes, names, listener)

    override suspend fun removeSetElement(
        sid: Long,
        eid: Long,
        listener: MegaRequestListenerInterface,
    ) =
        megaApi.removeSetElement(sid, eid, listener)

    override suspend fun getSets(): MegaSetList = megaApi.sets

    override suspend fun getSet(sid: Long): MegaSet? = megaApi.getSet(sid)

    override suspend fun getSetElements(sid: Long): MegaSetElementList =
        megaApi.getSetElements(sid)

    override fun removeSet(sid: Long, listener: MegaRequestListenerInterface) =
        megaApi.removeSet(sid, listener)

    override fun updateSetName(sid: Long, name: String?, listener: MegaRequestListenerInterface?) =
        megaApi.updateSetName(sid, name, listener)

    override fun updateSetName(sid: Long, name: String?) =
        megaApi.updateSetName(sid, name)

    override suspend fun putSetCover(sid: Long, eid: Long) = megaApi.putSetCover(sid, eid)

    override fun exportSet(sid: Long, listener: MegaRequestListenerInterface) {
        megaApi.exportSet(sid, listener)
    }

    override fun disableExportSet(sid: Long, listener: MegaRequestListenerInterface) {
        megaApi.disableExportSet(sid, listener)
    }

    override fun fetchPublicSet(publicSetLink: String, listener: MegaRequestListenerInterface) {
        megaApi.fetchPublicSet(publicSetLink, listener)
    }

    override fun stopPublicSetPreview() {
        megaApi.stopPublicSetPreview()
    }

    override fun getPreviewElementNode(eid: Long, listener: MegaRequestListenerInterface) {
        megaApi.getPreviewElementNode(eid, listener)
    }

    override fun removeRequestListener(listener: MegaRequestListenerInterface) {
        sharingScope.launch {
            megaApi.removeRequestListener(listener)
        }
    }

    override fun getUserCredentials(user: MegaUser, listener: MegaRequestListenerInterface) =
        megaApi.getUserCredentials(user, listener)

    override fun resetCredentials(user: MegaUser, listener: MegaRequestListenerInterface) =
        megaApi.resetCredentials(user, listener)

    override fun verifyCredentials(user: MegaUser, listener: MegaRequestListenerInterface) =
        megaApi.verifyCredentials(user, listener)

    override suspend fun isCurrentPassword(password: String) =
        megaApi.checkPassword(password)

    override fun changePassword(newPassword: String, listener: MegaRequestListenerInterface) =
        megaApi.changePassword(null, newPassword, listener)

    override fun resetPasswordFromLink(
        link: String?,
        newPassword: String,
        masterKey: String?,
        listener: MegaRequestListenerInterface,
    ) = megaApi.confirmResetPassword(link, newPassword, masterKey, listener)

    override fun enableMultiFactorAuth(pin: String, listener: MegaRequestListenerInterface?) {
        megaApi.multiFactorAuthEnable(pin, listener)
    }

    override fun isMasterKeyExported(listener: MegaRequestListenerInterface?) {
        megaApi.isMasterKeyExported(listener)
    }

    override fun getMultiFactorAuthCode(listener: MegaRequestListenerInterface?) {
        megaApi.multiFactorAuthGetCode(listener)
    }

    override suspend fun getPasswordStrength(password: String) =
        megaApi.getPasswordStrength(password)

    override fun getCountryCallingCodes(listener: MegaRequestListenerInterface) =
        megaApi.getCountryCallingCodes(listener)

    override fun logout(listener: MegaRequestListenerInterface?) {
        if (listener == null) {
            megaApi.logout()
        } else {
            megaApi.logout(listener)
        }
    }

    override fun sendSMSVerificationCode(
        phoneNumber: String,
        reVerifyingWhitelisted: Boolean,
        listener: MegaRequestListenerInterface,
    ) {
        if (reVerifyingWhitelisted) {
            megaApi.sendSMSVerificationCode(phoneNumber, listener, true)
        } else {
            megaApi.sendSMSVerificationCode(phoneNumber, listener)
        }
    }

    override fun resetSmsVerifiedPhoneNumber(listener: MegaRequestListenerInterface?) {
        if (listener == null) {
            megaApi.resetSmsVerifiedPhoneNumber()
        } else {
            megaApi.resetSmsVerifiedPhoneNumber(listener)
        }
    }

    override fun getExtendedAccountDetails(
        sessions: Boolean,
        purchases: Boolean,
        transactions: Boolean,
        listener: MegaRequestListenerInterface,
    ) {
        megaApi.getExtendedAccountDetails(sessions, purchases, transactions, listener)
    }

    override fun contactLinkCreate(renew: Boolean, listener: MegaRequestListenerInterface) {
        megaApi.contactLinkCreate(renew, listener)
    }

    override fun contactLinkDelete(handle: Long, listener: MegaRequestListenerInterface) {
        megaApi.contactLinkDelete(handle, listener)
    }

    override fun isChatNotifiable(chatId: Long): Boolean =
        megaApi.isChatNotifiable(chatId)

    override fun getPushNotificationSettings(listener: MegaRequestListenerInterface) =
        megaApi.getPushNotificationSettings(listener)

    override fun setPushNotificationSettings(
        settings: MegaPushNotificationSettings,
        listener: MegaRequestListenerInterface,
    ) = megaApi.setPushNotificationSettings(settings, listener)

    override fun inviteContact(email: String, listener: MegaRequestListenerInterface) =
        megaApi.inviteContact(email, null, MegaContactRequest.INVITE_ACTION_ADD, listener)

    override fun inviteContact(
        email: String,
        handle: Long,
        message: String?,
        listener: MegaRequestListenerInterface,
    ) = megaApi.inviteContact(
        email,
        message,
        MegaContactRequest.INVITE_ACTION_ADD,
        handle,
        listener
    )

    override suspend fun getOutgoingContactRequests(): ArrayList<MegaContactRequest> =
        megaApi.outgoingContactRequests

    override fun createFolder(
        name: String,
        parent: MegaNode,
        listener: MegaRequestListenerInterface,
    ) = megaApi.createFolder(name, parent, listener)

    override fun setCameraUploadsFolders(
        primaryFolder: Long,
        secondaryFolder: Long,
        listener: MegaRequestListenerInterface,
    ) = megaApi.setCameraUploadsFolders(primaryFolder, secondaryFolder, listener)

    override fun renameNode(
        node: MegaNode,
        newName: String,
        listener: MegaRequestListenerInterface,
    ) = megaApi.renameNode(node, newName, listener)

    override fun authorizeChatNode(node: MegaNode, authorizationToken: String): MegaNode? =
        megaApi.authorizeChatNode(node, authorizationToken)

    override fun submitPurchaseReceipt(
        gateway: Int,
        receipt: String?,
        listener: MegaRequestListenerInterface,
    ) = megaApi.submitPurchaseReceipt(gateway, receipt, listener)

    override fun setMyChatFilesFolder(nodeHandle: Long, listener: MegaRequestListenerInterface) =
        megaApi.setMyChatFilesFolder(
            nodeHandle,
            listener,
        )

    override fun getMyChatFilesFolder(listener: MegaRequestListenerInterface) =
        megaApi.getMyChatFilesFolder(listener)

    override fun getFileVersionsOption(listener: MegaRequestListenerInterface) {
        megaApi.getFileVersionsOption(listener)
    }

    override fun setFileVersionsOption(disable: Boolean, listener: MegaRequestListenerInterface) {
        megaApi.setFileVersionsOption(disable, listener)
    }

    override fun isMegaApiLoggedIn(): Int = megaApi.isLoggedIn

    override fun cancelTransferByTag(transferTag: Int, listener: MegaRequestListenerInterface?) {
        megaApi.cancelTransferByTag(transferTag, listener)
    }

    override fun getContactLink(handle: Long, listener: MegaRequestListenerInterface) {
        megaApi.contactLinkQuery(handle, listener)
    }

    override fun changeEmail(email: String, listener: MegaRequestListenerInterface) =
        megaApi.changeEmail(email, listener)

    override suspend fun isAccountNew(): Boolean = megaApi.accountIsNew()

    override suspend fun getExportMasterKey(): String? = megaApi.exportMasterKey()

    override fun setMasterKeyExported(listener: MegaRequestListenerInterface?) {
        megaApi.masterKeyExported(listener)
    }

    override fun setUserAttribute(
        type: Int,
        value: String,
        listener: MegaRequestListenerInterface,
    ) = megaApi.setUserAttribute(type, value, listener)

    override fun setUserAttribute(
        type: Int,
        value: MegaStringMap,
        listener: MegaRequestListenerInterface,
    ) = megaApi.setUserAttribute(type, value, listener)

    override fun querySignupLink(link: String, listener: MegaRequestListenerInterface) =
        megaApi.querySignupLink(link, listener)

    override fun queryResetPasswordLink(link: String, listener: MegaRequestListenerInterface) =
        megaApi.queryResetPasswordLink(link, listener)

    override fun getPublicNode(
        nodeFileLink: String,
        listener: MegaRequestListenerInterface,
    ) = megaApi.getPublicNode(nodeFileLink, listener)

    override fun cancelTransfers(direction: Int, listener: MegaRequestListenerInterface) =
        megaApi.cancelTransfers(direction, listener)

    override suspend fun getVerifiedPhoneNumber(): String? = megaApi.smsVerifiedPhoneNumber()

    override fun verifyPhoneNumber(pin: String, listener: MegaRequestListenerInterface) =
        megaApi.checkSMSVerificationCode(pin, listener)

    override fun localLogout(listener: MegaRequestListenerInterface) = megaApi.localLogout(listener)

    override suspend fun searchWithFilter(
        filter: MegaSearchFilter,
        order: Int,
        megaCancelToken: MegaCancelToken,
        megaSearchPage: MegaSearchPage?
    ): List<MegaNode> = megaApi.search(
        filter,
        order,
        megaCancelToken,
        megaSearchPage
    )

    override suspend fun getChildren(
        filter: MegaSearchFilter,
        order: Int,
        megaCancelToken: MegaCancelToken,
        megaSearchPage: MegaSearchPage?,
    ): List<MegaNode> = megaApi.getChildren(
        filter,
        order,
        megaCancelToken,
        megaSearchPage
    )

    override fun openShareDialog(
        megaNode: MegaNode,
        listener: MegaRequestListenerInterface,
    ) = megaApi.openShareDialog(megaNode, listener)

    override fun upgradeSecurity(listener: MegaRequestListenerInterface) =
        megaApi.upgradeSecurity(listener)

    override suspend fun getSmsAllowedState() = megaApi.smsAllowedState()

    override fun login(email: String, password: String, listener: MegaRequestListenerInterface) =
        megaApi.login(email, password, listener)

    override fun multiFactorAuthLogin(
        email: String,
        password: String,
        pin: String,
        listener: MegaRequestListenerInterface,
    ) = megaApi.multiFactorAuthLogin(email, password, pin, listener)

    override suspend fun getNodePath(node: MegaNode): String? = megaApi.getNodePath(node)

    override fun getAccess(megaNode: MegaNode) =
        megaApi.getAccess(megaNode)

    override fun stopSharingNode(megaNode: MegaNode) {
        megaApi.disableExport(megaNode)
    }

    override fun setShareAccess(
        megaNode: MegaNode,
        email: String,
        accessLevel: Int,
        listener: MegaRequestListenerInterface,
    ) = megaApi.share(megaNode, email, accessLevel, listener)

    override fun setAvatar(srcFilePath: String?, listener: MegaRequestListenerInterface) =
        megaApi.setAvatar(srcFilePath, listener)

    override fun skipPasswordReminderDialog(listener: MegaRequestListenerInterface) =
        megaApi.passwordReminderDialogSkipped(listener)

    override fun blockPasswordReminderDialog(listener: MegaRequestListenerInterface) =
        megaApi.passwordReminderDialogBlocked(listener)

    override fun successPasswordReminderDialog(listener: MegaRequestListenerInterface) =
        megaApi.passwordReminderDialogSucceeded(listener)

    override fun setUserAlias(
        userHandle: Long,
        name: String?,
        listener: MegaRequestListenerInterface,
    ) = megaApi.setUserAlias(userHandle, name, listener)

    override suspend fun getTransferData(): MegaTransferData? = megaApi.transferData

    override fun removeContact(user: MegaUser, listener: MegaRequestListenerInterface?) =
        listener?.let {
            megaApi.removeContact(user, it)
        } ?: megaApi.removeContact(user)

    override fun sendBackupHeartbeat(
        backupId: Long,
        status: Int,
        progress: Int,
        ups: Int,
        downs: Int,
        ts: Long,
        lastNode: Long,
        listener: MegaRequestListenerInterface?,
    ) {
        megaApi.sendBackupHeartbeat(backupId, status, progress, ups, downs, ts, lastNode, listener)
    }

    override fun updateBackup(
        backupId: Long,
        backupType: Int,
        targetNode: Long,
        localFolder: String?,
        backupName: String?,
        state: Int,
        subState: Int,
        listener: MegaRequestListenerInterface?,
    ) {
        megaApi.updateBackup(
            backupId,
            backupType,
            targetNode,
            localFolder,
            backupName,
            state,
            subState,
            listener
        )
    }

    override fun setCoordinates(
        nodeId: NodeId,
        latitude: Double,
        longitude: Double,
        listener: MegaRequestListenerInterface?,
    ) = megaApi.setNodeCoordinates(
        megaApi.getNodeByHandle(nodeId.longValue),
        latitude,
        longitude,
        listener
    )

    override fun shouldShowPasswordReminderDialog(
        atLogout: Boolean,
        listener: MegaRequestListenerInterface,
    ) = megaApi.shouldShowPasswordReminderDialog(atLogout, listener)

    override suspend fun isForeignNode(handle: Long) = megaApi.isForeignNode(handle)

    override fun setBackup(
        backupType: Int,
        targetNode: Long,
        localFolder: String,
        backupName: String,
        state: Int,
        subState: Int,
        listener: MegaRequestListenerInterface,
    ) = megaApi.setBackup(
        backupType,
        targetNode,
        localFolder,
        backupName,
        state,
        subState,
        listener,
    )

    override fun removeBackup(backupId: Long, listener: MegaRequestListenerInterface) {
        megaApi.removeBackup(backupId, listener)
    }

    override fun getBackupInfo(listener: MegaRequestListenerInterface) {
        megaApi.getBackupInfo(listener)
    }

    override suspend fun reconnect() = megaApi.reconnect()

    override fun createCancelToken(): MegaCancelToken = MegaCancelToken.createInstance()

    override fun exportNode(
        node: MegaNode,
        expireTime: Long?,
        listener: MegaRequestListenerInterface,
    ) = megaApi.exportNode(node, expireTime?.toInt() ?: 0, listener)

    override fun getDeviceName(deviceId: String, listener: MegaRequestListenerInterface?) =
        megaApi.getDeviceName(deviceId, listener)

    override fun setDeviceName(
        deviceId: String,
        deviceName: String,
        listener: MegaRequestListenerInterface?,
    ) = megaApi.setDeviceName(deviceId, deviceName, listener)

    override fun getDeviceId() = megaApi.deviceId

    override fun getABTestValue(flag: String): Long = megaApi.getABTestValue(flag)

    override suspend fun getBandwidthOverQuotaDelay() = megaApi.bandwidthOverquotaDelay

    override fun disableExport(node: MegaNode, listener: MegaRequestListenerInterface) =
        megaApi.disableExport(node, listener)

    override fun encryptLinkWithPassword(
        link: String,
        password: String,
        listener: MegaRequestListenerInterface,
    ) = megaApi.encryptLinkWithPassword(link, password, listener)

    override val currentUploadSpeed: Int
        get() = megaApi.currentUploadSpeed

    override suspend fun setNodeCoordinates(node: MegaNode, latitude: Double, longitude: Double) =
        megaApi.setNodeCoordinates(node, latitude, longitude, IgnoredRequestListener)

    override suspend fun createThumbnail(imagePath: String, destinationPath: String) =
        megaApi.createThumbnail(imagePath, destinationPath)

    override suspend fun createPreview(imagePath: String, destinationPath: String) =
        megaApi.createPreview(imagePath, destinationPath)

    override fun pauseTransfers(pause: Boolean, listener: MegaRequestListenerInterface) =
        megaApi.pauseTransfers(pause, listener)

    override fun setThumbnail(
        node: MegaNode,
        srcFilePath: String,
        listener: MegaRequestListenerInterface?,
    ) = megaApi.setThumbnail(node, srcFilePath, listener)

    override fun setPreview(
        node: MegaNode,
        srcFilePath: String,
        listener: MegaRequestListenerInterface?,
    ) = megaApi.setPreview(node, srcFilePath, listener)

    override fun pauseTransferByTag(
        transferTag: Int,
        pause: Boolean,
        listener: MegaRequestListenerInterface,
    ) = megaApi.pauseTransferByTag(transferTag, pause, listener)

    override suspend fun getContactVerificationWarningEnabled(): Boolean =
        megaApi.contactVerificationWarningEnabled()

    override fun createEphemeralAccountPlusPlus(
        firstName: String,
        lastName: String,
        listener: MegaRequestListenerInterface,
    ) = megaApi.createEphemeralAccountPlusPlus(firstName, lastName, listener)

    override suspend fun escapeFsIncompatible(fileName: String, dstPath: String) =
        megaApi.escapeFsIncompatible(fileName, dstPath)

    override val currentDownloadSpeed: Int
        get() = megaApi.currentDownloadSpeed

    override fun getPsa(listener: MegaRequestListenerInterface) =
        megaApi.getPSAWithUrl(listener)

    override suspend fun setPsaHandled(psaId: Int) = megaApi.setPSA(psaId)

    override suspend fun setNodeLabel(node: MegaNode, label: Int) {
        megaApi.setNodeLabel(node, label)
    }

    override suspend fun resetNodeLabel(node: MegaNode) {
        megaApi.resetNodeLabel(node)
    }

    override suspend fun setPublicKeyPinning(enable: Boolean) = megaApi.setPublicKeyPinning(enable)

    override suspend fun changeApiUrl(apiURL: String, disablePkp: Boolean) =
        megaApi.changeApiUrl(apiURL, disablePkp)

    override suspend fun copyMegaPushNotificationsSettings(pushNotificationSettings: MegaPushNotificationSettings): MegaPushNotificationSettings? =
        MegaPushNotificationSettingsAndroid.copy(pushNotificationSettings)

    override fun createInstanceMegaPushNotificationSettings(): MegaPushNotificationSettings =
        MegaPushNotificationSettings.createInstance()

    override fun unSerializeNode(serializedData: String): MegaNode? =
        MegaNode.unserialize(serializedData)

    override fun isGeolocationEnabled(listener: MegaRequestListenerInterface) =
        megaApi.isGeolocationEnabled(listener)

    override fun enableGeolocation(listener: MegaRequestListenerInterface) =
        megaApi.enableGeolocation(listener)

    override fun isCookieBannerEnabled() =
        megaApi.isCookieBannerEnabled

    override fun getUserData(listener: OptionalMegaRequestListenerInterface?) =
        if (listener == null) megaApi.getUserData() else megaApi.getUserData(listener)

    override fun getMiscFlags(listener: OptionalMegaRequestListenerInterface?) =
        if (listener == null) megaApi.getMiscFlags() else megaApi.getMiscFlags(listener)

    override fun getCookieSettings(
        listener: OptionalMegaRequestListenerInterface,
    ) = megaApi.getCookieSettings(listener)

    override fun setCookieSettings(
        bitSetToDecimal: Int,
        listener: OptionalMegaRequestListenerInterface,
    ) = megaApi.setCookieSettings(bitSetToDecimal, listener)

    override fun shouldShowRichLinkWarning(listener: MegaRequestListenerInterface) =
        megaApi.shouldShowRichLinkWarning(listener)

    override fun isRichPreviewsEnabled(listener: MegaRequestListenerInterface) =
        megaApi.isRichPreviewsEnabled(listener)

    override fun setRichLinkWarningCounterValue(
        value: Int,
        listener: MegaRequestListenerInterface,
    ) {
        megaApi.setRichLinkWarningCounterValue(value, listener)
    }

    override fun enableRichPreviews(enable: Boolean, listener: MegaRequestListenerInterface) =
        megaApi.enableRichPreviews(enable, listener)

    override fun getSessionTransferURL(path: String, listener: MegaRequestListenerInterface) {
        megaApi.getSessionTransferURL(path, listener)
    }

    override fun getMyUserHandleBinary(): Long =
        megaApi.myUserHandleBinary

    override fun getNodesByFingerprint(fingerprint: String): List<MegaNode> =
        megaApi.getNodesByFingerprint(fingerprint)

    override fun killSession(sessionHandle: Long, listener: MegaRequestListenerInterface) =
        megaApi.killSession(sessionHandle, listener)

    override fun confirmCancelAccount(
        link: String,
        pwd: String,
        listener: MegaRequestListenerInterface,
    ) = megaApi.confirmCancelAccount(link, pwd, listener)

    override fun confirmChangeEmail(
        link: String,
        pwd: String,
        listener: MegaRequestListenerInterface,
    ) = megaApi.confirmChangeEmail(link, pwd, listener)

    override fun queryCancelLink(link: String, listener: MegaRequestListenerInterface) =
        megaApi.queryCancelLink(link, listener)

    override fun queryChangeEmailLink(link: String, listener: MegaRequestListenerInterface) =
        megaApi.queryChangeEmailLink(link, listener)

    override fun resendSignupLink(
        email: String,
        name: String?,
        listener: MegaRequestListenerInterface,
    ) = megaApi.resendSignupLink(email, name, listener)

    override fun cancelCreateAccount(listener: MegaRequestListenerInterface) =
        megaApi.cancelCreateAccount(listener)

    override fun setNodeDescription(
        node: MegaNode,
        description: String?,
        listener: MegaRequestListenerInterface,
    ) {
        megaApi.setNodeDescription(node, description, listener)
    }

    override fun addNodeTag(node: MegaNode, tag: String, listener: MegaRequestListenerInterface) {
        megaApi.addNodeTag(node, tag, listener)
    }

    override fun removeNodeTag(
        node: MegaNode,
        tag: String,
        listener: MegaRequestListenerInterface,
    ) {
        megaApi.removeNodeTag(node, tag, listener)
    }

    override fun updateNodeTag(
        node: MegaNode,
        newTag: String,
        oldTag: String,
        listener: MegaRequestListenerInterface,
    ) {
        megaApi.updateNodeTag(node, newTag, oldTag, listener)
    }

    override fun getFlag(
        flagName: String,
        commit: Boolean,
    ): MegaFlag? =
        megaApi.getFlag(flagName, commit)

    override fun getAllNodeTags(
        searchString: String,
        cancelToken: MegaCancelToken?,
    ): MegaStringList? = megaApi.getAllNodeTags(searchString, cancelToken)

    override fun creditCardCancelSubscriptions(
        reason: String,
        subscriptionId: String,
        canContact: Int,
        listener: MegaRequestListenerInterface,
    ) {
        megaApi.creditCardCancelSubscriptions(reason, subscriptionId, canContact, listener)
    }

    override fun moveOrRemoveDeconfiguredBackupNodes(
        deconfiguredBackupRoot: NodeId,
        backupDestination: NodeId,
        listener: MegaRequestListenerInterface?,
    ) {
        megaApi.moveOrRemoveDeconfiguredBackupNodes(
            deconfiguredBackupRoot.longValue,
            backupDestination.longValue,
            listener
        )
    }

    override fun enableRequestStatusMonitor() = megaApi.enableRequestStatusMonitor()

    override fun createAccount(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        listener: MegaRequestListenerInterface,
    ) {
        megaApi.createAccount(email, password, firstName, lastName, listener)
    }

    override fun setMyBackupsFolder(
        localizedName: String,
        listener: MegaRequestListenerInterface?,
    ) {
        megaApi.setMyBackupsFolder(localizedName, listener)
    }

    override fun getSyncs(): MegaSyncList = megaApi.syncs

    override fun removeVersions(listener: MegaRequestListenerInterface) {
        megaApi.removeVersions(listener)
    }

    override fun cleanRubbishBin(listener: MegaRequestListenerInterface) {
        megaApi.cleanRubbishBin(listener)
    }

    override fun resendVerificationEmail(listener: MegaRequestListenerInterface) =
        megaApi.resendVerificationEmail(listener)

    override fun resumeCreateAccount(
        session: String,
        listener: MegaRequestListenerInterface,
    ) {
        megaApi.resumeCreateAccount(session, listener)
    }

    override fun getBanners(listener: MegaRequestListenerInterface) = megaApi.getBanners(listener)

    override fun dismissBanner(id: Int, listener: MegaRequestListenerInterface?) {
        if (listener == null) {
            megaApi.dismissBanner(id)
        } else {
            megaApi.dismissBanner(id, listener)
        }
    }

    override fun getRubbishBinAutopurgePeriod(listener: MegaRequestListenerInterface) {
        megaApi.getRubbishBinAutopurgePeriod(listener)
    }

    override fun setRubbishBinAutopurgePeriod(days: Int, listener: MegaRequestListenerInterface) {
        megaApi.setRubbishBinAutopurgePeriod(days, listener)
    }

    override suspend fun serverSideRubbishBinAutopurgeEnabled(): Boolean =
        megaApi.serverSideRubbishBinAutopurgeEnabled()

    override fun checkRecoveryKey(
        link: String,
        recoveryKey: String,
        listener: MegaRequestListenerInterface,
    ) {
        megaApi.checkRecoveryKey(link, recoveryKey, listener)
    }

    override suspend fun resumeTransfersForNotLoggedInInstance() {
        megaApi.resumeTransfersForNotLoggedInInstance()
    }

    override fun addRequestListener(listener: MegaRequestListenerInterface) {
        megaApi.addRequestListener(listener)
    }
}
