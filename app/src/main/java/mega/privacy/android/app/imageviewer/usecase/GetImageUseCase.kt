package mega.privacy.android.app.imageviewer.usecase

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.imageviewer.data.ImageItem
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

    fun get(nodeHandle: Long, fullSize: Boolean = false): Flowable<ImageItem> =
        Flowable.create({ emitter ->
            val node = megaApi.getNodeByHandle(nodeHandle)

            when {
                node == null -> {
                    emitter.onError(IllegalArgumentException("Node doesn't exist"))
                }
                !node.isFile -> {
                    emitter.onError(IllegalArgumentException("Node is not a file"))
                }
                else -> {
                    val thumbnailFile = if (node.hasThumbnail()) buildThumbnailFile(context, node.base64Handle + JPG_EXTENSION) else null
                    val previewFile = if (node.hasPreview()) buildPreviewFile(context, node.base64Handle + JPG_EXTENSION) else null
                    val fullFile = buildTempFile(context, node.base64Handle + JPG_EXTENSION)

                    val imageItem = ImageItem(
                        node.handle,
                        node.name,
                        thumbnailUri = if (thumbnailFile?.exists() == true) thumbnailFile.toUri() else null,
                        previewUri = if (previewFile?.exists() == true) previewFile.toUri() else null,
                        fullSizeUri = if (fullFile?.exists() == true) fullFile.toUri() else null
                    )

                    emitter.onNext(imageItem)

                    if (fullFile?.exists() == true || (!fullSize && previewFile?.exists() == true)) {
                        emitter.onComplete()
                        return@create
                    }

                    if (thumbnailFile != null && !thumbnailFile.exists()) {
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

                    if (previewFile != null && !previewFile.exists()) {
                        megaApi.getPreview(
                            node,
                            previewFile.absolutePath,
                            OptionalMegaRequestListenerInterface(
                                onRequestFinish = { _: MegaRequest, error: MegaError ->
                                    if (error.errorCode == API_OK) {
                                        imageItem.previewUri = previewFile.toUri()
                                        emitter.onNext(imageItem)
                                        if (!fullSize) {
                                            emitter.onComplete()
                                        }
                                    } else if (!fullSize) {
                                        emitter.onError(error.toThrowable())
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
                                            imageItem.fullSizeUri = fullFile.toUri()
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
}
