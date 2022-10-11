package mega.privacy.android.app.usecase

import android.app.Activity
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.di.MegaApiFolder
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.main.megachat.AndroidMegaChatMessage
import mega.privacy.android.app.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.usecase.exception.*
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaNodeUtil.getRootParentNode
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import java.io.File
import javax.inject.Inject

/**
 * Main use case to retrieve Mega Node information.
 *
 * @property context            Context needed to get offline node files.
 * @property megaApi            Mega API needed to call node information.
 * @property megaApiFolder      Mega API folder needed to authorize node.
 * @property megaChatApi        Mega Chat API needed to get nodes from chat messages.
 * @property databaseHandler    Database Handler needed to retrieve offline nodes.
 */
class GetNodeUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    @MegaApiFolder private val megaApiFolder: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val getChatMessageUseCase: GetChatMessageUseCase,
    private val databaseHandler: DatabaseHandler,
) {

    /**
     * Get a MegaNode given a Node Handle.
     *
     * @param nodeHandle    Mega node handle
     * @return              Single with Mega Node
     */
    fun get(nodeHandle: Long): Single<MegaNode> =
        Single.fromCallable { nodeHandle.getMegaNode() }

    /**
     * Get a MegaNodeItem given a Node Handle.
     *
     * @param nodeHandle    Mega node handle
     * @return              Single with Mega Node Item
     */
    fun getNodeItem(nodeHandle: Long): Single<MegaNodeItem> =
        get(nodeHandle).flatMap(::getNodeItem)

    /**
     * Get a MegaNodeItem given a Node public link.
     *
     * @param nodeFileLink  MegaNode public link
     * @return              Single with Mega Node Item
     */
    fun getNodeItem(nodeFileLink: String): Single<MegaNodeItem> =
        getPublicNode(nodeFileLink).flatMap(::getNodeItem)

    /**
     * Get a MegaNodeItem given a Node Chat Room Id and Chat Message Id.
     *
     * @param chatRoomId    Chat Message Room Id
     * @param chatMessageId Chat Message Id
     * @return              Single with Mega Node Item
     */
    fun getNodeItem(chatRoomId: Long, chatMessageId: Long): Single<MegaNodeItem> =
        getChatMessageUseCase.getChatNode(chatRoomId, chatMessageId).flatMap(::getNodeItem)

    /**
     * Get a MegaNodeItem given a Node.
     *
     * @param node  MegaNode
     * @return      Single with Mega Node Item
     */
    fun getNodeItem(node: MegaNode?): Single<MegaNodeItem> =
        Single.fromCallable {
            requireNotNull(node)

            var hasReadAccess = false
            var hasReadWriteAccess = false
            var hasFullAccess = false
            var hasOwnerAccess = false
            when (megaApi.getAccess(node)) {
                MegaShare.ACCESS_READ -> hasReadAccess = true
                MegaShare.ACCESS_READWRITE -> {
                    hasReadAccess = true
                    hasReadWriteAccess = true
                }
                MegaShare.ACCESS_FULL -> {
                    hasReadAccess = true
                    hasReadWriteAccess = true
                    hasFullAccess = true
                }
                MegaShare.ACCESS_OWNER -> {
                    hasReadAccess = true
                    hasReadWriteAccess = true
                    hasFullAccess = true
                    hasOwnerAccess = true
                }
            }

            val isAvailableOffline =
                isNodeAvailableOffline(node.handle).blockingGetOrNull() ?: false
            val hasVersions = megaApi.hasVersions(node)

            val isMine =
                hasOwnerAccess || (node.owner != INVALID_HANDLE && node.owner == megaApi.myUserHandleBinary)
            val isExternalNode = !isMine && (node.isPublic || node.isForeign)
            val rootParentNode = megaApi.getRootParentNode(node)
            val isFromIncoming = rootParentNode.isInShare
            var isFromRubbishBin = false
            var isFromInbox = false
            var isFromRoot = false
            when (rootParentNode.handle) {
                megaApi.rootNode?.handle -> isFromRoot = true
                megaApi.inboxNode?.handle -> isFromInbox = true
                megaApi.rubbishNode?.handle -> isFromRubbishBin = true
            }

            MegaNodeItem(
                name = node.name,
                handle = node.handle,
                hasReadAccess = hasReadAccess,
                hasReadWriteAccess = hasReadWriteAccess,
                hasFullAccess = hasFullAccess,
                hasOwnerAccess = hasOwnerAccess,
                isFromIncoming = isFromIncoming,
                isFromRubbishBin = isFromRubbishBin,
                isFromInbox = isFromInbox,
                isFromRoot = isFromRoot,
                isExternalNode = isExternalNode,
                hasVersions = hasVersions,
                isAvailableOffline = isAvailableOffline,
                node = node
            )
        }

    /**
     * Get a MegaOffline node given a node handle
     *
     * @param nodeHandle    Node handle to be retrieved
     * @return              Single with the MegaOffline
     */
    fun getOfflineNode(nodeHandle: Long): Single<MegaOffline> =
        Single.fromCallable {
            val offlineNode = databaseHandler.offlineFiles.find { megaOffline ->
                nodeHandle == megaOffline.handle.toLongOrNull()
                        || nodeHandle == megaOffline.handleIncoming.toLongOrNull()
            }
            offlineNode ?: error("Offline node was not found")
        }

    /**
     * Get an offline MegaNodeItem given a node handle
     *
     * @param nodeHandle    Node handle to be retrieved
     * @return              Single with the MegaNodeItem
     */
    fun getOfflineNodeItem(nodeHandle: Long): Single<MegaNodeItem> =
        Single.fromCallable {
            val offlineNode = getOfflineNode(nodeHandle).blockingGetOrNull()
            if (offlineNode != null) {
                val file = OfflineUtils.getOfflineFile(context, offlineNode)
                if (file.exists()) {
                    MegaNodeItem(
                        name = offlineNode.name,
                        handle = offlineNode.handle.toLong(),
                        hasReadAccess = true,
                        hasReadWriteAccess = false,
                        hasFullAccess = false,
                        hasOwnerAccess = false,
                        isFromIncoming = offlineNode.origin == MegaOffline.INCOMING,
                        isFromRubbishBin = false,
                        isFromInbox = offlineNode.origin == MegaOffline.INBOX,
                        isFromRoot = false,
                        hasVersions = false,
                        isExternalNode = false,
                        isAvailableOffline = true,
                        node = null
                    )
                } else {
                    error("Offline file doesn't exist")
                }
            } else {
                error("Offline node was not found")
            }
        }

    /**
     * Get a MegaNode given a Node public link.
     *
     * @param nodeFileLink      Node public link
     * @return                  Single with Mega Node
     */
    fun getPublicNode(nodeFileLink: String): Single<MegaNode> =
        Single.create { emitter ->
            if (nodeFileLink.isBlank()) {
                emitter.onError(IllegalArgumentException("Invalid megaFileLink"))
                return@create
            }

            megaApi.getPublicNode(nodeFileLink, OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == MegaError.API_OK) {
                        if (!request.flag) {
                            emitter.onSuccess(request.publicMegaNode)
                        } else {
                            emitter.onError(IllegalArgumentException("Invalid key for public node"))
                        }
                    } else {
                        emitter.onError(error.toMegaException())
                    }
                }
            ))
        }

    /**
     * Mark node as favorite
     *
     * @param nodeHandle    Node handle to mark as favorite
     * @param isFavorite    Flag to mark/unmark as favorite
     * @return              Completable
     */
    fun markAsFavorite(nodeHandle: Long, isFavorite: Boolean): Completable =
        get(nodeHandle).flatMapCompletable { markAsFavorite(it, isFavorite) }

    /**
     * Mark node as favorite
     *
     * @param node          Node to mark as favorite
     * @param isFavorite    Flag to mark/unmark as favorite
     * @return              Completable
     */
    fun markAsFavorite(node: MegaNode?, isFavorite: Boolean): Completable =
        Completable.create { emitter ->
            if (node == null) {
                emitter.onError(IllegalArgumentException("Null node"))
                return@create
            }

            megaApi.setNodeFavourite(node, isFavorite, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    when {
                        emitter.isDisposed -> return@OptionalMegaRequestListenerInterface
                        error.errorCode == MegaError.API_OK -> emitter.onComplete()
                        else -> emitter.onError(error.toMegaException())
                    }
                }
            ))
        }

    /**
     * Check if a node is available offline
     *
     * @param nodeHandle    Mega Node handle to check
     * @return              Single with true if it's available, false otherwise
     */
    fun isNodeAvailableOffline(nodeHandle: Long): Single<Boolean> =
        Single.fromCallable {
            databaseHandler.findByHandle(nodeHandle)?.let { offlineNode ->
                val offlineFile = OfflineUtils.getOfflineFile(context, offlineNode)
                val isFileAvailable = FileUtil.isFileAvailable(offlineFile)
                val isFileDownloadedLatest = nodeHandle.getMegaNode()?.let { node ->
                    FileUtil.isFileDownloadedLatest(offlineFile,
                        node) && offlineFile.length() == node.size
                } ?: false
                return@fromCallable isFileAvailable && isFileDownloadedLatest
            }

            return@fromCallable false
        }

    /**
     * Set node as available offline given its Node Handle.
     *
     * @param nodeHandle            Node handle to set available offline
     * @param setOffline            Flag to set/unset available offline
     * @param activity              Activity context needed to create file
     * @param isFromIncomingShares  Flag indicating if node is from incoming shares.
     * @param isFromInbox           Flag indicating if node is from inbox.
     * @return                      Completable
     */
    fun setNodeAvailableOffline(
        nodeHandle: Long,
        setOffline: Boolean,
        isFromIncomingShares: Boolean = false,
        isFromInbox: Boolean = false,
        activity: Activity,
    ): Completable =
        get(nodeHandle).flatMapCompletable {
            setNodeAvailableOffline(
                it,
                setOffline,
                isFromIncomingShares,
                isFromInbox,
                activity
            )
        }

    /**
     * Set node as available offline
     *
     * @param node                  Node to set available offline
     * @param setOffline            Flag to set/unset available offline
     * @param activity              Activity context needed to create file
     * @param isFromIncomingShares  Flag indicating if node is from incoming shares.
     * @param isFromInbox           Flag indicating if node is from inbox.
     * @return                      Completable
     */
    fun setNodeAvailableOffline(
        node: MegaNode?,
        setOffline: Boolean,
        isFromIncomingShares: Boolean = false,
        isFromInbox: Boolean = false,
        activity: Activity,
    ): Completable =
        Completable.fromCallable {
            requireNotNull(node)
            val isAvailableOffline =
                isNodeAvailableOffline(node.handle).blockingGetOrNull() ?: false
            when {
                setOffline && !isAvailableOffline -> {
                    val from = when {
                        isFromIncomingShares -> Constants.FROM_INCOMING_SHARES
                        isFromInbox -> Constants.FROM_INBOX
                        else -> Constants.FROM_OTHERS
                    }

                    val offlineParent =
                        OfflineUtils.getOfflineParentFile(activity, from, node, megaApi)
                    if (FileUtil.isFileAvailable(offlineParent)) {
                        val offlineFile = File(offlineParent, node.name)
                        if (FileUtil.isFileAvailable(offlineFile)) {
                            val offlineNode = databaseHandler.findByHandle(node.handle)
                            OfflineUtils.removeOffline(offlineNode, databaseHandler, activity)
                        }
                    }

                    OfflineUtils.saveOffline(offlineParent, node, activity)
                }
                !setOffline && isAvailableOffline -> {
                    removeOfflineNode(node.handle, activity).blockingAwait()
                }
            }
        }

    /**
     * Remove offline node
     *
     * @param nodeHandle    Node handle to be removed
     * @param activity      Activity context needed to remove file
     * @return              Completable
     */
    fun removeOfflineNode(nodeHandle: Long, activity: Activity): Completable =
        Completable.fromCallable {
            val offlineNode = databaseHandler.findByHandle(nodeHandle)
            OfflineUtils.removeOffline(offlineNode, databaseHandler, activity)
        }

    /**
     * Get a MegaNode given a Long handle in a synchronous way.
     * This will also authorize the Node if required.
     *
     * @return  MegaNode
     */
    private fun Long.getMegaNode(): MegaNode? =
        megaApi.getNodeByHandle(this)
            ?: megaApiFolder.authorizeNode(megaApiFolder.getNodeByHandle(this))

    /**
     * Check if a node is available
     *
     * @param nodeHandle  node handle
     * @return  True, if it is available. False, otherwise.
     */
    fun checkNodeAvailable(nodeHandle: Long): Single<Boolean> =
        Single.fromCallable {
            get(nodeHandle).blockingGetOrNull() != null
        }


    /**
     * Check if several MegaNodes are available
     *
     * @param androidMegaChatMessages List of mega chat messages
     * @return  True, if all nodes are available. False, otherwise.
     */
    fun checkNodesAvailable(androidMegaChatMessages: List<AndroidMegaChatMessage>): Single<Boolean> =
        Single.fromCallable {
            androidMegaChatMessages.forEach { chatMessage ->
                chatMessage.message?.let { message ->
                    if (message.type == MegaChatMessage.TYPE_NODE_ATTACHMENT
                        && message.userHandle == megaChatApi.myUserHandle
                        && (message.megaNodeList?.size() ?: 0) > 0
                    ) {
                        message.megaNodeList.get(0)?.let { node ->
                            checkNodeAvailable(node.handle).blockingGet().let { isAvailable ->
                                if (!isAvailable) {
                                    return@fromCallable false
                                }
                            }
                        }
                    }
                }
            }

            return@fromCallable true
        }
}
