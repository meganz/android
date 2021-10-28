package mega.privacy.android.app.imageviewer.usecase

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.errors.BusinessAccountOverdueMegaError
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaTransferListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager.*
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.MegaNodeUtil.isGif
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import nz.mega.sdk.*
import nz.mega.sdk.MegaError.*
import javax.inject.Inject

class GetImageUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationContext private val context: Context
) {

    fun get(
        nodeHandle: Long,
        fullSize: Boolean = false,
        highPriority: Boolean = false
    ): Flowable<ImageItem> =
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
                    val fileExtension = ".${MimeTypeList.typeForName(node.name).extension}"
                    val thumbnailFile = if (node.hasThumbnail()) buildThumbnailFile(context, node.base64Handle + fileExtension) else null
                    val previewFile = if (node.hasPreview()) buildPreviewFile(context, node.base64Handle + fileExtension) else null
                    val fullFile = buildTempFile(context, node.base64Handle + fileExtension)
                    val isFullSizeRequired = fullSize || node.isGif() || node.isVideo()

                    val imageItem = ImageItem(
                        node.handle,
                        node.name,
                        node.isVideo(),
                        thumbnailUri = if (thumbnailFile?.exists() == true) thumbnailFile.toUri() else null,
                        previewUri = if (previewFile?.exists() == true) previewFile.toUri() else null,
                        fullSizeUri = if (fullFile?.exists() == true) fullFile.toUri() else null
                    )

                    if (fullFile?.exists() == true || (!isFullSizeRequired && previewFile?.exists() == true)) {
                        imageItem.isFullyLoaded = true
                        emitter.onNext(imageItem)
                        emitter.onComplete()
                        return@create
                    } else {
                        emitter.onNext(imageItem)
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
                                        if (!isFullSizeRequired) {
                                            imageItem.isFullyLoaded = true
                                            emitter.onNext(imageItem)
                                            emitter.onComplete()
                                        } else {
                                            emitter.onNext(imageItem)
                                        }
                                    } else if (!isFullSizeRequired) {
                                        emitter.onError(error.toThrowable())
                                    }
                                }
                            ))
                    }

                    if (isFullSizeRequired && !fullFile.exists()) {
                        val listener = OptionalMegaTransferListenerInterface(
                            onTransferStart = { transfer ->
                                imageItem.transferTag = transfer.tag
                                emitter.onNext(imageItem)
                            },
                            onTransferFinish = { _: MegaTransfer, error: MegaError ->
                                if (emitter.isCancelled) return@OptionalMegaTransferListenerInterface

                                when (error.errorCode) {
                                    API_OK -> {
                                        imageItem.fullSizeUri = fullFile.toUri()
                                        imageItem.transferTag = null
                                        imageItem.isFullyLoaded = true
                                        emitter.onNext(imageItem)
                                        emitter.onComplete()
                                    }
                                    API_EBUSINESSPASTDUE ->
                                        emitter.onError(BusinessAccountOverdueMegaError())
                                    else ->
                                        emitter.onError(error.toThrowable())
                                }
                            }
                        )

                        if (highPriority) {
                            megaApi.startDownloadWithTopPriority(
                                node,
                                fullFile.absolutePath,
                                Constants.APP_DATA_BACKGROUND_TRANSFER,
                                listener
                            )
                        } else {
                            megaApi.startDownloadWithData(
                                node,
                                fullFile.absolutePath,
                                Constants.APP_DATA_BACKGROUND_TRANSFER,
                                listener
                            )
                        }
                    }
                }
            }
        }, BackpressureStrategy.LATEST)
}
