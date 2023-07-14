package mega.privacy.android.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.constant.FileConstant
import mega.privacy.android.data.extensions.encodeBase64
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.failWithException
import mega.privacy.android.data.extensions.getPreviewFileName
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.extensions.getScreenSize
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.extensions.toException
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.preferences.FileManagementPreferencesGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.node.ImageNodeMapper
import mega.privacy.android.data.model.MimeTypeList
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ImageRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
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
 * @param megaChatApiGateway MegaChatApiGateway
 * @param ioDispatcher CoroutineDispatcher
 * @param cacheGateway CacheGateway
 * @param fileManagementPreferencesGateway FileManagementPreferencesGateway
 * @param fileGateway FileGateway
 * @param imageNodeMapper ImageNodeMapper
 */
internal class DefaultImageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheGateway: CacheGateway,
    private val fileManagementPreferencesGateway: FileManagementPreferencesGateway,
    private val fileGateway: FileGateway,
    private val imageNodeMapper: ImageNodeMapper,
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

    override suspend fun getPublicNodeThumbnailFromLocal(handle: Long): File? =
        withContext(ioDispatcher) {
            megaApiFolderGateway.getMegaNodeByHandle(handle)?.run {
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
                                        continuation.failWithError(error, "getThumbnailFromServer")
                                    }
                                }
                            )
                        )
                    }
                }
            }
        }

    override suspend fun getPublicNodeThumbnailFromServer(handle: Long): File? =
        withContext(ioDispatcher) {
            megaApiFolderGateway.getMegaNodeByHandle(handle)?.let { node ->
                getThumbnailFile(node)?.let { thumbnail ->
                    suspendCancellableCoroutine { continuation ->
                        megaApiFolderGateway.getThumbnail(node, thumbnail.absolutePath,
                            OptionalMegaRequestListenerInterface(
                                onRequestFinish = { _, error ->
                                    if (error.errorCode == MegaError.API_OK) {
                                        continuation.resumeWith(Result.success(thumbnail))
                                    } else {
                                        Timber.e(error.toException("getPublicNodeThumbnailFromServer"))
                                        continuation.failWithError(
                                            error,
                                            "getPublicNodeThumbnailFromServer"
                                        )
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
                                        continuation.failWithError(error, "getPreviewFromServer")
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

    override suspend fun downloadPublicNodeThumbnail(
        handle: Long,
    ): Boolean = withContext(ioDispatcher) {
        val node = megaApiFolderGateway.getMegaNodeByHandle(handle)
        val thumbnailFolderPath = thumbnailFolderPath
        if (node == null || thumbnailFolderPath == null || !node.hasThumbnail()) {
            return@withContext false
        } else {
            return@withContext suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("getThumbnail") {
                    true
                }
                megaApiGateway.getThumbnail(
                    node,
                    getThumbnailPath(thumbnailFolderPath, node),
                    listener
                )

                continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
            }
        }
    }

    override suspend fun downloadPublicNodePreview(
        handle: Long,
    ): Boolean = withContext(ioDispatcher) {
        val node = megaApiFolderGateway.getMegaNodeByHandle(handle)
        val previewFolderPath = previewFolderPath
        if (node == null || previewFolderPath == null || !node.hasPreview()) {
            return@withContext false
        } else {
            return@withContext suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("getThumbnail") {
                    true
                }
                megaApiGateway.getPreview(
                    node,
                    getPreviewPath(previewFolderPath, node),
                    listener
                )

                continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
            }
        }
    }

    private fun getPreviewPath(previewFolderPath: String, megaNode: MegaNode) =
        "$previewFolderPath${File.separator}${megaNode.getPreviewFileName()}"

    private fun getThumbnailPath(thumbnailFolderPath: String, megaNode: MegaNode) =
        "$thumbnailFolderPath${File.separator}${megaNode.getThumbnailFileName()}"

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

        val previewUri = getOrGeneratePreview(fileName, isVideo, file, highPriority)

        return@withContext ImageResult(
            isVideo = isVideo,
            thumbnailUri = thumbnailFile?.takeIf { it.exists() }?.toUri()?.toString(),
            previewUri = previewUri,
            fullSizeUri = file.toUri().toString(),
            isFullyLoaded = true
        )
    }

    override suspend fun getImageFromFile(file: File, highPriority: Boolean): ImageResult =
        withContext(ioDispatcher) {
            val isVideo = MimeTypeList.typeForName(file.name).isVideo
            val fileName =
                "${(file.name + file.length()).encodeBase64()}${FileConstant.JPG_EXTENSION}"

            val previewUri = getOrGeneratePreview(fileName, isVideo, file, highPriority)

            return@withContext ImageResult(
                previewUri = previewUri,
                fullSizeUri = file.toUri().toString(),
                isVideo = isVideo,
                isFullyLoaded = true
            )
        }

    override suspend fun getThumbnailCacheFolderPath(): String? = withContext(ioDispatcher) {
        cacheGateway.getThumbnailCacheFolder()?.path
    }

    override suspend fun getPreviewCacheFolderPath(): String? = withContext(ioDispatcher) {
        cacheGateway.getPreviewCacheFolder()?.path
    }

    override suspend fun getFullSizeCacheFolderPath(): String? = withContext(ioDispatcher) {
        cacheGateway.getFullSizeCacheFolder()?.path
    }

    override suspend fun getImageNodeByHandle(handle: Long): ImageNode =
        withContext(ioDispatcher) {
            getImageNode {
                megaApiGateway.getMegaNodeByHandle(handle)
                    ?: megaApiFolderGateway.getMegaNodeByHandle(handle)
                        ?.let { megaApiFolderGateway.authorizeNode(it) }
            }
        }

    override suspend fun getImageNodeForPublicLink(nodeFileLink: String): ImageNode =
        withContext(ioDispatcher) {
            getImageNode { getPublicMegaNode(nodeFileLink) }
        }

    override suspend fun getImageNodeForChatMessage(
        chatRoomId: Long,
        chatMessageId: Long,
    ): ImageNode =
        withContext(ioDispatcher) {
            getImageNode { getChatMegaNode(chatRoomId, chatMessageId) }
        }

    private suspend fun getImageNode(getMegaNode: suspend () -> MegaNode?): ImageNode =
        withContext(ioDispatcher) {
            val megaNode = getMegaNode()
                ?: throw IllegalArgumentException("Node not found")
            imageNodeMapper(
                megaNode, megaApiGateway::hasVersion
            )
        }

    private suspend fun getPublicMegaNode(nodeFileLink: String) =
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        if (!request.flag) {
                            continuation.resumeWith(Result.success(request.publicMegaNode))
                        } else {
                            continuation.resumeWithException(IllegalArgumentException("Invalid key for public node"))
                        }
                    } else {
                        continuation.failWithException(error.toException("getPublicMegaNode"))
                    }
                }
            )
            megaApiGateway.getPublicNode(nodeFileLink, listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }

    private fun getChatMegaNode(chatRoomId: Long, chatMessageId: Long): MegaNode? {
        val chatMessage = megaChatApiGateway.getMessage(chatRoomId, chatMessageId)
            ?: megaChatApiGateway.getMessageFromNodeHistory(chatRoomId, chatMessageId)

        val megaNode = chatMessage?.let {
            val node = chatMessage.megaNodeList.get(0)
            val chatRoom = megaChatApiGateway.getChatRoom(chatRoomId)

            if (chatRoom?.isPreview == true) {
                megaApiGateway.authorizeChatNode(node, chatRoom.authorizationToken) ?: node
            } else {
                node
            }
        }
        return megaNode
    }

    private suspend fun getOrGeneratePreview(
        previewFileName: String,
        isVideo: Boolean,
        file: File,
        highPriority: Boolean,
    ): String? = withContext(ioDispatcher) {
        val previewFile = getPreviewFile(previewFileName)
        val previewUri = if (previewFile?.exists() == true) {
            previewFile.toUri().toString()
        } else {
            if (isVideo) {
                getVideoThumbnail(previewFilePath = previewFile?.absolutePath, file = file)
            } else {
                getImagePreview(
                    previewFilePath = previewFile?.absolutePath,
                    file = file,
                    highPriority = highPriority
                )
            }
        }
        previewUri
    }

    private suspend fun getImagePreview(
        previewFilePath: String?,
        file: File,
        highPriority: Boolean,
    ): String? =
        withContext(ioDispatcher) {
            val imageFile = file.takeIf { it.exists() && it.length() > SIZE_1_MB }
            imageFile?.let {
                previewFilePath?.let {
                    val previewFile = File(previewFilePath)
                    if (previewFile.exists()) return@withContext previewFile.toUri().toString()

                    val screenSize = context.getScreenSize()
                    val imageRequest = ImageRequestBuilder.newBuilderWithSource(file.toUri())
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
                            Fresco.getImagePipeline().fetchDecodedImage(imageRequest, file.toUri())
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
                                    Timber.d("Preview generated for ${previewFile.name} and ${previewFile.absolutePath}")
                                    continuation.resumeWith(
                                        Result.success(
                                            previewFile.toUri().toString()
                                        )
                                    )
                                    dataSource.close()
                                } ?: run {
                                    Timber.e("Preview generation failed ${previewFile.name}")
                                    continuation.resumeWithException(NullPointerException())
                                    dataSource.close()
                                }
                            }

                            override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
                                Timber.e(
                                    dataSource.failureCause,
                                    "Preview generation failed ${previewFile.name}"
                                )
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
        }

    private suspend fun getPreviewFile(fileName: String): File? =
        cacheGateway.getCacheFile(CacheFolderConstant.PREVIEW_FOLDER, fileName)


    private suspend fun getVideoThumbnail(previewFilePath: String?, file: File): String? =
        withContext(ioDispatcher) {
            previewFilePath?.let {
                val previewFile = File(previewFilePath)
                if (previewFile.exists()) return@withContext previewFile.toUri()
                    .toString()
                val videoFile = file.takeIf { it.exists() }
                videoFile?.let {
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
                        return@withContext previewFile.toUri()
                            .toString()
                    }
                }
            }
        }

    override suspend fun generatePreview(handle: Long, file: File): String? {
        val previewFileName =
            "${handle.toString().encodeBase64()}${FileConstant.JPG_EXTENSION}"
        val isVideo = MimeTypeList.typeForName(file.name).isVideo
        return getOrGeneratePreview(previewFileName, isVideo, file, false)
    }

    override suspend fun generateThumbnail(handle: Long, imageFile: File): String? =
        withContext(ioDispatcher) {
            if (MimeTypeList.typeForName(imageFile.name).isVideo.not()) return@withContext null
            if (!imageFile.exists()) return@withContext null
            val fileName =
                "${handle.toString().encodeBase64()}${FileConstant.JPG_EXTENSION}"
            val thumbnailFile = getThumbnailFile(fileName)
            if (thumbnailFile?.exists() == true) thumbnailFile.delete()
            thumbnailFile?.absolutePath?.let {
                val imageRequest = ImageRequestBuilder.newBuilderWithSource(imageFile.toUri())
                    .setRotationOptions(RotationOptions.autoRotate())
                    .setRequestPriority(Priority.LOW)
                    .setResizeOptions(
                        ResizeOptions.forSquareSize(THUMBNAIL_SIZE)
                    )
                    .build()

                return@withContext suspendCancellableCoroutine { continuation ->
                    val dataSource =
                        Fresco.getImagePipeline().fetchDecodedImage(imageRequest, imageFile.toUri())
                    dataSource.subscribe(object : BaseBitmapDataSubscriber() {
                        override fun onNewResultImpl(bitmap: Bitmap?) {
                            bitmap?.let {
                                BufferedOutputStream(FileOutputStream(thumbnailFile)).apply {
                                    this.use {
                                        bitmap.compress(
                                            Bitmap.CompressFormat.JPEG,
                                            BITMAP_COMPRESS_QUALITY,
                                            it
                                        )
                                    }
                                }
                                bitmap.recycle()
                                Timber.d("Thumbnail generated ${imageFile.name} and ${thumbnailFile.absolutePath}")
                                continuation.resumeWith(
                                    Result.success(
                                        thumbnailFile.toUri().toString()
                                    )
                                )
                                dataSource.close()
                            } ?: run {
                                Timber.e("Thumbnail generation failed ${thumbnailFile.name}")
                                continuation.resumeWithException(NullPointerException())
                                dataSource.close()
                            }
                        }

                        override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
                            Timber.e(
                                dataSource.failureCause,
                                "Thumbnail generation failed ${thumbnailFile.name}"
                            )
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

    companion object {
        private const val SIZE_1_MB = 1024 * 1024 * 1L
        private const val BITMAP_COMPRESS_QUALITY = 75
        private const val THUMBNAIL_SIZE = 200
    }
}
