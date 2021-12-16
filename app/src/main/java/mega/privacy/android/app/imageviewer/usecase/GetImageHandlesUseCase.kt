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
import mega.privacy.android.app.usecase.GetChatMessageUseCase
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
 * @property getChatMessageUseCase      ChatMessageUseCase required to retrieve chat node information
 * @property getGlobalChangesUseCase    GlobalChangesUseCase required to update nodes in realtime
 * @property getNodeUseCase             NodeUseCase required to retrieve node information
 */
class GetImageHandlesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val databaseHandler: DatabaseHandler,
    private val getChatMessageUseCase: GetChatMessageUseCase,
    private val getGlobalChangesUseCase: GetGlobalChangesUseCase,
    private val getNodeUseCase: GetNodeUseCase,
) {

    /**
     * Use case to retrieve image node handles given different sources
     *
     * @param nodeHandles       Image node handles
     * @param parentNodeHandle  Parent node to retrieve every other child
     * @param nodeFileLinks     Node public link
     * @param chatRoomId        Node Chat Message Room Id.
     * @param chatMessageIds    Node Chat Message Ids.
     * @param sortOrder         Node search order
     * @param isOffline         Flag to check if it's offline node
     * @return                  Flowable with up-todate image nodes
     */
    fun get(
        nodeHandles: LongArray? = null,
        parentNodeHandle: Long? = null,
        nodeFileLinks: List<String>? = null,
        chatRoomId: Long? = null,
        chatMessageIds: LongArray? = null,
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
                nodeHandles?.isNotEmpty() == true -> {
                    if (isOffline) {
                        items.addOfflineNodeHandles(nodeHandles)
                    } else {
                        items.addNodeHandles(nodeHandles)
                    }
                }
                nodeFileLinks?.isNotEmpty() == true ->
                    items.addNodeFileLinks(nodeFileLinks)
                chatRoomId != null && chatMessageIds?.isNotEmpty() == true ->
                    items.addChatChildren(chatRoomId, chatMessageIds)
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

                            if (changedNode.hasChanged(CHANGE_TYPE_NEW) || changedNode.hasChanged(CHANGE_TYPE_PARENT)) {
                                val hasSameParent = when {
                                    changedNode.parentHandle == null -> { // MegaNode.getParentHandle() can be null
                                        false
                                    }
                                    parentNodeHandle != null -> {
                                        changedNode.parentHandle == parentNodeHandle
                                    }
                                    items.isNotEmpty() -> {
                                        val node = items
                                            .firstOrNull { !it.isOffline && it.nodePublicLink == null }
                                            ?.let { getNodeUseCase.get(it.handle).blockingGetOrNull() }

                                        changedNode.parentHandle == node?.parentHandle
                                    }
                                    else -> {
                                        false
                                    }
                                }

                                if (hasSameParent) {
                                    if (changedNode.hasChanged(CHANGE_TYPE_PARENT)) {
                                        items[index] = changedNode.toImageItem(isDirty = true)
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
                                    items[index] = changedNode.toImageItem(isDirty = true)
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

    /**
     * Add Children nodes to a List of ImageItem given a MegaNode and a Sort Order
     *
     * @param megaNode      MegaNode to obtain children from
     * @param sortOrder     Sort Order for obtaining children
     */
    private fun MutableList<ImageItem>.addChildrenNodes(megaNode: MegaNode, sortOrder: Int) {
        megaApi.getChildren(megaNode, sortOrder).forEach { node ->
            if (node.isValidForImageViewer()) {
                this.add(node.toImageItem())
            }
        }
    }

    /**
     * Add MegaNode nodes to a List of ImageItem given their node handles.
     *
     * @param nodeHandles   Node handles to obtain MegaNode from
     */
    private fun MutableList<ImageItem>.addNodeHandles(nodeHandles: LongArray) {
        nodeHandles.forEach { handle ->
            val node = getNodeUseCase.get(handle).blockingGetOrNull()
            if (node?.isValidForImageViewer() == true) {
                this.add(node.toImageItem())
            }
        }
    }

    /**
     * Add MegaOffline nodes to a List of ImageItem given their node handles.
     *
     * @param nodeHandles   Node handles to obtain MegaOffline from
     */
    private fun MutableList<ImageItem>.addOfflineNodeHandles(nodeHandles: LongArray) {
        val existingOfflineNodes = databaseHandler.offlineFiles
        nodeHandles.forEach { nodeHandle ->
            existingOfflineNodes.find {
                nodeHandle == it.handle.toLongOrNull()
                        || nodeHandle == it.handleIncoming.toLongOrNull()
            }?.let { offlineNode ->
                if (!offlineNode.isFolder) {
                    val file = OfflineUtils.getOfflineFile(context, offlineNode)
                    if (file.exists()) {
                        val item = ImageItem(
                                handle = offlineNode.handle.toLong(),
                                imageResult = ImageResult(
                                        isVideo = MimeTypeList.typeForName(offlineNode.name).isVideo,
                                        fullSizeUri = file.toUri(),
                                        fullyLoaded = true
                                ),
                                isOffline = true
                        )
                        this.add(item)
                    }
                }
            }
        }
    }

    /**
     * Add MegaNode nodes to a List of ImageItem given their node public link.
     *
     * @param nodeFileLinks     Node public link to obtain MegaNode from
     */
    private fun MutableList<ImageItem>.addNodeFileLinks(nodeFileLinks: List<String>) {
        nodeFileLinks.forEach { nodeFileLink ->
            val node = getNodeUseCase.getPublicNode(nodeFileLink).blockingGetOrNull()
            if (node?.isValidForImageViewer() == true) {
                this.add(ImageItem(node.handle, nodePublicLink = nodeFileLink))
            }
        }
    }

    /**
     * Add MegaNode nodes to a List of ImageItem given their chat room Ids and message Ids.
     *
     * @param messageIds    Node Chat Message Ids.
     * @param chatRoomId    Node Chat Message Room Id.
     */
    private fun MutableList<ImageItem>.addChatChildren(chatRoomId: Long, messageIds: LongArray) {
        messageIds.forEach { messageId ->
            val node = getChatMessageUseCase.getChatNode(chatRoomId, messageId).blockingGetOrNull()
            if (node?.isValidForImageViewer() == true) {
                this.add(ImageItem(node.handle, chatRoomId = chatRoomId, chatMessageId = messageId))
            }
        }
    }

    /**
     * Convert MegaOffline to ImageItem
     *
     * @param isOffline Flag to check if Node is available offline
     * @param isDirty   Flag to check if Node needs to be updated
     * @return  Resulting ImageItem
     */
    private fun MegaNode.toImageItem(
        isOffline: Boolean = false,
        isDirty: Boolean = false
    ): ImageItem =
        ImageItem(handle, isOffline = isOffline, isDirty = isDirty)
}
