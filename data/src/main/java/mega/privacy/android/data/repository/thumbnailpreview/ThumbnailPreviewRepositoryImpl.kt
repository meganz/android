package mega.privacy.android.data.repository.thumbnailpreview

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.constant.FileConstant
import mega.privacy.android.data.extensions.encodeBase64
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getPreviewFileName
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.extensions.toException
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

internal class ThumbnailPreviewRepositoryImpl @Inject constructor(
    private val megaApi: MegaApiGateway,
    private val megaApiFolder: MegaApiFolderGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheGateway: CacheGateway,
) : ThumbnailPreviewRepository {

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
            megaApi.getMegaNodeByHandle(handle)?.run {
                getThumbnailFile(this).takeIf {
                    it?.exists() ?: false
                }
            }
        }

    override suspend fun getPublicNodeThumbnailFromLocal(handle: Long): File? =
        withContext(ioDispatcher) {
            megaApiFolder.getMegaNodeByHandle(handle)?.run {
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
            megaApi.getMegaNodeByHandle(handle)?.let { node ->
                getThumbnailFile(node)?.let { thumbnail ->
                    suspendCancellableCoroutine { continuation ->
                        megaApi.getThumbnail(node, thumbnail.absolutePath,
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
            megaApiFolder.getMegaNodeByHandle(handle)?.let { node ->
                getThumbnailFile(node)?.let { thumbnail ->
                    suspendCancellableCoroutine { continuation ->
                        megaApiFolder.getThumbnail(node, thumbnail.absolutePath,
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


    override suspend fun getPreviewFromLocal(handle: Long): File? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(handle)?.run {
                getPreviewFile(this).takeIf {
                    it?.exists() ?: false
                }
            }
        }

    override suspend fun getPreviewFromServer(handle: Long): File? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(handle)?.let { node ->
                getPreviewFile(node)?.let { preview ->
                    suspendCancellableCoroutine { continuation ->
                        megaApi.getPreview(node, preview.absolutePath,
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
        val node = megaApi.getMegaNodeByHandle(handle)
        if (node == null || thumbnailFolderPath == null || !node.hasThumbnail()) {
            callback(false)
        } else {
            megaApi.getThumbnail(
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
        val node = megaApi.getMegaNodeByHandle(handle)
        if (node == null || previewFolderPath == null || !node.hasPreview()) {
            callback(false)
        } else {
            megaApi.getPreview(
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
        val node = megaApiFolder.getMegaNodeByHandle(handle)
        val thumbnailFolderPath = thumbnailFolderPath
        if (node == null || thumbnailFolderPath == null || !node.hasThumbnail()) {
            return@withContext false
        } else {
            return@withContext suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("getThumbnail") {
                    true
                }
                megaApi.getThumbnail(
                    node,
                    getThumbnailPath(thumbnailFolderPath, node),
                    listener
                )

                continuation.invokeOnCancellation { megaApi.removeRequestListener(listener) }
            }
        }
    }

    override suspend fun downloadPublicNodePreview(
        handle: Long,
    ): Boolean = withContext(ioDispatcher) {
        val node = megaApiFolder.getMegaNodeByHandle(handle)
        val previewFolderPath = previewFolderPath
        if (node == null || previewFolderPath == null || !node.hasPreview()) {
            return@withContext false
        } else {
            return@withContext suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("getThumbnail") {
                    true
                }
                megaApi.getPreview(
                    node,
                    getPreviewPath(previewFolderPath, node),
                    listener
                )

                continuation.invokeOnCancellation { megaApi.removeRequestListener(listener) }
            }
        }
    }

    private fun getPreviewPath(previewFolderPath: String, megaNode: MegaNode) =
        "$previewFolderPath${File.separator}${megaNode.getPreviewFileName()}"

    private fun getThumbnailPath(thumbnailFolderPath: String, megaNode: MegaNode) =
        "$thumbnailFolderPath${File.separator}${megaNode.getThumbnailFileName()}"

    private suspend fun getThumbnailFile(fileName: String): File? =
        cacheGateway.getCacheFile(CacheFolderConstant.THUMBNAIL_FOLDER, fileName)


    private suspend fun getPreviewFile(fileName: String): File? =
        cacheGateway.getCacheFile(CacheFolderConstant.PREVIEW_FOLDER, fileName)

    override suspend fun getThumbnailCacheFolderPath(): String? = withContext(ioDispatcher) {
        cacheGateway.getThumbnailCacheFolder()?.path
    }

    override suspend fun getPreviewCacheFolderPath(): String? = withContext(ioDispatcher) {
        cacheGateway.getPreviewCacheFolder()?.path
    }

    override suspend fun getFullSizeCacheFolderPath(): String? = withContext(ioDispatcher) {
        cacheGateway.getFullSizeCacheFolder()?.path
    }


    override suspend fun createThumbnail(handle: Long, file: File) = withContext(ioDispatcher) {
        val thumbnailFileName =
            "${handle.toString().encodeBase64()}${FileConstant.JPG_EXTENSION}"
        val thumbnailFile = getThumbnailFile(thumbnailFileName)
        requireNotNull(thumbnailFile)
        megaApi.createThumbnail(file.absolutePath, thumbnailFile.absolutePath)
    }


    override suspend fun createPreview(handle: Long, file: File) = withContext(ioDispatcher) {
        val previewFileName =
            "${handle.toString().encodeBase64()}${FileConstant.JPG_EXTENSION}"
        val previewFile = getPreviewFile(previewFileName)
        requireNotNull(previewFile)
        megaApi.createPreview(file.absolutePath, previewFile.absolutePath)
    }

    override suspend fun deleteThumbnail(handle: Long) = withContext(ioDispatcher) {
        val thumbnailFileName =
            "${handle.toString().encodeBase64()}${FileConstant.JPG_EXTENSION}"
        getThumbnailFile(thumbnailFileName)?.delete()
    }

    override suspend fun deletePreview(handle: Long) = withContext(ioDispatcher) {
        val previewFileName =
            "${handle.toString().encodeBase64()}${FileConstant.JPG_EXTENSION}"
        getPreviewFile(previewFileName)?.delete()
    }

    override suspend fun getThumbnailOrPreviewFileName(nodeHandle: Long) =
        withContext(ioDispatcher) {
            megaApi.handleToBase64(nodeHandle) + ".jpg"
        }

    override suspend fun setThumbnail(nodeHandle: Long, srcFilePath: String) =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(nodeHandle)?.let {
                suspendCancellableCoroutine { continuation ->
                    val listener = continuation.getRequestListener("setThumbnail") {}
                    megaApi.setThumbnail(it, srcFilePath, listener)
                    continuation.invokeOnCancellation { megaApi.removeRequestListener(listener) }
                }
            } ?: Unit
        }

    override suspend fun setPreview(nodeHandle: Long, srcFilePath: String) =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(nodeHandle)?.let {
                suspendCancellableCoroutine { continuation ->
                    val listener = continuation.getRequestListener("setThumbnail") {}
                    megaApi.setThumbnail(it, srcFilePath, listener)
                    continuation.invokeOnCancellation { megaApi.removeRequestListener(listener) }
                }
            } ?: Unit
        }
}