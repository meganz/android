package mega.privacy.android.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.facebook.common.executors.CallerThreadExecutor
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequestBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.constant.FileConstant
import mega.privacy.android.data.extensions.encodeBase64
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.failWithException
import mega.privacy.android.data.extensions.getPreviewFileName
import mega.privacy.android.data.extensions.getScreenSize
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.extensions.isVideo
import mega.privacy.android.data.extensions.toException
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.preferences.FileManagementPreferencesGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.model.FullImageDownloadResult
import mega.privacy.android.data.model.MimeTypeList
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.ResourceAlreadyExistsMegaException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ImageRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

/**
 * The repository implementation class regarding thumbnail feature.
 *
 * @param context Context
 * @param megaApiGateway MegaApiGateway
 * @param ioDispatcher CoroutineDispatcher
 * @param cacheGateway CacheGateway
 * @param fileManagementPreferencesGateway FileManagementPreferencesGateway
 * @param fileGateway FileGateway
 */
internal class DefaultImageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaApiGateway: MegaApiGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheGateway: CacheGateway,
    private val fileManagementPreferencesGateway: FileManagementPreferencesGateway,
    private val fileGateway: FileGateway,
) : ImageRepository {

    private var thumbnailFolderPath: String? = null

    private var previewFolderPath: String? = null

    init {
        runBlocking(ioDispatcher) {
            thumbnailFolderPath =
                cacheGateway.getOrCreateCacheFolder(CacheFolderConstant.THUMBNAIL_FOLDER)?.path
            previewFolderPath =
                cacheGateway.getOrCreateCacheFolder(CacheFolderConstant.PREVIEW_FOLDER)?.path
        }
    }

    override suspend fun getThumbnailFromLocal(handle: Long): File? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(handle)?.run {
                getThumbnailFile(this).takeIf {
                    it?.exists() ?: false
                }
            }
        }

    private suspend fun getThumbnailFile(node: MegaNode): File? =
        cacheGateway.getCacheFile(
            CacheFolderConstant.THUMBNAIL_FOLDER,
            "${node.base64Handle}${FileConstant.JPG_EXTENSION}"
        )

    override suspend fun getThumbnailFromServer(handle: Long): File? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(handle)?.let { node ->
                getThumbnailFile(node)?.let { thumbnail ->
                    suspendCancellableCoroutine { continuation ->
                        megaApiGateway.getThumbnail(node, thumbnail.absolutePath,
                            OptionalMegaRequestListenerInterface(
                                onRequestFinish = { _, error ->
                                    if (error.errorCode == MegaError.API_OK) {
                                        continuation.resumeWith(Result.success(thumbnail))
                                    } else {
                                        continuation.failWithError(error)
                                    }
                                }
                            )
                        )
                    }
                }
            }
        }

    private suspend fun getPreviewFile(node: MegaNode): File? =
        cacheGateway.getCacheFile(
            CacheFolderConstant.PREVIEW_FOLDER,
            "${node.base64Handle}${FileConstant.JPG_EXTENSION}"
        )

    private suspend fun getFullFile(node: MegaNode): File? =
        cacheGateway.getCacheFile(
            CacheFolderConstant.TEMPORARY_FOLDER,
            "${node.base64Handle}.${MimeTypeList.typeForName(node.name).extension}"
        )

    override suspend fun getPreviewFromLocal(handle: Long): File? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(handle)?.run {
                getPreviewFile(this).takeIf {
                    it?.exists() ?: false
                }
            }
        }

    override suspend fun getPreviewFromServer(handle: Long): File? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(handle)?.let { node ->
                getPreviewFile(node)?.let { preview ->
                    suspendCancellableCoroutine { continuation ->
                        megaApiGateway.getPreview(node, preview.absolutePath,
                            OptionalMegaRequestListenerInterface(
                                onRequestFinish = { _, error ->
                                    if (error.errorCode == MegaError.API_OK) {
                                        continuation.resumeWith(Result.success(preview))
                                    } else {
                                        continuation.failWithError(error)
                                    }
                                }
                            )
                        )
                    }
                }
            }
        }

    override suspend fun downloadThumbnail(
        handle: Long,
        callback: (success: Boolean) -> Unit,
    ) = withContext(ioDispatcher) {
        val node = megaApiGateway.getMegaNodeByHandle(handle)
        if (node == null || thumbnailFolderPath == null || !node.hasThumbnail()) {
            callback(false)
        } else {
            megaApiGateway.getThumbnail(
                node,
                getThumbnailPath(thumbnailFolderPath ?: return@withContext, node),
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { _, error ->
                        callback(error.errorCode == MegaError.API_OK)
                    }
                )
            )
        }
    }

    override suspend fun downloadPreview(
        handle: Long,
        callback: (success: Boolean) -> Unit,
    ) = withContext(ioDispatcher) {
        val node = megaApiGateway.getMegaNodeByHandle(handle)
        if (node == null || previewFolderPath == null || !node.hasPreview()) {
            callback(false)
        } else {
            megaApiGateway.getPreview(
                node,
                getPreviewPath(previewFolderPath ?: return@withContext, node),
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { _, error ->
                        callback(error.errorCode == MegaError.API_OK)
                    }
                )
            )
        }
    }

    private fun getPreviewPath(previewFolderPath: String, megaNode: MegaNode) =
        "$previewFolderPath${File.separator}${megaNode.getPreviewFileName()}"

    private fun getThumbnailPath(thumbnailFolderPath: String, megaNode: MegaNode) =
        "$thumbnailFolderPath${File.separator}${megaNode.getThumbnailFileName()}"


    override suspend fun getImageByNodeHandle(
        nodeHandle: Long,
        fullSize: Boolean,
        highPriority: Boolean,
        isMeteredConnection: Boolean,
        resetDownloads: () -> Unit,
    ): Flow<ImageResult> = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeHandle)?.let {
            if (!it.isFile) throw IllegalArgumentException("Node is not a file")
            return@let getImageByNode(
                it,
                fullSize,
                highPriority,
                isMeteredConnection,
                resetDownloads
            )
        } ?: throw IllegalArgumentException("Node is null")
    }

    override suspend fun getImageByNodePublicLink(
        nodeFileLink: String,
        fullSize: Boolean,
        highPriority: Boolean,
        isMeteredConnection: Boolean,
        resetDownloads: () -> Unit,
    ): Flow<ImageResult> = withContext(ioDispatcher) {
        if (nodeFileLink.isBlank()) throw IllegalArgumentException("Invalid megaFileLink")
        return@withContext getImageByNode(
            getPublicNode(nodeFileLink),
            fullSize,
            highPriority,
            isMeteredConnection,
            resetDownloads
        )
    }

    override suspend fun getImageForChatMessage(
        chatRoomId: Long,
        chatMessageId: Long,
        fullSize: Boolean,
        highPriority: Boolean,
        isMeteredConnection: Boolean,
        resetDownloads: () -> Unit,
    ): Flow<ImageResult> = withContext(ioDispatcher) {
        getChatNode(chatRoomId, chatMessageId)?.let {
            return@let getImageByNode(
                it,
                fullSize,
                highPriority,
                isMeteredConnection,
                resetDownloads
            )
        } ?: throw IllegalArgumentException("Node is null")
    }

    private suspend fun getThumbnailFile(fileName: String): File? =
        cacheGateway.getCacheFile(CacheFolderConstant.THUMBNAIL_FOLDER, fileName)

    override suspend fun getImageByOfflineFile(
        offlineNodeInformation: OfflineNodeInformation,
        file: File,
        highPriority: Boolean,
    ): ImageResult = withContext(ioDispatcher) {
        val isVideo = MimeTypeList.typeForName(offlineNodeInformation.name).isVideo

        val fileName =
            "${offlineNodeInformation.handle.encodeBase64()}${FileConstant.JPG_EXTENSION}"
        val thumbnailFile = getThumbnailFile(fileName)
        var previewFile = getPreviewFile(fileName)

        if (previewFile?.exists() != true) {
            previewFile = if (isVideo) {
                getVideoThumbnail(fileName = fileName, videoUri = file.toUri())?.toUri()?.toFile()
            } else {
                getImagePreview(
                    fileName = fileName,
                    imageUri = file.toUri(),
                    highPriority = highPriority
                )?.toUri()?.toFile()
            }
        }

        return@withContext ImageResult(
            isVideo = isVideo,
            thumbnailUri = thumbnailFile?.takeIf { it.exists() }?.toUri().toString(),
            previewUri = previewFile?.takeIf { it.exists() }?.toUri().toString(),
            fullSizeUri = file.toUri().toString(),
            isFullyLoaded = true
        )
    }

    override suspend fun getImageFromFile(file: File, highPriority: Boolean): ImageResult =
        withContext(ioDispatcher) {
            val isVideo = MimeTypeList.typeForName(file.name).isVideo
            val previewName =
                "${(file.name + file.length()).encodeBase64()}${FileConstant.JPG_EXTENSION}"

            val previewUri = if (isVideo) {
                getVideoThumbnail(fileName = previewName, videoUri = file.toUri())
            } else {
                getImagePreview(
                    fileName = previewName,
                    imageUri = file.toUri(),
                    highPriority = highPriority
                )
            }

            return@withContext ImageResult(
                previewUri = previewUri,
                fullSizeUri = file.toUri().toString(),
                isVideo = isVideo,
                isFullyLoaded = true
            )
        }

    private suspend fun getImagePreview(
        fileName: String,
        imageUri: Uri,
        highPriority: Boolean,
    ): String? =
        withContext(ioDispatcher) {
            val imageFile = imageUri.toFile().takeIf { it.exists() && it.length() > SIZE_1_MB }
            imageFile?.let { getPreviewFile(fileName) }?.let { previewFile ->
                if (previewFile.exists()) return@withContext previewFile.toUri().toString()

                val screenSize = context.getScreenSize()
                val imageRequest = ImageRequestBuilder.newBuilderWithSource(imageUri)
                    .setRotationOptions(RotationOptions.autoRotate())
                    .setRequestPriority(if (highPriority) Priority.HIGH else Priority.LOW)
                    .setResizeOptions(
                        ResizeOptions.forDimensions(
                            screenSize.width,
                            screenSize.height
                        )
                    )
                    .build()

                return@withContext suspendCancellableCoroutine { continuation ->
                    val dataSource =
                        Fresco.getImagePipeline().fetchDecodedImage(imageRequest, imageUri)
                    dataSource.subscribe(object : BaseBitmapDataSubscriber() {
                        override fun onNewResultImpl(bitmap: Bitmap?) {
                            bitmap?.let {
                                BufferedOutputStream(FileOutputStream(previewFile)).apply {
                                    this.use {
                                        bitmap.compress(
                                            Bitmap.CompressFormat.JPEG,
                                            BITMAP_COMPRESS_QUALITY,
                                            it
                                        )
                                    }
                                }
                                bitmap.recycle()
                                continuation.resumeWith(
                                    Result.success(
                                        previewFile.toUri().toString()
                                    )
                                )
                                dataSource.close()
                            } ?: run {
                                continuation.resumeWithException(NullPointerException())
                                dataSource.close()
                            }
                        }

                        override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
                            continuation.resumeWithException(dataSource.failureCause ?: return)
                            dataSource.close()
                        }
                    }, CallerThreadExecutor.getInstance())
                    continuation.invokeOnCancellation {
                        dataSource.close()
                    }
                }
            }
        }

    private suspend fun getPublicNode(nodeFileLink: String): MegaNode =
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        if (!request.flag) {
                            continuation.resumeWith(Result.success(request.publicNode))
                        } else {
                            continuation.resumeWithException(IllegalArgumentException("Invalid key for public node"))
                        }
                    } else {
                        continuation.failWithException(error.toException())
                    }
                }
            )
            megaApiGateway.getPublicNode(nodeFileLink, listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }

    private suspend fun getChatNode(chatId: Long, chatMessageId: Long) =
        withContext(ioDispatcher) {
            val chatMessage = megaChatApiGateway.getMessage(chatId, chatMessageId)
                ?: megaChatApiGateway.getMessageFromNodeHistory(chatId, chatMessageId)

            chatMessage?.let {
                val node = chatMessage.megaNodeList.get(0)
                val chatRoom = megaChatApiGateway.getChatRoom(chatId)

                if (chatRoom?.isPreview == true) {
                    megaApiGateway.authorizeChatNode(node, chatRoom.authorizationToken)
                } else {
                    node
                }
            }
        }

    private suspend fun getFullImageFromServer(
        imageResult: ImageResult,
        node: MegaNode,
        fullFile: File,
        highPriority: Boolean,
        isValidNodeFile: Boolean,
        resetDownloads: () -> Unit,
    ): Flow<FullImageDownloadResult> = callbackFlow {
        val listener = OptionalMegaTransferListenerInterface(
            onTransferStart = { transfer ->
                imageResult.transferTag = transfer.tag
                imageResult.totalBytes = transfer.totalBytes
                trySend(FullImageDownloadResult(imageResult))
            },
            onTransferFinish = { _: MegaTransfer, error: MegaError ->
                imageResult.transferTag = null
                when (error.errorCode) {
                    MegaError.API_OK -> {
                        imageResult.fullSizeUri =
                            fullFile.toUri().toString()
                        imageResult.isFullyLoaded = true
                        trySend(FullImageDownloadResult(imageResult))
                    }
                    MegaError.API_EEXIST -> {
                        if (isValidNodeFile) {
                            imageResult.fullSizeUri =
                                fullFile.toUri().toString()
                            imageResult.isFullyLoaded = true
                            trySend(FullImageDownloadResult(imageResult))
                        } else {
                            trySend(
                                FullImageDownloadResult(
                                    deleteFile = fullFile,
                                    exception = ResourceAlreadyExistsMegaException(
                                        error.errorCode,
                                        error.errorString
                                    )
                                )
                            )
                        }
                    }
                    MegaError.API_ENOENT -> {
                        imageResult.isFullyLoaded = true
                        trySend(FullImageDownloadResult(imageResult = imageResult))
                    }
                    else -> {
                        trySend(
                            FullImageDownloadResult(
                                exception = MegaException(
                                    error.errorCode,
                                    error.errorString
                                )
                            )
                        )
                    }
                }
                resetDownloads()
            },
            onTransferTemporaryError = { _, error ->
                if (error.errorCode == MegaError.API_EOVERQUOTA) {
                    imageResult.isFullyLoaded = true
                    trySend(
                        FullImageDownloadResult(
                            imageResult = imageResult,
                            exception = QuotaExceededMegaException(
                                error.errorCode,
                                error.errorString
                            )
                        )
                    )
                }
            },
            onTransferUpdate = {
                imageResult.transferredBytes = it.transferredBytes
                trySend(FullImageDownloadResult(imageResult = imageResult))
            }
        )

        megaApiGateway.getFullImage(
            node,
            fullFile,
            highPriority, listener
        )

        awaitClose {
            megaApiGateway.removeTransferListener(listener)
        }
    }

    private suspend fun getImageByNode(
        node: MegaNode,
        fullSize: Boolean,
        highPriority: Boolean,
        isMeteredConnection: Boolean,
        resetDownloads: () -> Unit,
    ): Flow<ImageResult> = flow {
        val fullSizeRequired =
            isFullSizeRequired(
                node,
                fullSize,
                fileManagementPreferencesGateway.isMobileDataAllowed(),
                isMeteredConnection
            )

        val thumbnailFile = if (node.hasThumbnail()) getThumbnailFile(node) else null

        val previewFile =
            if (node.hasPreview() || node.isVideo()) getPreviewFile(node) else null

        getFullFile(node)?.let { fullFile ->
            val isValidNodeFile = megaApiGateway.checkValidNodeFile(node, fullFile)
            if (!isValidNodeFile) {
                fileGateway.deleteFile(fullFile)
            }

            val imageResult = ImageResult(
                isVideo = node.isVideo(),
                thumbnailUri = thumbnailFile?.takeIf { it.exists() }?.toUri().toString(),
                previewUri = previewFile?.takeIf { it.exists() }?.toUri().toString(),
                fullSizeUri = fullFile.takeIf { it.exists() }?.toUri().toString(),
            )

            if (imageResult.isVideo && imageResult.fullSizeUri != null && previewFile == null) {
                imageResult.previewUri = getVideoThumbnail(
                    node.getThumbnailFileName(),
                    fullFile.toUri()
                )
            }

            if ((!fullSizeRequired && !imageResult.previewUri.isNullOrBlank()) || isValidNodeFile) {
                imageResult.isFullyLoaded = true
                emit(imageResult)
            } else {
                emit(imageResult)
                if (imageResult.thumbnailUri == null) {
                    runCatching {
                        getThumbnailFromServer(node.handle)
                    }.onSuccess {
                        imageResult.thumbnailUri = it?.toUri().toString()
                        emit(imageResult)
                    }.onFailure {
                        Timber.w(it)
                    }
                }

                if (imageResult.previewUri == null) {
                    runCatching {
                        getPreviewFromServer(node.handle)
                    }.onSuccess {
                        imageResult.previewUri = it?.toUri().toString()
                        if (!fullSizeRequired) {
                            imageResult.isFullyLoaded = true
                            emit(imageResult)
                        } else {
                            emit(imageResult)
                            if (imageResult.fullSizeUri == null) {
                                getFullImageFromServer(
                                    imageResult,
                                    node,
                                    fullFile,
                                    highPriority,
                                    isValidNodeFile,
                                    resetDownloads
                                ).collect { result ->
                                    result.imageResult?.let { downloadImageResult ->
                                        emit(downloadImageResult)
                                    }
                                    result.deleteFile?.let { file ->
                                        fileGateway.deleteFile(file)
                                    }
                                    result.exception?.let { exception ->
                                        throw exception
                                    }
                                }
                            }
                        }
                    }.onFailure { exception ->
                        if (!fullSizeRequired) {
                            throw exception
                        } else {
                            Timber.w(exception)
                        }
                    }
                }
            }
        } ?: throw IllegalArgumentException("Full image file is null")
    }


    private fun isFullSizeRequired(
        node: MegaNode,
        fullSize: Boolean,
        isMobileDataAllowed: Boolean,
        isMeteredConnection: Boolean,
    ) = when {
        node.isTakenDown || node.isVideo() -> false
        node.size <= SIZE_1_MB -> true
        node.size in SIZE_1_MB..SIZE_50_MB -> fullSize || isMobileDataAllowed || !isMeteredConnection
        else -> false
    }

    private suspend fun getPreviewFile(fileName: String): File? =
        cacheGateway.getCacheFile(CacheFolderConstant.PREVIEW_FOLDER, fileName)

    @Suppress("deprecation")
    suspend fun getVideoThumbnail(fileName: String, videoUri: Uri): String? =
        withContext(ioDispatcher) {
            val videoFile = videoUri.toFile().takeIf { it.exists() }
            videoFile?.let { getPreviewFile(fileName) }?.let { previewFile ->
                if (previewFile.exists()) return@withContext previewFile.toUri().toString()

                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ThumbnailUtils.createVideoThumbnail(
                        videoFile,
                        context.getScreenSize(),
                        null
                    )
                } else {
                    ThumbnailUtils.createVideoThumbnail(
                        videoFile.path,
                        MediaStore.Images.Thumbnails.FULL_SCREEN_KIND
                    )
                }

                bitmap?.let {
                    BufferedOutputStream(FileOutputStream(previewFile)).apply {
                        this.use {
                            bitmap.compress(
                                Bitmap.CompressFormat.JPEG,
                                BITMAP_COMPRESS_QUALITY,
                                it,
                            )
                        }
                    }
                    bitmap.recycle()
                    return@withContext previewFile.toUri().toString()
                }
            }
        }

    companion object {
        private const val SIZE_1_MB = 1024 * 1024 * 1L
        private const val SIZE_50_MB = SIZE_1_MB * 50L
        private const val BITMAP_COMPRESS_QUALITY = 75
    }

}