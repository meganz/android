package mega.privacy.android.app

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.image.data.ImageItem
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaTransferListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager.*
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import nz.mega.sdk.*
import nz.mega.sdk.MegaError.*
import javax.inject.Inject

class GetImageUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationContext private val context: Context
) {

    fun getFullImage(nodeHandle: Long): Single<ImageItem> =
        Single.create { emitter ->
            val node = megaApi.getNodeByHandle(nodeHandle)

            when {
                !node.isFile -> {
                    emitter.onError(IllegalArgumentException("Node is not a file"))
                }
                !node.isImage() -> {
                    emitter.onError(IllegalArgumentException("Node is not an image"))
                }
                else -> {
                    val file = buildTempFile(context, node.base64Handle + JPG_EXTENSION)

                    if (file.exists()) {
                        val item = ImageItem(
                            node.handle,
                            node.name,
                            fullSizeUri = file.toUri()
                        )
                        emitter.onSuccess(item)
                    } else {
                        megaApi.startDownload(
                            node,
                            file.absolutePath,
                            OptionalMegaTransferListenerInterface(
                                onTransferFinish = { _: MegaTransfer, error: MegaError ->
                                    when (error.errorCode) {
                                        API_OK -> {
                                            val item = ImageItem(
                                                node.handle,
                                                node.name,
                                                fullSizeUri = file.toUri()
                                            )
                                            emitter.onSuccess(item)
                                        }
                                        API_EBUSINESSPASTDUE ->
                                            emitter.onError(IllegalStateException("Business account is overdue"))
                                        else ->
                                            emitter.onError(error.toThrowable())
                                    }
                                }
                            )
                        )
                    }
                }
            }
        }

    fun getPreviewImage(nodeHandle: Long): Single<ImageItem> =
        Single.create { emitter ->
            val node = megaApi.getNodeByHandle(nodeHandle)

            when {
                !node.isFile -> {
                    emitter.onError(IllegalArgumentException("Node is not a file"))
                }
                !node.isImage() -> {
                    emitter.onError(IllegalArgumentException("Node is not an image"))
                }
                !node.hasPreview() -> {
                    emitter.onError(IllegalStateException("Node doesn't have an associated preview"))
                }
                else -> {
                    val file = buildPreviewFile(context, node.base64Handle + JPG_EXTENSION)

                    if (file.exists()) {
                        val item = ImageItem(
                            node.handle,
                            node.name,
                            previewUri = file.toUri()
                        )
                        emitter.onSuccess(item)
                    } else {
                        megaApi.getPreview(
                            node,
                            file.absolutePath,
                            OptionalMegaRequestListenerInterface(
                                onRequestFinish = { _: MegaRequest, error: MegaError ->
                                    when (error.errorCode) {
                                        API_OK -> {
                                            val item = ImageItem(
                                                node.handle,
                                                node.name,
                                                previewUri = file.toUri()
                                            )
                                            emitter.onSuccess(item)
                                        }
                                        API_ENOENT ->
                                            emitter.onError(IllegalStateException("Node doesn't have an associated preview"))
                                        else ->
                                            emitter.onError(error.toThrowable())
                                    }
                                }
                            ))
                    }
                }
            }
        }

    fun getThumbnailImage(nodeHandle: Long): Single<ImageItem> =
        Single.create { emitter ->
            val node = megaApi.getNodeByHandle(nodeHandle)

            when {
                !node.isFile -> {
                    emitter.onError(IllegalArgumentException("Node is not a file"))
                }
                !node.isImage() -> {
                    emitter.onError(IllegalArgumentException("Node is not an image"))
                }
                !node.hasThumbnail() -> {
                    emitter.onError(IllegalStateException("Node doesn't have an associated thumbnail"))
                }
                else -> {
                    val file = buildThumbnailFile(context, node.base64Handle + JPG_EXTENSION)

                    if (file.exists()) {
                        val item = ImageItem(
                            node.handle,
                            node.name,
                            thumbnailUri = file.toUri()
                        )
                        emitter.onSuccess(item)
                    } else {
                        megaApi.getThumbnail(
                            node,
                            file.absolutePath,
                            OptionalMegaRequestListenerInterface(
                                onRequestFinish = { _: MegaRequest, error: MegaError ->
                                    when (error.errorCode) {
                                        API_OK -> {
                                            val item = ImageItem(
                                                node.handle,
                                                node.name,
                                                thumbnailUri = file.toUri()
                                            )
                                            emitter.onSuccess(item)
                                        }
                                        API_ENOENT ->
                                            emitter.onError(IllegalStateException("Node doesn't have an associated thumbnail"))
                                        else ->
                                            emitter.onError(error.toThrowable())
                                    }
                                }
                            ))
                    }
                }
            }
        }

    fun getProgressiveImage(nodeHandle: Long, fullSize: Boolean = false): Flowable<ImageItem> =
        Flowable.create({ emitter ->
            val node = megaApi.getNodeByHandle(nodeHandle)

            when {
                !node.isFile -> {
                    emitter.onError(IllegalArgumentException("Node is not a file"))
                }
                !node.isImage() -> {
                    emitter.onError(IllegalArgumentException("Node is not an image"))
                }
                else -> {
                    val thumbnailFile = buildThumbnailFile(context, node.base64Handle + JPG_EXTENSION)
                    val previewFile = buildPreviewFile(context, node.base64Handle + JPG_EXTENSION)
                    val fullFile = buildTempFile(context, node.base64Handle + JPG_EXTENSION)

                    val thumbnailUri = if (node.hasThumbnail() && thumbnailFile.exists()) thumbnailFile.toUri() else null
                    val previewUri = if (node.hasPreview() && previewFile.exists()) previewFile.toUri() else null
                    val fullSizeUri = if (fullFile.exists()) fullFile.toUri() else null

                    val imageItem = ImageItem(
                        node.handle,
                        node.name,
                        thumbnailUri = thumbnailUri,
                        previewUri = previewUri,
                        fullSizeUri = fullSizeUri
                    )

                    emitter.onNext(imageItem)

                    if (fullSize && imageItem.fullSizeUri != null || imageItem.previewUri != null) {
                        emitter.onComplete()
                        return@create
                    }

                    if (node.hasThumbnail() && !thumbnailFile.exists()) {
                        megaApi.getThumbnail(
                            node,
                            thumbnailFile.absolutePath,
                            OptionalMegaRequestListenerInterface(
                                onRequestFinish = { _: MegaRequest, error: MegaError ->
                                    if (error.errorCode == API_OK) {
                                        imageItem.thumbnailUri = thumbnailFile.toUri()
                                        emitter.onNext(imageItem)
                                    }
                                }
                            ))
                    }

                    if (node.hasPreview() && !previewFile.exists()) {
                        megaApi.getPreview(
                            node,
                            previewFile.absolutePath,
                            OptionalMegaRequestListenerInterface(
                                onRequestFinish = { _: MegaRequest, error: MegaError ->
                                    if (error.errorCode == API_OK) {
                                        imageItem.previewUri = previewFile.toUri()
                                        emitter.onNext(imageItem)
                                    }
                                }
                            ))
                    }

                    if (fullSize && !fullFile.exists()) {
                        megaApi.startDownload(
                            node,
                            fullFile.absolutePath,
                            OptionalMegaTransferListenerInterface(
                                onTransferFinish = { _: MegaTransfer, error: MegaError ->
                                    when (error.errorCode) {
                                        API_OK -> {
                                            imageItem.fullSizeUri = previewFile.toUri()
                                            emitter.onNext(imageItem)
                                            emitter.onComplete()
                                        }
                                        API_EBUSINESSPASTDUE ->
                                            emitter.onError(IllegalStateException("Business account is overdue"))
                                        else ->
                                            emitter.onError(error.toThrowable())
                                    }
                                }
                            )
                        )
                    }
                }
            }
        }, BackpressureStrategy.LATEST)

    private fun MegaNode.isImage(): Boolean =
        this.isFile
//        isFile && MimeTypeList.typeForName(name).isImage

    fun getImages(parentNodeHandle: Long): Flowable<List<ImageItem>> =
        getImages(megaApi.getChildren(megaApi.getNodeByHandle(parentNodeHandle)).map { it.handle })

    fun getImages(nodeHandles: List<Long>, nodePosition: Int = 0): Flowable<List<ImageItem>> =
        Flowable.create({ emitter ->
            val items = sortedMapOf<Long, ImageItem>()

            nodeHandles.forEachIndexed { index, nodeHandle ->
                val node = megaApi.getNodeByHandle(nodeHandle)
                items[node.handle] = ImageItem(node.handle, node.name)

//                if (index in nodePosition - 1..nodePosition + 1) {
//                    getProgressiveImage(node.handle).subscribeBy(
//                        onNext = { imageUri ->
//                            items[node.handle] = ImageItem(node.handle, node.name, imageUri)
//                            emitter.onNext(items.values.toList())
//                        }, onError = { error ->
//                            LogUtil.logError(error.stackTraceToString())
//                        }
//                    )
//                }
            }

            emitter.onNext(items.values.toList())
        }, BackpressureStrategy.LATEST)
}
