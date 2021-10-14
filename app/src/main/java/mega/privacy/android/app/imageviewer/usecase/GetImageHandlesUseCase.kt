package mega.privacy.android.app.imageviewer.usecase

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.utils.MegaNodeUtil.isValidForImageViewer
import mega.privacy.android.app.utils.OfflineUtils
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.ORDER_PHOTO_ASC
import javax.inject.Inject

class GetImageHandlesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val databaseHandler: DatabaseHandler
) {

    fun get(nodeHandles: LongArray): Single<List<ImageItem>> =
        Single.create { emitter ->
            val items = mutableListOf<ImageItem>()

            nodeHandles.forEach { nodeHandle ->
                val node = megaApi.getNodeByHandle(nodeHandle)
                if (node?.isValidForImageViewer() == true) {
                    items.add(ImageItem(node.handle, node.name))
                }
            }

            emitter.onSuccess(items)
        }

    fun getOffline(nodeHandles: LongArray): Single<List<ImageItem>> =
        Single.create { emitter ->
            val offlineNodes = databaseHandler.offlineFiles
            val items = mutableListOf<ImageItem>()

            nodeHandles.forEach { nodeHandle ->
                offlineNodes.find {
                    nodeHandle == it.handle.toLongOrNull() ||
                            nodeHandle == it.handleIncoming.toLongOrNull()
                }?.let { node ->
                    val file = OfflineUtils.getOfflineFile(context, node)
                    if (file.exists()) {
                        items.add(
                            ImageItem(node.handle.toLong(), node.name, fullSizeUri = file.toUri())
                        )
                    }
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
                    if (node.isValidForImageViewer()) {
                        items.add(ImageItem(node.handle, node.name))
                    }
                }
                emitter.onSuccess(items)
            } else {
                emitter.onError(IllegalStateException("Node is null or has no children"))
            }
        }
}
