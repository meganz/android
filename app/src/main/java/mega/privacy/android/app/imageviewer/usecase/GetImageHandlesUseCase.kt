package mega.privacy.android.app.imageviewer.usecase

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.kotlin.subscribeBy
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.imageviewer.data.ImageResult
import mega.privacy.android.app.usecase.GetGlobalChangesUseCase
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.MegaNodeUtil.isValidForImageViewer
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.ORDER_PHOTO_ASC
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNode.*
import javax.inject.Inject

/**
 * Use case to retrieve image node handles given different sources
 *
 * @property context                    Context to retrieve offline nodes
 * @property megaApi                    MegaAPI required for node requests
 * @property databaseHandler            DatabaseHandler required for offline nodes
 * @property getGlobalChangesUseCase    GlobalChangesUseCase required to update nodes in realtime
 * @property getNodeUseCase             NodeUseCase required to retrieve node information
 */
class GetImageHandlesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val databaseHandler: DatabaseHandler,
    private val getGlobalChangesUseCase: GetGlobalChangesUseCase,
    private val getNodeUseCase: GetNodeUseCase,
) {

    /**
     * Use case to retrieve image node handles given different sources
     *
     * @param nodeHandles       Image node handles
     * @param parentNodeHandle  Parent node to retrieve every other child
     * @param nodeFileLink      Node public link
     * @param sortOrder         Node search order
     * @param isOffline         Flag to check if it's offline node
     * @return                  Flowable with up-todate image nodes
     */
    fun get(
        nodeHandles: LongArray? = null,
        parentNodeHandle: Long? = null,
        nodeFileLink: String? = null,
        sortOrder: Int? = ORDER_PHOTO_ASC,
        isOffline: Boolean = false
    ): Flowable<List<ImageItem>> =
        Flowable.create({ emitter ->
            val items = mutableListOf<ImageItem>()
            when {
                parentNodeHandle != null && parentNodeHandle != INVALID_HANDLE -> {
                    val parentNode = getNodeUseCase.get(parentNodeHandle).blockingGetOrNull()
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
                !nodeFileLink.isNullOrBlank() -> {
                    val node = getNodeUseCase.getPublicNode(nodeFileLink).blockingGetOrNull()
                    if (node?.isValidForImageViewer() == true) {
                        items.add(node.toImageItem())
                    }
                }
                else -> {
                    emitter.onError(IllegalArgumentException("Invalid parameters"))
                    return@create
                }
            }

            if (items.isNotEmpty()) {
                emitter.onNext(items)
            } else {
                emitter.onError(IllegalArgumentException("Invalid image handles"))
                return@create
            }

            val globalSubscription = getGlobalChangesUseCase.get().subscribeBy(
                onNext = { change ->
                    if (emitter.isCancelled) return@subscribeBy

                    if (change is GetGlobalChangesUseCase.Result.OnNodesUpdate) {
                        change.nodes?.forEach { changedNode ->
                            val index = items.indexOfFirst { it.handle == changedNode.handle }

                            if (changedNode.hasChanged(CHANGE_TYPE_NEW)
                                || changedNode.hasChanged(CHANGE_TYPE_PARENT)
                            ) {
                                val hasSameParent = when {
                                    changedNode.parentHandle == null -> // getParentHandle() can be null
                                        false
                                    parentNodeHandle != null ->
                                        changedNode.parentHandle == parentNodeHandle
                                    items.isNotEmpty() ->
                                        changedNode.parentHandle == items[0].nodeItem?.node?.parentHandle
                                    else ->
                                        false
                                }

                                if (hasSameParent) {
                                    if (changedNode.hasChanged(CHANGE_TYPE_PARENT)) {
                                        items[index] = changedNode.toImageItem()
                                    } else if (changedNode.isValidForImageViewer()) {
                                        items.add(changedNode.toImageItem())
                                    }
                                } else if (changedNode.hasChanged(CHANGE_TYPE_PARENT)) {
                                    items.removeAt(index)
                                }
                            } else if (index != INVALID_POSITION) {
                                if (changedNode.hasChanged(CHANGE_TYPE_REMOVED)) {
                                    items.removeAt(index)
                                } else {
                                    items[index] = changedNode.toImageItem()
                                }
                            }
                        }

                        emitter.onNext(items)
                    }
                }
            )

            emitter.setCancellable {
                globalSubscription.dispose()
            }
        }, BackpressureStrategy.LATEST)

    private fun MutableList<ImageItem>.addNodeHandles(nodeHandles: LongArray) {
        nodeHandles.forEach { nodeHandle ->
            val node = getNodeUseCase.get(nodeHandle).blockingGetOrNull()
            if (node?.isValidForImageViewer() == true) {
                this.add(node.toImageItem())
            }
        }
    }

    private fun MutableList<ImageItem>.addChildrenNodes(megaNode: MegaNode, sortOrder: Int) {
        megaApi.getChildren(megaNode, sortOrder).forEach { node ->
            if (node.isValidForImageViewer()) {
                this.add(node.toImageItem())
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
                                handle = offlineNode.handle.toLong(),
                                name = offlineNode.name,
                                imageResult = ImageResult(
                                    isVideo = MimeTypeList.typeForName(offlineNode.name).isVideo,
                                    fullSizeUri = file.toUri()
                                )
                            )
                        )
                    }
                }
        }
    }

    /**
     * Convert {@link MegaNode} to {@link ImageItem}
     *
     * @return  Resulting ImageItem
     */
    private fun MegaNode.toImageItem(): ImageItem =
        ImageItem(
            handle = handle,
            name = name,
            nodeItem = getNodeUseCase.getNodeItem(this).blockingGet()
        )
}
