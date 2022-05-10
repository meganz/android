package mega.privacy.android.app.imageviewer.usecase

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.net.toFile
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.subscribeBy
import mega.privacy.android.app.DownloadService
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.imageviewer.data.ImageResult
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaTransferListenerInterface
import mega.privacy.android.app.usecase.*
import mega.privacy.android.app.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.app.usecase.exception.*
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContextUtils.getScreenSize
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MegaNodeUtil.checkValidNodeFile
import mega.privacy.android.app.utils.MegaNodeUtil.getFileName
import mega.privacy.android.app.utils.MegaNodeUtil.getThumbnailFileName
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import mega.privacy.android.app.utils.MegaTransferUtils.getNumPendingDownloadsNonBackground
import mega.privacy.android.app.utils.NetworkUtil.isMeteredConnection
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import mega.privacy.android.app.utils.StringUtils.encodeBase64
import nz.mega.sdk.*
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
 * @property preferences            App preferences to get Mobile Data high resolution setting
 */
class GetImageUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val getNodeUseCase: GetNodeUseCase,
    private val getChatMessageUseCase: GetChatMessageUseCase,
    private val preferences: SharedPreferences
) {

    companion object {
        private const val SIZE_1_MB = 1024 * 1024 * 1L
        private const val SIZE_50_MB = SIZE_1_MB * 50L
    }

    private val isMobileDataAllowed: Boolean by lazy {
        preferences.getBoolean(SettingsConstants.KEY_MOBILE_DATA_HIGH_RESOLUTION, true)
    }

    /**
     * Get an ImageResult given a Node handle.
     *
     * @param nodeHandle    Image Node handle to request.
     * @param fullSize      Flag to request full size image despite data/size requirements.
     * @param highPriority  Flag to request image with high priority.
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
     * @param fullSize      Flag to request full size image despite data/size requirements.
     * @param highPriority  Flag to request image with high priority.
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
     * @param fullSize          Flag to request full size image despite data/size requirements.
     * @param highPriority      Flag to request image with high priority.
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
     * @param fullSize      Flag to request full size image despite data/size requirements.
     * @param highPriority  Flag to request image with high priority.
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
                    val fullSizeRequired = when {
                        node.isTakenDown || node.isVideo() -> false
                        node.size <= SIZE_1_MB -> true
                        node.size in SIZE_1_MB..SIZE_50_MB -> fullSize || isMobileDataAllowed || !context.isMeteredConnection()
                        else -> false
                    }

                    val thumbnailFile = if (node.hasThumbnail()) CacheFolderManager.buildThumbnailFile(context, node.getThumbnailFileName()) else null
                    val previewFile = if (node.hasPreview() || node.isVideo()) CacheFolderManager.buildPreviewFile(context, node.getThumbnailFileName()) else null

                    CacheFolderManager.buildTempFile(context, node.getFileName())?.let { nonNullFullFile ->
                        if (!megaApi.checkValidNodeFile(node, nonNullFullFile)) {
                            FileUtil.deleteFileSafely(nonNullFullFile)
                        }

                        val image = ImageResult(
                            isVideo = node.isVideo(),
                            thumbnailUri = if (thumbnailFile?.exists() == true) thumbnailFile.toUri() else null,
                            previewUri = if (previewFile?.exists() == true) previewFile.toUri() else null,
                            fullSizeUri = if (nonNullFullFile.exists()) nonNullFullFile.toUri() else null
                        )

                        if (image.isVideo && nonNullFullFile.exists() && previewFile == null) {
                            image.previewUri = getVideoThumbnail(node.getThumbnailFileName(), nonNullFullFile.toUri()).blockingGetOrNull()
                        }

                        if ((!fullSizeRequired && previewFile?.exists() == true) || megaApi.checkValidNodeFile(node, nonNullFullFile)) {
                            image.isFullyLoaded = true
                            emitter.onNext(image)
                            emitter.onComplete()
                            return@create
                        } else {
                            emitter.onNext(image)
                        }

                        if (thumbnailFile != null && !thumbnailFile.exists()) {
                            getThumbnailImage(node, thumbnailFile.absolutePath).subscribeBy(
                                onComplete = {
                                    image.thumbnailUri = thumbnailFile.toUri()
                                    emitter.onNext(image)
                                },
                                onError = { error ->
                                    logWarning(error.stackTraceToString())
                                }
                            )
                        }

                        if (previewFile != null && !previewFile.exists()) {
                            getPreviewImage(node, previewFile.absolutePath).subscribeBy(
                                onComplete = {
                                    image.previewUri = previewFile.toUri()
                                    if (fullSizeRequired) {
                                        emitter.onNext(image)
                                    } else {
                                        image.isFullyLoaded = true
                                        emitter.onNext(image)
                                        emitter.onComplete()
                                    }
                                },
                                onError = { error ->
                                    if (!fullSizeRequired) {
                                        emitter.onError(error)
                                    } else {
                                        logWarning(error.stackTraceToString())
                                    }
                                }
                            )
                        }

                        if (fullSizeRequired && !nonNullFullFile.exists()) {
                            val listener = OptionalMegaTransferListenerInterface(
                                onTransferStart = { transfer ->
                                    if (emitter.isCancelled) return@OptionalMegaTransferListenerInterface

                                    image.transferTag = transfer.tag
                                    emitter.onNext(image)
                                },
                                onTransferFinish = { _: MegaTransfer, error: MegaError ->
                                    if (emitter.isCancelled) return@OptionalMegaTransferListenerInterface

                                    image.transferTag = null

                                    when (val megaException = error.toMegaException()) {
                                        is SuccessMegaException -> {
                                            image.fullSizeUri = nonNullFullFile.toUri()
                                            image.isFullyLoaded = true
                                            emitter.onNext(image)
                                            emitter.onComplete()
                                        }
                                        is ResourceAlreadyExistsMegaException -> {
                                            if (megaApi.checkValidNodeFile(node, nonNullFullFile)) {
                                                image.fullSizeUri = nonNullFullFile.toUri()
                                                image.isFullyLoaded = true
                                                emitter.onNext(image)
                                                emitter.onComplete()
                                            } else {
                                                FileUtil.deleteFileSafely(nonNullFullFile)
                                                emitter.onError(megaException)
                                            }
                                        }
                                        is ResourceDoesNotExistMegaException -> {
                                            image.isFullyLoaded = true
                                            emitter.onNext(image)
                                            emitter.onComplete()
                                        }
                                        else ->
                                            emitter.onError(megaException)
                                    }

                                    resetTotalDownloadsIfNeeded()
                                },
                                onTransferTemporaryError = { _, error ->
                                    if (emitter.isCancelled) return@OptionalMegaTransferListenerInterface
                                    val megaException = error.toMegaException()

                                    if (megaException is QuotaExceededMegaException) {
                                        emitter.onError(megaException)
                                    }
                                }
                            )

                            megaApi.startDownload(
                                node,
                                nonNullFullFile.absolutePath,
                                Constants.APP_DATA_BACKGROUND_TRANSFER,
                                null,
                                highPriority,
                                null,
                                listener
                            )
                        }
                    }
                }
            }
        }, BackpressureStrategy.LATEST)

    /**
     * Get the thumbnail of a node.
     *
     * @param node      Node to retrieve thumbnail from
     * @param filePath  Destination path for the thumbnail
     * @return          Completable
     */
    fun getThumbnailImage(node: MegaNode, filePath: String): Completable =
        Completable.create { emitter ->
            if (!node.hasThumbnail()) {
                emitter.onError(ResourceDoesNotExistMegaException("Node has no thumbnail"))
                return@create
            }

            megaApi.getThumbnail(
                node,
                filePath,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { _: MegaRequest, error: MegaError ->
                        if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                        val megaException = error.toMegaException()
                        if (megaException is SuccessMegaException) {
                            emitter.onComplete()
                        } else {
                            emitter.onError(megaException)
                        }
                    }
                ))
        }

    /**
     * Get the preview of a node.
     *
     * @param node      Node to retrieve preview from
     * @param filePath  Destination path for the preview
     * @return          Completable
     */
    fun getPreviewImage(node: MegaNode, filePath: String): Completable =
        Completable.create { emitter ->
            if (!node.hasPreview()) {
                emitter.onError(ResourceDoesNotExistMegaException("Node has no preview"))
                return@create
            }

            megaApi.getPreview(
                node,
                filePath,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { _: MegaRequest, error: MegaError ->
                        if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                        val megaException = error.toMegaException()
                        if (megaException is SuccessMegaException) {
                            emitter.onComplete()
                        } else {
                            emitter.onError(megaException)
                        }
                    }
                ))
        }

    /**
     * Get an ImageResult given an offline node handle.
     *
     * @param nodeHandle    Image Node handle to request.
     * @return              Single with the ImageResult
     */
    fun getOfflineNode(nodeHandle: Long): Flowable<ImageResult> =
        Flowable.fromCallable {
            val offlineNode = getNodeUseCase.getOfflineNode(nodeHandle).blockingGetOrNull()
            when {
                offlineNode == null -> error("Offline node was not found")
                offlineNode.isFolder -> error("Offline node is a folder")
                else -> {
                    val file = OfflineUtils.getOfflineFile(context, offlineNode)
                    if (file.exists()) {
                        val isVideo = MimeTypeList.typeForName(offlineNode.name).isVideo
                        val thumbnailFileName = "${offlineNode.handle.encodeBase64()}$JPG_EXTENSION"
                        val thumbnailFile = CacheFolderManager.buildThumbnailFile(context, thumbnailFileName)
                        var previewFile = CacheFolderManager.buildPreviewFile(context, thumbnailFileName)

                        previewFile?.let { nonNullPreviewFile ->
                            if (!nonNullPreviewFile.exists()) {
                                if (isVideo) {
                                    previewFile = getVideoThumbnail(thumbnailFileName, file.toUri()).blockingGetOrNull()?.toFile()
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    previewFile = getImageThumbnail(thumbnailFileName, file.toUri()).blockingGetOrNull()?.toFile()
                                }
                            }
                            thumbnailFile?.let { nonNullThumbNailFile ->
                                ImageResult(
                                    isVideo = isVideo,
                                    thumbnailUri = if (nonNullThumbNailFile.exists()) nonNullThumbNailFile.toUri() else null,
                                    previewUri = if (nonNullPreviewFile.exists()) nonNullPreviewFile.toUri() else null,
                                    fullSizeUri = file.toUri(),
                                    isFullyLoaded = true
                                )
                            }
                        }

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
            if (file.exists() && file.canRead()) {
                val isVideo = MimeTypeList.typeForName(file.name).isVideo

                val previewName = "${(file.name + file.length()).encodeBase64()}$JPG_EXTENSION"
                var previewUri: Uri? = null
                if (isVideo) {
                    previewUri = getVideoThumbnail(previewName, file.toUri()).blockingGetOrNull()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    previewUri = getImageThumbnail(previewName, file.toUri()).blockingGetOrNull()
                }

                ImageResult(
                    previewUri = previewUri,
                    fullSizeUri = file.toUri(),
                    isVideo = isVideo,
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
    fun getVideoThumbnail(fileName: String, videoUri: Uri): Single<Uri> =
        Single.fromCallable {
            val videoFile = videoUri.toFile()
            require(videoFile.exists())

            CacheFolderManager.buildPreviewFile(context, fileName)?.let { previewFile->
                if (previewFile.exists()) return@fromCallable previewFile.toUri()

                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ThumbnailUtils.createVideoThumbnail(videoFile, context.getScreenSize(), null)
                } else {
                    ThumbnailUtils.createVideoThumbnail(videoFile.path, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND)
                }
                requireNotNull(bitmap)

                BufferedOutputStream(FileOutputStream(previewFile)).apply {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, this)
                    close()
                }
                bitmap.recycle()

                previewFile.toUri()
            }
        }

    /**
     * Generate a thumbnail given an image file
     *
     * @param fileName  Thumbnail file name
     * @param imageUri  Image to get thumbnail from
     * @return          Single with generated file uri
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun getImageThumbnail(fileName: String, imageUri: Uri): Single<Uri> =
        Single.fromCallable {
            require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)

            val imageFile = imageUri.toFile()
            require(imageFile.exists())
            require(imageFile.length() > SIZE_1_MB) { "Image too small" }

            CacheFolderManager.buildPreviewFile(context, fileName)?.let { previewFile->
                if (previewFile.exists()) return@fromCallable previewFile.toUri()
                val bitmap = ThumbnailUtils.createImageThumbnail(imageFile, context.getScreenSize(), null)
                BufferedOutputStream(FileOutputStream(previewFile)).apply {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, this)
                    close()
                }
                bitmap.recycle()

                previewFile.toUri()
            }
        }

    /**
     * Reset MegaApi Total Downloads count to avoid counting background transfers.
     */
    private fun resetTotalDownloadsIfNeeded() {
        val currentTransfers = megaApi.getNumPendingDownloadsNonBackground()
        val isServiceRunning = TransfersManagement.isServiceRunning(DownloadService::class.java)
        if (currentTransfers == 0 && !isServiceRunning) {
            @Suppress("DEPRECATION")
            megaApi.resetTotalDownloads()
        }
    }
}
