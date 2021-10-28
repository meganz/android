package mega.privacy.android.app.imageviewer.usecase

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.utils.MegaNodeUtil.isValidForImageViewer
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import mega.privacy.android.app.utils.OfflineUtils
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.ORDER_PHOTO_ASC
import nz.mega.sdk.MegaNode
import javax.inject.Inject

class GetImageHandlesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val databaseHandler: DatabaseHandler
) {

    fun get(
        nodeHandles: LongArray? = null,
        parentNodeHandle: Long? = null,
        sortOrder: Int? = ORDER_PHOTO_ASC,
        isOffline: Boolean = false
    ): Single<List<ImageItem>> =
        Single.create { emitter ->
            val items = mutableListOf<ImageItem>()
            when {
                parentNodeHandle != null && parentNodeHandle != INVALID_HANDLE -> {
                    val parentNode = megaApi.getNodeByHandle(parentNodeHandle)
                    if (parentNode != null && megaApi.hasChildren(parentNode)) {
                        items.addChildrenNodes(parentNode, sortOrder ?: ORDER_PHOTO_ASC)
                    } else {
                        emitter.onError(IllegalStateException("Node is null or has no children"))
                        return@create
                    }
                }
                isOffline && nodeHandles != null && nodeHandles.isNotEmpty() -> {
                    items.addOfflineNodeHandles(nodeHandles)
                }
                nodeHandles != null && nodeHandles.isNotEmpty() -> {
                    items.addNodeHandles(nodeHandles)
                }
                else -> {
                    emitter.onError(IllegalArgumentException("Invalid parameters"))
                    return@create
                }
            }
            emitter.onSuccess(items)
        }

    private fun MutableList<ImageItem>.addNodeHandles(nodeHandles: LongArray) {
        nodeHandles.forEach { nodeHandle ->
            val node = megaApi.getNodeByHandle(nodeHandle)
            if (node?.isValidForImageViewer() == true) {
                this.add(ImageItem(node.handle, node.name, node.isVideo()))
            }
        }
    }

    private fun MutableList<ImageItem>.addChildrenNodes(megaNode: MegaNode, sortOrder: Int) {
        megaApi.getChildren(megaNode, sortOrder).forEach { node ->
            if (node.isValidForImageViewer()) {
                this.add(ImageItem(node.handle, node.name, node.isVideo()))
            }
        }
    }

    private fun MutableList<ImageItem>.addOfflineNodeHandles(nodeHandles: LongArray) {
        val offlineNodes = databaseHandler.offlineFiles
        nodeHandles.forEach { nodeHandle ->
            offlineNodes
                .find { offlineNode ->
                    nodeHandle == offlineNode.handle.toLongOrNull() ||
                            nodeHandle == offlineNode.handleIncoming.toLongOrNull()
                }?.let { offlineNode ->
                    val file = OfflineUtils.getOfflineFile(context, offlineNode)
                    if (file.exists()) {
                        this.add(
                            ImageItem(
                                offlineNode.handle.toLong(),
                                offlineNode.name,
                                MimeTypeList.typeForName(offlineNode.name).isVideo,
                                fullSizeUri = file.toUri()
                            )
                        )
                    }
                }
        }
    }
}
