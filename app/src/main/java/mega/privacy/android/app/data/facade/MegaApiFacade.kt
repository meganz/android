package mega.privacy.android.app.data.facade

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.di.ApplicationScope
import mega.privacy.android.app.di.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaEvent
import nz.mega.sdk.MegaGlobalListenerInterface
import nz.mega.sdk.MegaLoggerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferListenerInterface
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
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

    override suspend fun getRootNode(): MegaNode? = megaApi.rootNode

    override suspend fun getRubbishBinNode(): MegaNode? = megaApi.rubbishNode

    override suspend fun getSdkVersion(): String = megaApi.version

    override val globalUpdates: Flow<GlobalUpdate>
        get() = callbackFlow {
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

    override fun getFavourites(
        node: MegaNode?,
        count: Int,
        listener: MegaRequestListenerInterface?,
    ) {
        megaApi.getFavourites(node, count, listener)
    }

    override suspend fun getMegaNodeByHandle(nodeHandle: Long): MegaNode? =
        megaApi.getNodeByHandle(nodeHandle)

    override fun hasVersion(node: MegaNode): Boolean = megaApi.hasVersions(node)

    override suspend fun getChildrenByNode(parentNode: MegaNode, order: Int?): ArrayList<MegaNode> =
        if (order == null)
            megaApi.getChildren(parentNode)
        else
            megaApi.getChildren(parentNode, order)

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
        listener: MegaRequestListenerInterface,
    ) = megaApi.getThumbnail(node, thumbnailFilePath, listener)

    override fun handleToBase64(handle: Long): String = MegaApiAndroid.handleToBase64(handle)

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

    override fun startDownload(
        node: MegaNode,
        localPath: String,
        fileName: String?,
        appData: String?,
        startFirst: Boolean,
        cancelToken: MegaCancelToken?,
        listener: MegaTransferListenerInterface?,
    ) = megaApi.startDownload(node, localPath, fileName, appData, startFirst, cancelToken, listener)

    companion object {
        private const val ANDROID_SUPPORT_ISSUE = 10
    }
}