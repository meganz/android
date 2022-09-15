package mega.privacy.android.app.data.facade

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.model.GlobalTransfer
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.di.ApplicationScope
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaTransferListenerInterface
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
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferListenerInterface
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
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
class MegaApiFacade @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationScope private val sharingScope: CoroutineScope,
) : MegaApiGateway {
    override fun multiFactorAuthAvailable(): Boolean {
        return megaApi.multiFactorAuthAvailable()
    }

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

    override fun startUploadForSupport(
        path: String,
        listener: MegaTransferListenerInterface,
    ) {
        megaApi.startUploadForSupport(path, false, listener)
    }

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
                users: java.util.ArrayList<MegaUser>?,
            ) {
                trySend(GlobalUpdate.OnUsersUpdate(users))
            }

            override fun onUserAlertsUpdate(
                api: MegaApiJava?,
                userAlerts: java.util.ArrayList<MegaUserAlert>?,
            ) {
                trySend(GlobalUpdate.OnUserAlertsUpdate(userAlerts))
            }

            override fun onNodesUpdate(
                api: MegaApiJava?,
                nodeList: java.util.ArrayList<MegaNode>?,
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
                requests: java.util.ArrayList<MegaContactRequest>?,
            ) {
                trySend(GlobalUpdate.OnContactRequestsUpdate(requests))
            }

            override fun onEvent(api: MegaApiJava?, event: MegaEvent?) {
                trySend(GlobalUpdate.OnEvent(event))
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

    override fun hasVersion(node: MegaNode): Boolean = megaApi.hasVersions(node)

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

    override suspend fun getPublicLinks(order: Int?): List<MegaNode> =
        if (order == null)
            megaApi.publicLinks
        else
            megaApi.getPublicLinks(order)


    override fun getNumChildFolders(node: MegaNode): Int = megaApi.getNumChildFolders(node)

    override fun getNumChildFiles(node: MegaNode): Int = megaApi.getNumChildFiles(node)

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

    override fun cancelTransfer(transfer: MegaTransfer) {
        megaApi.cancelTransfer(transfer)
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

    override suspend fun getUserAvatar(user: MegaUser, dstPath: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, e ->
                    continuation.resume(e.errorCode == MegaError.API_OK)
                },
                onRequestTemporaryError = { _, e -> continuation.resume(e.errorCode == MegaError.API_OK) })

            continuation.invokeOnCancellation { megaApi.removeRequestListener(listener) }
            megaApi.getUserAvatar(user,
                dstPath,
                listener
            )
        }
    }

    override suspend fun acknowledgeUserAlerts() {
        megaApi.acknowledgeUserAlerts()
    }

    override suspend fun getIncomingContactRequests() =
        megaApi.incomingContactRequests

    companion object {
        private const val ANDROID_SUPPORT_ISSUE = 10
    }

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

    override suspend fun isInRubbish(node: MegaNode): Boolean = megaApi.isInRubbish(node)

    override suspend fun getChildren(parentNodes: MegaNodeList, order: Int): List<MegaNode> =
        megaApi.getChildren(parentNodes, order)

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
}