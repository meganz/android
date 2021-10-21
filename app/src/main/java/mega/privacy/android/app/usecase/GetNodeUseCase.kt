package mega.privacy.android.app.usecase

import android.app.Activity
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.errors.BusinessAccountOverdueMegaError
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.OfflineUtils
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import java.io.File
import javax.inject.Inject

class GetNodeUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val databaseHandler: DatabaseHandler
) {

    fun get(nodeHandle: Long): Single<MegaNode> =
        Single.fromCallable {
            megaApi.getNodeByHandle(nodeHandle)
        }

    fun getNodeItem(nodeHandle: Long): Single<MegaNodeItem> =
        Single.fromCallable {
            val node = megaApi.getNodeByHandle(nodeHandle)
            val nodeAccess = megaApi.getAccess(node)

            val hasFullAccess = nodeAccess == MegaShare.ACCESS_OWNER || nodeAccess == MegaShare.ACCESS_FULL
            val isFromRubbishBin = node.parentHandle == megaApi.rubbishNode.handle
            val isFromInbox = node.parentHandle == megaApi.inboxNode.handle
            val isFromRoot = node.parentHandle == megaApi.rootNode.handle
            val isAvailableOffline = isNodeAvailableOffline(nodeHandle).blockingGet()

            MegaNodeItem(
                node,
                hasFullAccess,
                isFromRubbishBin,
                isFromInbox,
                isFromRoot,
                isAvailableOffline
            )
        }

    fun markAsFavorite(nodeHandle: Long, isFavorite: Boolean): Completable =
        Completable.fromCallable {
            val node = megaApi.getNodeByHandle(nodeHandle)
            megaApi.setNodeFavourite(node, isFavorite)
        }

    fun isNodeAvailableOffline(nodeHandle: Long): Single<Boolean> =
        Single.fromCallable {
            val node = megaApi.getNodeByHandle(nodeHandle)

            if (databaseHandler.exists(nodeHandle)) {
                databaseHandler.findByHandle(nodeHandle)?.let { offlineNode ->
                    val offlineFile = OfflineUtils.getOfflineFile(context, offlineNode)
                    val isFileAvailable = FileUtil.isFileAvailable(offlineFile)
                    val isFileDownloadedLatest = FileUtil.isFileDownloadedLatest(offlineFile, node)
                    return@fromCallable isFileAvailable && isFileDownloadedLatest
                }
            }

            return@fromCallable false
        }

    fun setNodeAvailableOffline(
        activity: Activity,
        nodeHandle: Long,
        setAvailableOffline: Boolean
    ): Completable =
        Completable.fromCallable {
            val isCurrentlyAvailable = isNodeAvailableOffline(nodeHandle).blockingGet()

            if (setAvailableOffline) {
                if (!isCurrentlyAvailable) {
                    val node = megaApi.getNodeByHandle(nodeHandle)
                    val offlineParent = OfflineUtils.getOfflineParentFile(activity, Constants.FROM_OTHERS, node, megaApi)
                    if (FileUtil.isFileAvailable(offlineParent)) {
                        val parentName = OfflineUtils.getOfflineParentFileName(activity, node).absolutePath + File.separator
                        val offlineNode = databaseHandler.findbyPathAndName(parentName, node.name)
                        OfflineUtils.removeOffline(offlineNode, databaseHandler, activity)
                    }

                    OfflineUtils.saveOffline(offlineParent, node, activity)
                }
            } else if (isCurrentlyAvailable) {
                val offlineNode = databaseHandler.findByHandle(nodeHandle)
                OfflineUtils.removeOffline(offlineNode, databaseHandler, activity)
            }
        }

    fun copyNode(nodeHandle: Long, newParentHandle: Long): Completable =
        Completable.create { emitter ->
            val currentNode = megaApi.getNodeByHandle(nodeHandle)
            val newParentNode = megaApi.getNodeByHandle(newParentHandle)
            megaApi.copyNode(currentNode, newParentNode, OptionalMegaRequestListenerInterface(
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

    fun moveNode(nodeHandle: Long, newParentHandle: Long): Completable =
        Completable.create { emitter ->
            val currentNode = megaApi.getNodeByHandle(nodeHandle)
            val newParentNode = megaApi.getNodeByHandle(newParentHandle)
            megaApi.moveNode(currentNode, newParentNode, OptionalMegaRequestListenerInterface(
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
        moveNode(nodeHandle, megaApi.rubbishNode.handle)

    fun removeNode(nodeHandle: Long): Completable =
        Completable.create { emitter ->
            val node = megaApi.getNodeByHandle(nodeHandle)
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
}
