package mega.privacy.android.app

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.subscribeBy
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.image.data.ImageItem
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaTransferListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager.*
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.*
import nz.mega.sdk.MegaError.*
import javax.inject.Inject

class GetImageUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationContext private val context: Context
) {

    fun getFullImage(nodeHandle: Long): Single<Uri> =
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
                        emitter.onSuccess(file.toUri())
                    } else {
                        megaApi.startDownload(
                            node,
                            file.absolutePath,
                            OptionalMegaTransferListenerInterface(
                                onTransferFinish = { _: MegaTransfer, error: MegaError ->
                                    when (error.errorCode) {
                                        API_OK ->
                                            emitter.onSuccess(file.toUri())
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

    fun getPreviewImage(nodeHandle: Long): Single<Uri> =
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
                        emitter.onSuccess(file.toUri())
                    } else {
                        megaApi.getPreview(
                            node,
                            file.absolutePath,
                            OptionalMegaRequestListenerInterface(
                                onRequestFinish = { _: MegaRequest, error: MegaError ->
                                    when (error.errorCode) {
                                        API_OK ->
                                            emitter.onSuccess(file.toUri())
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

    fun getThumbnailImage(nodeHandle: Long): Single<Uri> =
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
                        emitter.onSuccess(file.toUri())
                    } else {
                        megaApi.getThumbnail(
                            node,
                            file.absolutePath,
                            OptionalMegaRequestListenerInterface(
                                onRequestFinish = { _: MegaRequest, error: MegaError ->
                                    when (error.errorCode) {
                                        API_OK ->
                                            emitter.onSuccess(file.toUri())
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

    fun getProgressiveImage(nodeHandle: Long, fullSize: Boolean = false): Flowable<Uri> =
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
                    if (node.hasThumbnail()) {
                        val thumbnailFile =
                            buildThumbnailFile(context, node.base64Handle + JPG_EXTENSION)
                        if (thumbnailFile.exists()) {
                            emitter.onNext(thumbnailFile.toUri())
                        } else {
                            megaApi.getThumbnail(
                                node,
                                thumbnailFile.absolutePath,
                                OptionalMegaRequestListenerInterface(
                                    onRequestFinish = { _: MegaRequest, error: MegaError ->
                                        if (error.errorCode == API_OK) {
                                            emitter.onNext(thumbnailFile.toUri())
                                        }
                                    }
                                ))
                        }
                    }

                    if (node.hasPreview()) {
                        val previewFile =
                            buildPreviewFile(context, node.base64Handle + JPG_EXTENSION)
                        if (previewFile.exists()) {
                            emitter.onNext(previewFile.toUri())
                        } else {
                            megaApi.getThumbnail(
                                node,
                                previewFile.absolutePath,
                                OptionalMegaRequestListenerInterface(
                                    onRequestFinish = { _: MegaRequest, error: MegaError ->
                                        if (error.errorCode == API_OK) {
                                            emitter.onNext(previewFile.toUri())
                                        }
                                    }
                                ))
                        }
                    }

                    if (fullSize) {
                        val fullFile = buildTempFile(context, node.base64Handle + JPG_EXTENSION)
                        if (fullFile.exists()) {
                            emitter.onNext(fullFile.toUri())
                            emitter.onComplete()
                        } else {
                            megaApi.startDownload(
                                node,
                                fullFile.absolutePath,
                                OptionalMegaTransferListenerInterface(
                                    onTransferFinish = { _: MegaTransfer, error: MegaError ->
                                        when (error.errorCode) {
                                            API_OK -> {
                                                emitter.onNext(fullFile.toUri())
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
            }
        }, BackpressureStrategy.LATEST)

    private fun MegaNode.isImage(): Boolean =
        this.isFile
//        isFile && MimeTypeList.typeForName(name).isImage

    fun getImages(parentNodeHandle: Long): Flowable<List<ImageItem>> =
        getImages(megaApi.getChildren(megaApi.getNodeByHandle(parentNodeHandle)).map { it.handle })

    fun getImages(nodeHandles: List<Long>): Flowable<List<ImageItem>> =
        Flowable.create({ emitter ->
            val items = sortedMapOf<Long, ImageItem>()
            nodeHandles.forEach { node ->
                val childNode = megaApi.getNodeByHandle(node)
                Log.wtf("TEST", "ChildNode: ${childNode.name}")

                getProgressiveImage(childNode.handle).subscribeBy(
                    onNext = { imageUri ->
                        Log.wtf("TEST", "ImageUri: $imageUri")
                        items[childNode.handle] = ImageItem(childNode.handle, childNode.name, imageUri)
                        emitter.onNext(items.values.toList())
                    }, onError = { error ->
                        logError(error.stackTraceToString())
                    }
                )
            }
        }, BackpressureStrategy.LATEST)
}
