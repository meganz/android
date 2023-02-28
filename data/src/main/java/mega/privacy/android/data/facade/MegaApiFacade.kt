package mega.privacy.android.data.facade

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine
import mega.privacy.android.data.extensions.APP_DATA_BACKGROUND_TRANSFER
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaEvent
import nz.mega.sdk.MegaGlobalListenerInterface
import nz.mega.sdk.MegaLoggerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaSetElementList
import nz.mega.sdk.MegaSetList
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferListenerInterface
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

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

    override fun getInvalidHandle(): Long = MegaApiAndroid.INVALID_HANDLE

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

    override fun startUpload(
        localPath: String,
        parentNode: MegaNode,
        fileName: String?,
        modificationTime: Long,
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
            modificationTime,
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
    override val isMasterBusinessAccount: Boolean
        get() = megaApi.isMasterBusinessAccount
    override val isEphemeralPlusPlus: Boolean
        get() = megaApi.isEphemeralPlusPlus
    override val accountAuth: String
        get() = megaApi.accountAuth
    override val myCredentials: String?
        get() = megaApi.myCredentials
    override val dumpSession: String?
        get() = megaApi.dumpSession()

    override suspend fun areTransfersPaused(): Boolean =
        megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD) ||
                megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)

    override suspend fun areUploadTransfersPaused(): Boolean =
        megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)

    override suspend fun getRootNode(): MegaNode? = megaApi.rootNode

    override suspend fun getRubbishBinNode(): MegaNode? = megaApi.rubbishNode

    override suspend fun getSdkVersion(): String = megaApi.version

    override val globalUpdates: Flow<GlobalUpdate> = callbackFlow {
        val listener = object : MegaGlobalListenerInterface {
            override fun onUsersUpdate(
                api: MegaApiJava?,
                users: ArrayList<MegaUser>?,
            ) {
                trySend(GlobalUpdate.OnUsersUpdate(users))
            }

            override fun onUserAlertsUpdate(
                api: MegaApiJava?,
                userAlerts: ArrayList<MegaUserAlert>?,
            ) {
                trySend(GlobalUpdate.OnUserAlertsUpdate(userAlerts))
            }

            override fun onNodesUpdate(
                api: MegaApiJava?,
                nodeList: ArrayList<MegaNode>?,
            ) {
                trySend(GlobalUpdate.OnNodesUpdate(nodeList))
            }

            override fun onReloadNeeded(api: MegaApiJava?) {
                trySend(GlobalUpdate.OnReloadNeeded)
            }

            override fun onAccountUpdate(api: MegaApiJava?) {
                trySend(GlobalUpdate.OnAccountUpdate)
            }

            override fun onContactRequestsUpdate(
                api: MegaApiJava?,
                requests: ArrayList<MegaContactRequest>?,
            ) {
                trySend(GlobalUpdate.OnContactRequestsUpdate(requests))
            }

            override fun onEvent(api: MegaApiJava?, event: MegaEvent?) {
                trySend(GlobalUpdate.OnEvent(event))
            }

            override fun onSetsUpdate(api: MegaApiJava?, sets: java.util.ArrayList<MegaSet>?) {
                trySend(GlobalUpdate.OnSetsUpdate(sets))
            }

            override fun onSetElementsUpdate(
                api: MegaApiJava?,
                elements: java.util.ArrayList<MegaSetElement>?,
            ) {
                trySend(GlobalUpdate.OnSetElementsUpdate(elements))
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
            }
        )

        megaApi.addTransferListener(listener)

        awaitClose {
            megaApi.removeTransferListener(listener)
        }
    }.shareIn(sharingScope, SharingStarted.WhileSubscribed(), 1)

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

    override suspend fun getChildrenByNode(parentNode: MegaNode, order: Int?): List<MegaNode> =
        if (order == null)
            megaApi.getChildren(parentNode)
        else
            megaApi.getChildren(parentNode, order)

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

    override suspend fun isPendingShare(node: MegaNode): Boolean = megaApi.isPendingShare(node)

    override suspend fun getPublicLinks(order: Int?): List<MegaNode> =
        if (order == null)
            megaApi.publicLinks
        else
            megaApi.getPublicLinks(order)


    override suspend fun getNumChildFolders(node: MegaNode): Int = megaApi.getNumChildFolders(node)

    override suspend fun getNumChildFiles(node: MegaNode): Int = megaApi.getNumChildFiles(node)

    override fun setAutoAcceptContactsFromLink(
        disableAutoAccept: Boolean,
        listener: MegaRequestListenerInterface,
    ) = megaApi.setContactLinksOption(disableAutoAccept, listener)

    override fun isAutoAcceptContactsFromLinkEnabled(listener: MegaRequestListenerInterface) =
        megaApi.getContactLinksOption(listener)

    override fun getFolderInfo(node: MegaNode?, listener: MegaRequestListenerInterface) =
        megaApi.getFolderInfo(node, listener)

    override fun setNodeFavourite(node: MegaNode?, favourite: Boolean) {
        megaApi.setNodeFavourite(node, favourite)
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

    override fun cancelAllUploadTransfers(listener: MegaRequestListenerInterface?) {
        if (listener != null) {
            megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, listener)
        } else {
            megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD)
        }
    }

    override suspend fun getNumUnreadUserAlerts(): Int = megaApi.numUnreadUserAlerts

    override suspend fun getInboxNode(): MegaNode? = megaApi.inboxNode

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

    override suspend fun getTransfers(type: Int): List<MegaTransfer> = megaApi.getTransfers(type)

    override suspend fun getTransfersByTag(tag: Int): MegaTransfer? = megaApi.getTransferByTag(tag)

    override fun startDownload(
        node: MegaNode,
        localPath: String,
        fileName: String?,
        appData: String?,
        startFirst: Boolean,
        cancelToken: MegaCancelToken?,
        listener: MegaTransferListenerInterface?,
    ) = megaApi.startDownload(node, localPath, fileName, appData, startFirst, cancelToken, listener)

    override fun getUserEmail(userHandle: Long, callback: MegaRequestListenerInterface) =
        megaApi.getUserEmail(userHandle, callback)

    override suspend fun getContact(email: String): MegaUser? = megaApi.getContact(email)

    override suspend fun getUserAlerts(): List<MegaUserAlert> = megaApi.userAlerts

    @Suppress("DEPRECATION")
    override suspend fun sendEvent(eventID: Int, message: String) =
        megaApi.sendEvent(eventID, message)

    override suspend fun getUserAvatarColor(megaUser: MegaUser): String =
        megaApi.getUserAvatarColor(megaUser)

    override suspend fun getUserAvatarColor(userHandle: Long): String =
        megaApi.getUserAvatarColor(userHandleToBase64(userHandle))

    override suspend fun getUserAvatar(user: MegaUser, destinationPath: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, e ->
                    continuation.resume(e.errorCode == MegaError.API_OK)
                })

            continuation.invokeOnCancellation { megaApi.removeRequestListener(listener) }
            megaApi.getUserAvatar(
                user,
                destinationPath,
                listener
            )
        }
    }

    override suspend fun acknowledgeUserAlerts() {
        megaApi.acknowledgeUserAlerts()
    }

    override suspend fun getIncomingContactRequests(): ArrayList<MegaContactRequest> =
        megaApi.incomingContactRequests

    override suspend fun searchByType(
        cancelToken: MegaCancelToken,
        order: Int,
        type: Int,
        target: Int,
    ): List<MegaNode> = megaApi.searchByType(cancelToken, order, type, target)

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
            APP_DATA_BACKGROUND_TRANSFER,
            highPriority,
            null,
            listener
        )
    }

    override suspend fun isInRubbish(node: MegaNode): Boolean = megaApi.isInRubbish(node)

    override suspend fun isInInbox(node: MegaNode): Boolean = megaApi.isInInbox(node)

    override suspend fun getChildren(parentNodes: MegaNodeList, order: Int): List<MegaNode> =
        megaApi.getChildren(parentNodes, order)

    override suspend fun getChildren(parent: MegaNode, order: Int): List<MegaNode> =
        megaApi.getChildren(parent, order)

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

    override suspend fun isBusinessAccountActive(): Boolean = megaApi.isBusinessAccountActive

    companion object {
        private const val ANDROID_SUPPORT_ISSUE = 10
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
        listener: MegaRequestListenerInterface,
    ) = megaApi.getRecentActionsAsync(days, maxNodes, listener)

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

    override fun copyBucket(bucket: MegaRecentActionBucket): MegaRecentActionBucket =
        megaApi.copyBucket(bucket)

    override fun checkAccessErrorExtended(node: MegaNode, level: Int): MegaError =
        megaApi.checkAccessErrorExtended(node, level)

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

    override suspend fun getRubbishNode(): MegaNode = megaApi.rubbishNode

    override fun createSet(name: String, listener: MegaRequestListenerInterface) =
        megaApi.createSet(name, listener)

    override fun createSetElement(sid: Long, node: Long, listener: MegaRequestListenerInterface) =
        megaApi.createSetElement(sid, node, "", listener)

    override suspend fun removeSetElement(sid: Long, eid: Long, listener: MegaRequestListenerInterface) =
        megaApi.removeSetElement(sid, eid, listener)

    override suspend fun getSets(): MegaSetList = megaApi.sets

    override suspend fun getSet(sid: Long): MegaSet? = megaApi.getSet(sid)

    override suspend fun getSetElements(sid: Long): MegaSetElementList =
        megaApi.getSetElements(sid, false)

    override fun removeSet(sid: Long, listener: MegaRequestListenerInterface) =
        megaApi.removeSet(sid, listener)

    override fun updateSetName(sid: Long, name: String?, listener: MegaRequestListenerInterface?) =
        megaApi.updateSetName(sid, name, listener)

    override fun updateSetName(sid: Long, name: String?) =
        megaApi.updateSetName(sid, name)

    override suspend fun putSetCover(sid: Long, eid: Long) = megaApi.putSetCover(sid, eid)

    override fun removeRequestListener(listener: MegaRequestListenerInterface) =
        megaApi.removeRequestListener(listener)

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

    override fun outgoingContactRequests(): ArrayList<MegaContactRequest> =
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

    override fun submitPurchaseReceipt(
        gateway: Int,
        receipt: String?,
        lastPublicHandle: Long,
        lastPublicHandleType: Int,
        lastAccessTimestamp: Long,
        listener: MegaRequestListenerInterface,
    ) = megaApi.submitPurchaseReceipt(
        gateway,
        receipt,
        lastPublicHandle,
        lastPublicHandleType,
        lastAccessTimestamp,
        listener,
    )

    override fun setMyChatFilesFolder(nodeHandle: Long, listener: MegaRequestListenerInterface) =
        megaApi.setMyChatFilesFolder(
            nodeHandle,
            listener,
        )

    override fun getFileVersionsOption(listener: MegaRequestListenerInterface) {
        megaApi.getFileVersionsOption(listener)
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        "Function related to statistics will be reviewed in future updates to\n" +
                "     * provide more data and avoid race conditions. They could change or be removed in the current form."
    )
    override val numberOfPendingUploads: Int
        get() = megaApi.numPendingUploads

    override fun setFileVersionsOption(disable: Boolean, listener: MegaRequestListenerInterface) {
        megaApi.setFileVersionsOption(disable, listener)
    }

    override fun isUserLoggedIn(): Int = megaApi.isLoggedIn
    override fun cancelTransferByTag(transferTag: Int, listener: MegaRequestListenerInterface?) {
        megaApi.cancelTransferByTag(transferTag, listener)
    }

    override fun getContactLink(handle: Long, listener: MegaRequestListenerInterface) {
        megaApi.contactLinkQuery(handle, listener)
    }

    override fun checkValidNodeFile(node: MegaNode, nodeFile: File?) =
        nodeFile?.canRead() == true && nodeFile.length() == node.size
                && node.fingerprint == megaApi.getFingerprint(nodeFile.absolutePath)

    override fun changeEmail(email: String, listener: MegaRequestListenerInterface) =
        megaApi.changeEmail(email, listener)

    @Suppress("DEPRECATION")
    @Deprecated(
        "Function related to statistics will be reviewed in future updates to\n" +
                " * provide more data and avoid race conditions. They could change or be removed in the current form."
    )
    override fun resetTotalUploads() {
        megaApi.resetTotalUploads()
    }

    override suspend fun getExportMasterKey(): String? = megaApi.exportMasterKey()

    override fun setMasterKeyExported(listener: MegaRequestListenerInterface?) {
        megaApi.masterKeyExported(listener)
    }

    override fun setUserAttribute(
        type: Int,
        value: String,
        listener: MegaRequestListenerInterface,
    ) = megaApi.setUserAttribute(type, value, listener)

    @Suppress("DEPRECATION")
    @Deprecated(
        "Function related to statistics will be reviewed in future updates to\n" +
                " * provide more data and avoid race conditions. They could change or be removed in the current form."
    )
    override suspend fun resetTotalDownloads() {
        megaApi.resetTotalDownloads()
    }

    override fun querySignupLink(link: String, listener: MegaRequestListenerInterface) =
        megaApi.querySignupLink(link, listener)

    override fun getPublicNode(
        nodeFileLink: String,
        listener: MegaRequestListenerInterface,
    ) = megaApi.getPublicNode(nodeFileLink, listener)

    override suspend fun cancelTransfers(direction: Int) = megaApi.cancelTransfers(direction)

    override suspend fun getVerifiedPhoneNumber(): String? = megaApi.smsVerifiedPhoneNumber()

    override fun verifyPhoneNumber(pin: String, listener: MegaRequestListenerInterface) =
        megaApi.checkSMSVerificationCode(pin, listener)

    override fun localLogout(listener: MegaRequestListenerInterface) = megaApi.localLogout(listener)
    override suspend fun searchOnInShares(
        query: String,
        megaCancelToken: MegaCancelToken,
        order: Int,
    ): List<MegaNode> {
        return megaApi.searchOnInShares(
            query,
            megaCancelToken,
            order
        )
    }

    override suspend fun searchOnOutShares(
        query: String,
        megaCancelToken: MegaCancelToken,
        order: Int,
    ): List<MegaNode> {
        return megaApi.searchOnOutShares(
            query,
            megaCancelToken,
            order
        )
    }

    override suspend fun searchOnLinkShares(
        query: String,
        megaCancelToken: MegaCancelToken,
        order: Int,
    ): List<MegaNode> {
        return megaApi.searchOnPublicLinks(
            query,
            megaCancelToken,
            order
        )
    }

    override suspend fun search(
        parent: MegaNode,
        query: String,
        megaCancelToken: MegaCancelToken,
        order: Int,
    ): List<MegaNode> {
        return megaApi.search(
            parent,
            query,
            megaCancelToken,
            true,
            order
        )
    }

    override suspend fun getUnverifiedIncomingShares(order: Int): List<MegaShare> =
        megaApi.getUnverifiedIncomingShares(order)

    override suspend fun getUnverifiedOutgoingShares(order: Int): List<MegaShare> =
        megaApi.getUnverifiedOutgoingShares(order)

    override fun openShareDialog(
        megaNode: MegaNode,
        listener: MegaRequestListenerInterface,
    ) = megaApi.openShareDialog(megaNode, listener)

    override fun upgradeSecurity(listener: MegaRequestListenerInterface) =
        megaApi.upgradeSecurity(listener)

    @Deprecated("This API is for testing purpose, will be deleted later")
    override fun setSecureFlag(enable: Boolean) = megaApi.setSecureFlag(enable)

    override suspend fun getSmsAllowedState() = megaApi.smsAllowedState()

}
