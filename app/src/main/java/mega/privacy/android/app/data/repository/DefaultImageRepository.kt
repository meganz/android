package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.gateway.CacheFolderGateway
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaNodeUtil.getPreviewFileName
import mega.privacy.android.app.utils.MegaNodeUtil.getThumbnailFileName
import mega.privacy.android.domain.repository.ImageRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

/**
 * The repository implementation class regarding thumbnail feature.
 *
 * @param megaApiGateway MegaApiGateway
 * @param ioDispatcher CoroutineDispatcher
 * @param cacheFolder CacheFolderGateway
 */
class DefaultImageRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheFolder: CacheFolderGateway,
) : ImageRepository {

    private var thumbnailFolderPath: String? = null

    private var previewFolderPath: String? = null

    init {
        thumbnailFolderPath = cacheFolder.getCacheFolder(CacheFolderManager.THUMBNAIL_FOLDER)?.path
        previewFolderPath = cacheFolder.getCacheFolder(CacheFolderManager.PREVIEW_FOLDER)?.path
    }

    override suspend fun getThumbnailFromLocal(handle: Long): File? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(handle)?.run {
                getThumbnailFile(this).takeIf {
                    it?.exists() ?: false
                }
            }
        }

    private fun getThumbnailFile(node: MegaNode): File? =
        cacheFolder.getCacheFile(CacheFolderManager.THUMBNAIL_FOLDER,
            "${node.base64Handle}${FileUtil.JPG_EXTENSION}")

    override suspend fun getThumbnailFromServer(handle: Long): File? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(handle)?.let { node ->
                getThumbnailFile(node)?.let { thumbnail ->
                    suspendCoroutine { continuation ->
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

    private fun getPreviewFile(node: MegaNode): File? =
        cacheFolder.getCacheFile(CacheFolderManager.PREVIEW_FOLDER,
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
                    suspendCoroutine { continuation ->
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
                getThumbnailPath(thumbnailFolderPath!!, node),
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
                getPreviewPath(previewFolderPath!!, node),
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