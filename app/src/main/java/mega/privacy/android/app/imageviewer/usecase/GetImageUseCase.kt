package mega.privacy.android.app.imageviewer.usecase

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.errors.BusinessAccountOverdueMegaError
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaTransferListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager.*
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.MegaNodeUtil.isGif
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
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
                    val isFullSizeRequired = fullSize || (!node.isVideo() && node.isGif())

                    val imageItem = ImageItem(
                        node.handle,
                        node.name,
                        node.isVideo(),
                        thumbnailUri = if (thumbnailFile?.exists() == true) thumbnailFile.toUri() else null,
                        previewUri = if (previewFile?.exists() == true) previewFile.toUri() else null,
                        fullSizeUri = if (fullFile?.exists() == true) fullFile.toUri() else null
                    )

                    emitter.onNext(imageItem)

                    if (fullFile?.exists() == true || (!isFullSizeRequired && previewFile?.exists() == true)) {
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
                                        if (!isFullSizeRequired) {
                                            emitter.onComplete()
                                        }
                                    } else if (!isFullSizeRequired) {
                                        emitter.onError(error.toThrowable())
                                    }
                                }
                            ))
                    }

                    if (isFullSizeRequired && !fullFile.exists()) {
                        megaApi.startDownloadWithTopPriority(
                            node,
                            fullFile.absolutePath,
                            Constants.APP_DATA_BACKGROUND_TRANSFER,
                            OptionalMegaTransferListenerInterface(
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
                        )
                    }
                }
            }
        }, BackpressureStrategy.LATEST)

    fun cancelTransfer(transferTag: Int): Completable =
        Completable.create { emitter ->
            megaApi.cancelTransferByTag(transferTag, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    when (error.errorCode) {
                        API_OK ->
                            emitter.onComplete()
                        else ->
                            emitter.onError(error.toThrowable())
                    }
                }
            ))
        }
}
