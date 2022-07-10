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
import mega.privacy.android.domain.repository.ThumbnailRepository
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
class DefaultThumbnailRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheFolder: CacheFolderGateway,
) : ThumbnailRepository {
    override suspend fun getThumbnailFromLocal(handle: Long): File? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(handle)?.run {
                getThumbnailFile(this).takeIf {
                    it?.exists() ?: false
                }
            }
        }

    // Mark as suspend function, and later will CacheFolderGateway functions to suspend as well
    @Suppress("RedundantSuspendModifier")
    private suspend fun getThumbnailFile(node: MegaNode): File? = withContext(ioDispatcher) {
        cacheFolder.getCacheFile(CacheFolderManager.THUMBNAIL_FOLDER,
            "${node.base64Handle}${FileUtil.JPG_EXTENSION}")
    }

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
}