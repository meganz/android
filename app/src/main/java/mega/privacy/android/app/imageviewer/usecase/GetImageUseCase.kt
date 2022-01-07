package mega.privacy.android.app.imageviewer.usecase

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.DownloadService
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.components.transferWidget.TransfersManagement
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.errors.BusinessAccountOverdueMegaError
import mega.privacy.android.app.errors.QuotaOverdueMegaError
import mega.privacy.android.app.imageviewer.data.ImageResult
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaTransferListenerInterface
import mega.privacy.android.app.usecase.GetChatMessageUseCase
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.utils.CacheFolderManager.*
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.MegaNodeUtil.isGif
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import mega.privacy.android.app.utils.MegaTransferUtils.getNumPendingDownloadsNonBackground
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import nz.mega.sdk.*
import nz.mega.sdk.MegaError.*
import javax.inject.Inject

/**
 * Use case to retrieve a single image
 *
 * @property context                Context required to build files
 * @property megaApi                MegaAPI required for node requests
 * @property getNodeUseCase         NodeUseCase required to retrieve node information
 * @property getChatMessageUseCase  ChatMessageUseCase required to retrieve node information
 */
class GetImageUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val getNodeUseCase: GetNodeUseCase,
    private val getChatMessageUseCase: GetChatMessageUseCase
) {

    /**
     * Get an image given a Node handle.
     *
     * @param nodeHandle    Image Node handle to request.
     * @param fullSize      Flag to request full size image.
     * @param highPriority  Flag to request full image with high priority.
     * @return              Flowable which emits Uri for every image, from low to high resolution.
     */
    fun get(
        nodeHandle: Long,
        fullSize: Boolean = false,
        highPriority: Boolean = false
    ): Flowable<ImageResult> =
        getNodeUseCase.get(nodeHandle)
            .flatMapPublisher { node -> get(node, fullSize, highPriority) }

    /**
     * Get an image given a Node file link.
     *
     * @param nodeFileLink  Image Node file link.
     * @param fullSize      Flag to request full size image.
     * @param highPriority  Flag to request full image with high priority.
     * @return              Flowable which emits Uri for every image, from low to high resolution.
     */
    fun get(
        nodeFileLink: String,
        fullSize: Boolean = false,
        highPriority: Boolean = false
    ): Flowable<ImageResult> =
        getNodeUseCase.getPublicNode(nodeFileLink)
            .flatMapPublisher { node -> get(node, fullSize, highPriority) }

    /**
     * Get an image given a Node Chat Room Id and Chat Message Id.
     *
     * @param chatRoomId        Chat Message Room Id
     * @param chatMessageId     Chat Message Id
     * @param fullSize          Flag to request full size image.
     * @param highPriority      Flag to request full image with high priority.
     * @return                  Flowable which emits Uri for every image, from low to high resolution.
     */
    fun get(
        chatRoomId: Long,
        chatMessageId: Long,
        fullSize: Boolean = false,
        highPriority: Boolean = false
    ): Flowable<ImageResult> =
        getChatMessageUseCase.getChatNode(chatRoomId, chatMessageId)
            .flatMapPublisher { node -> get(node, fullSize, highPriority) }

    /**
     * Get an image given a Node.
     *
     * @param node          Image Node to request.
     * @param fullSize      Flag to request full size image.
     * @param highPriority  Flag to request full image with high priority.
     * @return              Flowable which emits Uri for every image, from low to high resolution.
     */
    fun get(
        node: MegaNode?,
        fullSize: Boolean = false,
        highPriority: Boolean = false
    ): Flowable<ImageResult> =
        Flowable.create({ emitter ->
            when {
                node == null -> emitter.onError(IllegalArgumentException("Node is null"))
                !node.isFile -> emitter.onError(IllegalArgumentException("Node is not a file"))
                else -> {
                    val fileExtension = ".${MimeTypeList.typeForName(node.name).extension}"
                    val thumbnailFile = if (node.hasThumbnail()) buildThumbnailFile(context, node.base64Handle + fileExtension) else null
                    val previewFile = if (node.hasPreview()) buildPreviewFile(context, node.base64Handle + fileExtension) else null
                    val fullFile = buildTempFile(context, node.base64Handle + fileExtension)
                    val isFullSizeRequired = fullSize || node.isGif() || node.isVideo()

                    val image = ImageResult(
                        isVideo = node.isVideo(),
                        thumbnailUri = if (thumbnailFile?.exists() == true) thumbnailFile.toUri() else null,
                        previewUri = if (previewFile?.exists() == true) previewFile.toUri() else null,
                        fullSizeUri = if (fullFile?.exists() == true) fullFile.toUri() else null
                    )

                    if (fullFile?.exists() == true || (!isFullSizeRequired && previewFile?.exists() == true)) {
                        image.fullyLoaded = true
                        emitter.onNext(image)
                        emitter.onComplete()
                        return@create
                    } else {
                        emitter.onNext(image)
                    }

                    if (thumbnailFile != null && !thumbnailFile.exists()) {
                        megaApi.getThumbnail(
                            node,
                            thumbnailFile.absolutePath,
                            OptionalMegaRequestListenerInterface(
                                onRequestFinish = { _: MegaRequest, error: MegaError ->
                                    if (emitter.isCancelled) return@OptionalMegaRequestListenerInterface

                                    if (error.errorCode == API_OK) {
                                        image.thumbnailUri = thumbnailFile.toUri()
                                        emitter.onNext(image)
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
                                    if (emitter.isCancelled) return@OptionalMegaRequestListenerInterface

                                    if (error.errorCode == API_OK) {
                                        image.previewUri = previewFile.toUri()
                                        if (isFullSizeRequired) {
                                            emitter.onNext(image)
                                        } else {
                                            image.fullyLoaded = true
                                            emitter.onNext(image)
                                            emitter.onComplete()
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
                                if (emitter.isCancelled) return@OptionalMegaTransferListenerInterface

                                image.transferTag = transfer.tag
                                emitter.onNext(image)
                            },
                            onTransferFinish = { _: MegaTransfer, error: MegaError ->
                                if (emitter.isCancelled) return@OptionalMegaTransferListenerInterface

                                when (error.errorCode) {
                                    API_OK -> {
                                        image.fullSizeUri = fullFile.toUri()
                                        image.transferTag = null
                                        image.fullyLoaded = true
                                        emitter.onNext(image)
                                        emitter.onComplete()
                                    }
                                    API_EBUSINESSPASTDUE ->
                                        emitter.onError(BusinessAccountOverdueMegaError())
                                    else ->
                                        emitter.onError(error.toThrowable())
                                }

                                resetTotalDownloadsIfNeeded()
                            },
                            onTransferTemporaryError = { _, error ->
                                if (emitter.isCancelled) return@OptionalMegaTransferListenerInterface

                                if (error.errorCode == API_EOVERQUOTA) {
                                    emitter.onError(QuotaOverdueMegaError())
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

    /**
     * Reset MegaApi Total Downloads count to avoid counting background transfers.
     */
    private fun resetTotalDownloadsIfNeeded() {
        val currentTransfers = megaApi.getNumPendingDownloadsNonBackground()
        val isServiceRunning = TransfersManagement.isServiceRunning(DownloadService::class.java)
        if (currentTransfers == 0 && !isServiceRunning) {
            megaApi.resetTotalDownloads()
        }
    }

    /**
     * Get an offline image given a Node handle.
     *
     * @param nodeHandle    Image Node handle to request.
     * @return              Single with the ImageResult
     */
    fun getOffline(nodeHandle: Long): Flowable<ImageResult> =
        Flowable.fromCallable {
            val offlineNode = getNodeUseCase.getOfflineNode(nodeHandle).blockingGetOrNull()
            when {
                offlineNode == null -> error("Offline node was not found")
                offlineNode.isFolder -> error("Offline node is a folder")
                else -> {
                    val file = OfflineUtils.getOfflineFile(context, offlineNode)
                    if (file.exists()) {
                        ImageResult(
                            isVideo = MimeTypeList.typeForName(offlineNode.name).isVideo,
                            fullSizeUri = file.toUri(),
                            fullyLoaded = true
                        )
                    } else {
                        error("Offline file doesn't exist")
                    }
                }
            }
        }
}
