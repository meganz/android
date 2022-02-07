package mega.privacy.android.app.imageviewer.usecase

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils.createVideoThumbnail
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toFile
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.DownloadService
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.errors.BusinessAccountOverdueMegaError
import mega.privacy.android.app.errors.QuotaOverdueMegaError
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.imageviewer.data.ImageResult
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaTransferListenerInterface
import mega.privacy.android.app.usecase.GetChatMessageUseCase
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.utils.CacheFolderManager.*
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContextUtils.getScreenSize
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MegaNodeUtil.getFileName
import mega.privacy.android.app.utils.MegaNodeUtil.getThumbnailFileName
import mega.privacy.android.app.utils.MegaNodeUtil.isImage
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import mega.privacy.android.app.utils.MegaTransferUtils.getNumPendingDownloadsNonBackground
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import mega.privacy.android.app.utils.StringUtils.encodeBase64
import nz.mega.sdk.*
import nz.mega.sdk.MegaError.*
import java.io.BufferedOutputStream
import java.io.FileOutputStream
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
     * Get an ImageResult given a Node handle.
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
     * Get an ImageResult given a Node file link.
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
     * Get an ImageResult given a Node Chat Room Id and Chat Message Id.
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
     * Get an ImageResult given a Node.
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
                    val hasReadAccess = megaApi.getAccess(node) != MegaShare.ACCESS_UNKNOWN
                    val isFullSizeRequired = hasReadAccess && (fullSize || !node.isImage())

                    val thumbnailFile = if (node.hasThumbnail()) buildThumbnailFile(context, node.getThumbnailFileName()) else null
                    val previewFile = if (node.hasPreview() || node.isVideo()) buildPreviewFile(context, node.getThumbnailFileName()) else null
                    val fullFile = if (hasReadAccess) buildTempFile(context, node.getFileName()) else null

                    if (fullFile?.exists() == true && (!fullFile.canRead() || fullFile.length() != node.size)) {
                        FileUtil.deleteFileSafely(fullFile)
                    }

                    val image = ImageResult(
                        isVideo = node.isVideo(),
                        thumbnailUri = if (thumbnailFile?.exists() == true) thumbnailFile.toUri() else null,
                        previewUri = if (previewFile?.exists() == true) previewFile.toUri() else null,
                        fullSizeUri = if (fullFile?.exists() == true) fullFile.toUri() else null
                    )

                    if (image.isVideo && fullFile?.exists() == true && previewFile == null) {
                        image.previewUri = getVideoPreviewImage(node.getThumbnailFileName(), fullFile.toUri()).blockingGetOrNull()
                    }

                    if ((!hasReadAccess && previewFile?.exists() == true) || (fullFile?.exists() == true && (previewFile == null || previewFile.exists()))) {
                        image.isFullyLoaded = true
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
                                    } else {
                                        logWarning(error.toThrowable().stackTraceToString())
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
                                            image.isFullyLoaded = true
                                            emitter.onNext(image)
                                            emitter.onComplete()
                                        }
                                    } else if (!isFullSizeRequired) {
                                        emitter.onError(error.toThrowable())
                                    } else {
                                        logWarning(error.toThrowable().stackTraceToString())
                                    }
                                }
                            ))
                    }

                    if (isFullSizeRequired && fullFile != null && !fullFile.exists()) {
                        val listener = OptionalMegaTransferListenerInterface(
                            onTransferStart = { transfer ->
                                if (emitter.isCancelled) return@OptionalMegaTransferListenerInterface

                                image.transferTag = transfer.tag
                                emitter.onNext(image)
                            },
                            onTransferFinish = { _: MegaTransfer, error: MegaError ->
                                if (emitter.isCancelled) return@OptionalMegaTransferListenerInterface

                                when (error.errorCode) {
                                    API_OK, API_EEXIST -> {
                                        image.fullSizeUri = fullFile.toUri()
                                        image.transferTag = null
                                        image.isFullyLoaded = true
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

                        megaApi.startDownload(
                            node,
                            fullFile.absolutePath,
                            Constants.APP_DATA_BACKGROUND_TRANSFER,
                            null,
                            highPriority,
                            null,
                            listener
                        )
                    }
                }
            }
        }, BackpressureStrategy.LATEST)

    /**
     * Get an ImageResult given an offline node handle.
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
                        val isVideo = MimeTypeList.typeForName(offlineNode.name).isVideo
                        val thumbnailFileName = "${offlineNode.handle.encodeBase64()}${FileUtil.JPG_EXTENSION}"
                        val thumbnailFile = buildThumbnailFile(context, thumbnailFileName)
                        val previewFile = buildPreviewFile(context, thumbnailFileName)

                        if (isVideo && !previewFile.exists()) {
                            getVideoPreviewImage(thumbnailFileName, file.toUri()).blockingGetOrNull()
                        }

                        ImageResult(
                            isVideo = isVideo,
                            thumbnailUri = if (thumbnailFile.exists()) thumbnailFile.toUri() else null,
                            previewUri = if (previewFile.exists()) previewFile.toUri() else null,
                            fullSizeUri = file.toUri(),
                            isFullyLoaded = true
                        )
                    } else {
                        error("Offline file doesn't exist")
                    }
                }
            }
        }

    /**
     * Get an ImageResult given an Image file uri.
     *
     * @param imageUri      Image file uri
     * @return              Single with the ImageResult
     */
    fun getImageUri(imageUri: Uri): Flowable<ImageResult> =
        Flowable.fromCallable {
            val file = imageUri.toFile()
            if (file.exists()) {
                ImageResult(
                    fullSizeUri = file.toUri(),
                    isFullyLoaded = true
                )
            } else {
                error("Image file doesn't exist")
            }
        }

    /**
     * Generate a thumbnail given a video file
     *
     * @param fileName  Thumbnail file name
     * @param videoUri  Video to get thumbnail from
     * @return          Single with generated file uri
     */
    @Suppress("deprecation")
    fun getVideoPreviewImage(fileName: String, videoUri: Uri): Single<Uri> =
        Single.fromCallable {
            val videoFile = videoUri.toFile()
            require(videoFile.exists())

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val screenSize = context.getScreenSize()
                createVideoThumbnail(videoFile, screenSize, null)
            } else {
                createVideoThumbnail(videoFile.path, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND)
            }
            requireNotNull(bitmap)

            val previewFile = buildPreviewFile(context, fileName)
            BufferedOutputStream(FileOutputStream(previewFile)).apply {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, this)
                close()
            }
            bitmap.recycle()

            previewFile.toUri()
        }

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
}
