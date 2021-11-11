package mega.privacy.android.app.usecase

import android.app.Activity
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.di.MegaApiFolder
import mega.privacy.android.app.errors.BusinessAccountOverdueMegaError
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.MegaNodeUtil.getLastAvailableTime
import mega.privacy.android.app.utils.MegaNodeUtil.getRootParentNode
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import java.io.File
import javax.inject.Inject

class GetNodeUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    @MegaApiFolder private val megaApiFolder: MegaApiAndroid,
    private val databaseHandler: DatabaseHandler
) {

    fun get(nodeHandle: Long): Single<MegaNode> =
        Single.fromCallable { nodeHandle.getMegaNode() }

    fun getNodeItem(nodeHandle: Long): Single<MegaNodeItem> =
        get(nodeHandle).flatMap(::getNodeItem)

    fun getNodeItem(node: MegaNode?): Single<MegaNodeItem> =
        Single.fromCallable {
            requireNotNull(node)

            val nodeSizeText = Util.getSizeString(node.size)
            val nodeDateText = TimeUtils.formatLongDateTime(node.getLastAvailableTime())
            val infoText = TextUtil.getFileInfo(nodeSizeText, nodeDateText)

            val nodeAccess = megaApi.getAccess(node)
            val hasFullAccess = nodeAccess == MegaShare.ACCESS_OWNER || nodeAccess == MegaShare.ACCESS_FULL

            val isAvailableOffline = isNodeAvailableOffline(node.handle).blockingGetOrNull() ?: false
            val hasVersions = megaApi.hasVersions(node)

            var isFromRubbishBin = false
            var isFromInbox = false
            var isFromRoot = false
            when (megaApi.getRootParentNode(node).handle) {
                megaApi.rootNode.handle -> isFromRoot = true
                megaApi.inboxNode.handle -> isFromInbox = true
                megaApi.rubbishNode.handle -> isFromRubbishBin = true
            }

            MegaNodeItem(
                node,
                infoText,
                hasFullAccess,
                isFromRubbishBin,
                isFromInbox,
                isFromRoot,
                isAvailableOffline,
                hasVersions
            )
        }

    fun getPublicNode(nodeFileLink: String): Single<MegaNode> =
        Single.create { emitter ->
            if (nodeFileLink.isBlank()) {
                emitter.onError(IllegalArgumentException("Invalid megaFileLink"))
                return@create
            }

            megaApi.getPublicNode(nodeFileLink, OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        if (!request.flag) {
                            emitter.onSuccess(request.publicNode)
                        } else {
                            emitter.onError(IllegalArgumentException("Invalid key for public node"))
                        }
                    } else {
                        emitter.onError(error.toThrowable())
                    }
                }
            ))
        }

    fun markAsFavorite(nodeHandle: Long, isFavorite: Boolean): Completable =
        get(nodeHandle).flatMapCompletable { markAsFavorite(it, isFavorite) }

    fun markAsFavorite(node: MegaNode?, isFavorite: Boolean): Completable =
        Completable.fromCallable {
            requireNotNull(node)
            megaApi.setNodeFavourite(node, isFavorite)
        }

    fun isNodeAvailableOffline(nodeHandle: Long): Single<Boolean> =
        Single.fromCallable {
            if (databaseHandler.exists(nodeHandle)) {
                databaseHandler.findByHandle(nodeHandle)?.let { offlineNode ->
                    val offlineFile = OfflineUtils.getOfflineFile(context, offlineNode)
                    val isFileAvailable = FileUtil.isFileAvailable(offlineFile)
                    val isFileDownloadedLatest = nodeHandle.getMegaNode()
                        ?.let { FileUtil.isFileDownloadedLatest(offlineFile, it) } ?: false
                    return@fromCallable isFileAvailable && isFileDownloadedLatest
                }
            }

            return@fromCallable false
        }

    fun setNodeAvailableOffline(
        nodeHandle: Long,
        availableOffline: Boolean,
        activity: Activity
    ): Completable =
        get(nodeHandle).flatMapCompletable { setNodeAvailableOffline(it, availableOffline, activity) }

    fun setNodeAvailableOffline(
        node: MegaNode?,
        availableOffline: Boolean,
        activity: Activity
    ): Completable =
        Completable.fromCallable {
            requireNotNull(node)
            val isCurrentlyAvailable = isNodeAvailableOffline(node.handle).blockingGet()

            if (availableOffline) {
                if (!isCurrentlyAvailable) {
                    val offlineParent = OfflineUtils.getOfflineParentFile(activity, Constants.FROM_OTHERS, node, megaApi)
                    if (FileUtil.isFileAvailable(offlineParent)) {
                        val parentName = OfflineUtils.getOfflineParentFileName(activity, node).absolutePath + File.separator
                        val offlineNode = databaseHandler.findbyPathAndName(parentName, node.name)
                        OfflineUtils.removeOffline(offlineNode, databaseHandler, activity)
                    }
                    OfflineUtils.saveOffline(offlineParent, node, activity)
                }
            } else if (isCurrentlyAvailable) {
                val offlineNode = databaseHandler.findByHandle(node.handle)
                OfflineUtils.removeOffline(offlineNode, databaseHandler, activity)
            }
        }


    fun copyNode(
        nodeHandle: Long? = null,
        toParentHandle: Long? = null,
        node: MegaNode? = null,
        toParentNode: MegaNode? = null
    ): Completable =
        Completable.fromCallable {
            require((node != null || nodeHandle != null) && (toParentNode != null || toParentHandle != null))
            copyNode(
                node ?: nodeHandle?.getMegaNode(),
                toParentNode ?: toParentHandle?.getMegaNode()
            ).blockingAwait()
        }

    fun copyNode(currentNode: MegaNode?, toParentNode: MegaNode?): Completable =
        Completable.create { emitter ->
            if (currentNode == null || toParentNode == null) {
                emitter.onError(IllegalArgumentException("Null nodes"))
                return@create
            }

            megaApi.copyNode(currentNode, toParentNode, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    when (error.errorCode) {
                        MegaError.API_OK ->
                            emitter.onComplete()
                        MegaError.API_EBUSINESSPASTDUE ->
                            emitter.onError(BusinessAccountOverdueMegaError())
                        else ->
                            emitter.onError(error.toThrowable())
                    }
                }
            ))
        }

    fun moveNode(nodeHandle: Long, toParentHandle: Long): Completable =
        Completable.fromCallable {
            moveNode(nodeHandle.getMegaNode(), toParentHandle.getMegaNode()).blockingAwait()
        }

    fun moveNode(currentNode: MegaNode?, toParentNode: MegaNode?): Completable =
        Completable.create { emitter ->
            if (currentNode == null || toParentNode == null) {
                emitter.onError(IllegalArgumentException("Null node"))
                return@create
            }

            megaApi.moveNode(currentNode, toParentNode, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    when (error.errorCode) {
                        MegaError.API_OK ->
                            emitter.onComplete()
                        MegaError.API_EBUSINESSPASTDUE ->
                            emitter.onError(BusinessAccountOverdueMegaError())
                        else ->
                            emitter.onError(error.toThrowable())
                    }
                }
            ))
        }

    fun moveToRubbishBin(nodeHandle: Long): Completable =
        Completable.fromCallable {
            moveNode(nodeHandle, megaApi.rubbishNode.handle).blockingAwait()
        }

    fun removeNode(nodeHandle: Long): Completable =
        Completable.fromCallable {
            removeNode(nodeHandle.getMegaNode()).blockingAwait()
        }

    fun removeNode(node: MegaNode?): Completable =
        Completable.create { emitter ->
            if (node == null) {
                emitter.onError(IllegalArgumentException("Null node"))
                return@create
            }

            megaApi.remove(node, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    when (error.errorCode) {
                        MegaError.API_OK ->
                            emitter.onComplete()
                        MegaError.API_EMASTERONLY ->
                            emitter.onError(IllegalStateException("Sub-user business account"))
                        else ->
                            emitter.onError(error.toThrowable())
                    }
                }
            ))
        }

    private fun Long.getMegaNode(): MegaNode? =
        megaApi.getNodeByHandle(this)
            ?: megaApiFolder.authorizeNode(megaApiFolder.getNodeByHandle(this))
}
