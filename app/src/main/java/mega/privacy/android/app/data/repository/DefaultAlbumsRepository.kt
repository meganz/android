package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.gateway.CacheFolderGateway
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.repository.AlbumsRepository
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.FileUtil
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [AlbumsRepository]
 *
 * @property apiFacade MegaApiGateway
 * @property ioDispatcher CoroutineDispatcher
 * @property megaLocalStorageFacade MegaLocalStorageGateway
 * @property cacheFolderFacade CacheFolderGateway
 */
class DefaultAlbumsRepository @Inject constructor(
        private val apiFacade: MegaApiGateway,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val megaLocalStorageFacade: MegaLocalStorageGateway,
        private val cacheFolderFacade: CacheFolderGateway,
) : AlbumsRepository {

    override suspend fun getCameraUploadFolderId(): Long? = megaLocalStorageFacade.getCamSyncHandle()

    override suspend fun getMediaUploadFolderId(): Long? = megaLocalStorageFacade.getMegaHandleSecondaryFolder()

    override suspend fun getThumbnailFromLocal(nodeId: Long): File? {
        return getThumbnailFile(apiFacade.getMegaNodeByHandle(nodeId)).takeIf {
            it?.exists() ?: false
        }
    }

    // Mark as suspend function, and later will CacheFolderGateway functions to suspend as well
    @Suppress("RedundantSuspendModifier")
    private suspend fun getThumbnailFile(node: MegaNode): File? = withContext(ioDispatcher) {
        cacheFolderFacade.getCacheFile(CacheFolderManager.THUMBNAIL_FOLDER, "${node.base64Handle}${FileUtil.JPG_EXTENSION}")
    }

    override suspend fun getThumbnailFromServer(nodeId: Long): File =
            withContext(ioDispatcher) {
                val node = apiFacade.getMegaNodeByHandle(nodeId)
                val thumbnail = getThumbnailFile(node)
                suspendCoroutine { continuation ->
                    thumbnail?.let {
                        apiFacade.getThumbnail(node, it.absolutePath,
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