package mega.privacy.android.app.imageviewer.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.imageviewer.data.ImageItem
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.ORDER_PHOTO_ASC
import nz.mega.sdk.MegaChatApiAndroid
import javax.inject.Inject

class GetImageHandlesUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid
) {

    fun get(nodeHandles: List<Long>): Single<List<ImageItem>> =
        Single.create { emitter ->
            val items = mutableListOf<ImageItem>()
            nodeHandles.forEach { nodeHandle ->
                val node = megaApi.getNodeByHandle(nodeHandle)
                if (node != null) {
                    items.add(ImageItem(node.handle, node.name))
                }
            }
            emitter.onSuccess(items)
        }

    fun getChildren(
        parentNodeHandle: Long,
        order: Int? = ORDER_PHOTO_ASC
    ): Single<List<ImageItem>> =
        Single.create { emitter ->
            val parentNode = megaApi.getNodeByHandle(parentNodeHandle)

            if (parentNode != null && megaApi.hasChildren(parentNode)) {
                val items = mutableListOf<ImageItem>()
                megaApi.getChildren(parentNode, order ?: ORDER_PHOTO_ASC).forEach { node ->
                    items.add(ImageItem(node.handle, node.name))
                }
                emitter.onSuccess(items)
            } else {
                emitter.onError(IllegalStateException("Node is null or has no children"))
            }
        }

    fun getFromChatMessages(chatId: Long, messageIds: Array<Long>): Single<List<ImageItem>> =
        Single.create { emitter ->
            val items = mutableListOf<ImageItem>()

            messageIds.forEach { messageId ->
                val message = megaChatApi.getMessage(chatId, messageId)
                val node = message.megaNodeList.get(0)
                items.add(ImageItem(node.handle, node.name))
//                for (i in 0 until message.megaNodeList.size()) {
//                    val node = nodeList[i]
//                    if (node.isImage()) {
//                        items.add(ImageItem(node.handle, node.name))
//                    }
//                }
            }

            emitter.onSuccess(items)
        }
}
