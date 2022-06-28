package mega.privacy.android.app.imageviewer.usecase

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.chat.DeleteChatMessageUseCase
import mega.privacy.android.app.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.app.utils.FileUtil

import mega.privacy.android.app.utils.MegaNodeUtil.getInfoText
import mega.privacy.android.app.utils.MegaNodeUtil.isValidForImageViewer
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.ORDER_PHOTO_ASC
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Use case to retrieve image node handles given different sources
 *
 * @property context                    Context to retrieve offline nodes
 * @property megaApi                    MegaAPI required for node requests
 * @property getChatMessageUseCase      ChatMessageUseCase required to retrieve chat node information
 * @property getNodeUseCase             NodeUseCase required to retrieve node information
 * @property deleteChatMessageUseCase   UseCase required to delete current chat node message
 */
class GetImageHandlesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val getChatMessageUseCase: GetChatMessageUseCase,
    private val getNodeUseCase: GetNodeUseCase,
    private val deleteChatMessageUseCase: DeleteChatMessageUseCase,
) {

    /**
     * Use case to retrieve ImageItems given different sources
     *
     * @param nodeHandles       Image node handles
     * @param parentNodeHandle  Parent node to retrieve every other child
     * @param nodeFileLinks     Node public link
     * @param chatRoomId        Node Chat Message Room Id
     * @param chatMessageIds    Node Chat Message Ids
     * @param imageFileUri      Image file uri
     * @param showNearbyFiles   Show nearby image files from current parent file
     * @param sortOrder         Node search order
     * @param isOffline         Flag to check if it's offline node
     * @return                  Single with image nodes
     */
    fun get(
        nodeHandles: LongArray? = null,
        parentNodeHandle: Long? = null,
        nodeFileLinks: List<String>? = null,
        chatRoomId: Long? = null,
        chatMessageIds: LongArray? = null,
        imageFileUri: Uri? = null,
        showNearbyFiles: Boolean? = false,
        sortOrder: Int? = ORDER_PHOTO_ASC,
        isOffline: Boolean? = false,
    ): Single<List<ImageItem>> =
        Single.fromCallable {
            val items = mutableListOf<ImageItem>()
            when {
                parentNodeHandle != null && parentNodeHandle != INVALID_HANDLE -> {
                    val parentNode = getNodeUseCase.get(parentNodeHandle).blockingGetOrNull()
                    if (parentNode != null && megaApi.hasChildren(parentNode)) {
                        items.addChildrenNodes(parentNode, sortOrder ?: ORDER_PHOTO_ASC)
                    } else {
                        error("Node is null or has no children")
                    }
                }
                nodeHandles?.isNotEmpty() == true -> {
                    if (isOffline == true) {
                        items.addOfflineNodeHandles(nodeHandles)
                    } else {
                        items.addNodeHandles(nodeHandles)
                    }
                }
                nodeFileLinks?.isNotEmpty() == true ->
                    items.addNodeFileLinks(nodeFileLinks)
                chatRoomId != null && chatMessageIds?.isNotEmpty() == true ->
                    items.addChatChildren(chatRoomId, chatMessageIds)
                imageFileUri != null ->
                    items.addFileImageUris(imageFileUri, showNearbyFiles)
                else -> {
                    error("Invalid parameters")
                }
            }

            items.ifEmpty {
                error("Invalid image handles")
            }
        }

    /**
     * Add Children nodes to a List of ImageItem given a MegaNode and a Sort Order
     *
     * @param megaNode      MegaNode to obtain children from
     * @param sortOrder     Sort Order for obtaining children
     */
    private fun MutableList<ImageItem>.addChildrenNodes(megaNode: MegaNode, sortOrder: Int) {
        megaApi.getChildren(megaNode, sortOrder).forEach { node ->
            if (node.isValidForImageViewer()) {
                this.add(
                    ImageItem.Node(
                        id = node.handle,
                        handle = node.handle,
                        name = node.name,
                        infoText = node.getInfoText()
                    )
                )
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
                this.add(
                    ImageItem.Node(
                        id = node.handle,
                        handle = node.handle,
                        name = node.name,
                        infoText = node.getInfoText()
                    )
                )
            }
        }
    }

    /**
     * Add MegaOffline nodes to a List of ImageItem given their node handles.
     *
     * @param nodeHandles   Node handles to obtain MegaOffline from
     */
    private fun MutableList<ImageItem>.addOfflineNodeHandles(nodeHandles: LongArray) {
        nodeHandles.forEach { nodeHandle ->
            val node = getNodeUseCase.getOfflineNode(nodeHandle).blockingGetOrNull()
            if (node?.isValidForImageViewer() == true) {
                this.add(
                    ImageItem.OfflineNode(
                        id = node.handle.toLong(),
                        handle = node.handle.toLong(),
                        name = node.name,
                        infoText = node.getInfoText(context),
                        fileUri = OfflineUtils.getOfflineFile(context, node).toUri(),
                    )
                )
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
                this.add(
                    ImageItem.PublicNode(
                        id = nodeFileLink.hashCode().toLong(),
                        handle = node.handle,
                        name = node.name,
                        infoText = node.getInfoText(),
                        nodePublicLink = nodeFileLink
                    )
                )
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
                val deletable =
                    deleteChatMessageUseCase.check(chatRoomId, messageId).blockingGetOrNull()
                        ?: false
                this.add(
                    ImageItem.ChatNode(
                        id = (chatRoomId.hashCode() + messageId.hashCode()).toLong(),
                        handle = node.handle,
                        name = node.name,
                        infoText = node.getInfoText(),
                        chatRoomId = chatRoomId,
                        chatMessageId = messageId,
                        isDeletable = deletable
                    )
                )
            }
        }
    }

    /**
     * Add Images to a list of ImageItem given an image file uri
     *
     * @param imageFileUri      Image file uri to retrieve images from
     * @param showNearbyFiles   Show nearby image files from current parent file
     */
    private fun MutableList<ImageItem>.addFileImageUris(
        imageFileUri: Uri,
        showNearbyFiles: Boolean? = false,
    ) {
        var imageUris = listOf(imageFileUri)
        if (showNearbyFiles == true) {
            try {
                val nearbyImages = imageFileUri.toFile().parentFile?.listFiles()?.map(File::toUri)
                if (!nearbyImages.isNullOrEmpty()) {
                    imageUris = nearbyImages
                }
            } catch (ignored: Exception) {
                Timber.w(ignored)
            }
        }

        imageUris.forEach { imageUri ->
            val file = imageUri.toFile()
            if (FileUtil.isValidForImageViewer(imageUri.toFile())) {
                val fileTime = TimeUtils.formatLongDateTime(file.lastModified())
                val fileSize = file.length().toString()
                val infoText = TextUtil.getFileInfo(fileSize, fileTime)

                this.add(
                    ImageItem.File(
                        id = imageUri.hashCode().toLong(),
                        name = file.name,
                        infoText = infoText,
                        fileUri = imageUri
                    )
                )
            } else {
                Timber.w("File ($imageUri) can't be read or isn't valid for Image Viewer")
            }
        }
    }
}
