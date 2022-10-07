package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaNodeUtil.getPreviewFileName
import mega.privacy.android.app.utils.MegaNodeUtil.getThumbnailFileName
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ImageRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import java.io.File
import javax.inject.Inject

/**
 * The repository implementation class regarding thumbnail feature.
 *
 * @param megaApiGateway MegaApiGateway
 * @param ioDispatcher CoroutineDispatcher
 * @param cacheGateway CacheGateway
 */
class DefaultImageRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheGateway: CacheGateway,
) : ImageRepository {

    private var thumbnailFolderPath: String? = null

    private var previewFolderPath: String? = null

    init {
        runBlocking(ioDispatcher) {
            thumbnailFolderPath =
                cacheGateway.getOrCreateCacheFolder(CacheFolderManager.THUMBNAIL_FOLDER)?.path
            previewFolderPath =
                cacheGateway.getOrCreateCacheFolder(CacheFolderManager.PREVIEW_FOLDER)?.path
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
        cacheGateway.getCacheFile(CacheFolderManager.THUMBNAIL_FOLDER,
            "${node.base64Handle}${FileUtil.JPG_EXTENSION}")

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
        cacheGateway.getCacheFile(CacheFolderManager.PREVIEW_FOLDER,
            "${node.base64Handle}${FileUtil.JPG_EXTENSION}")

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
}