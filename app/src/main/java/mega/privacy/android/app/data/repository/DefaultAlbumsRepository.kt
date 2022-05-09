package mega.privacy.android.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.failWithError
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

class DefaultAlbumsRepository @Inject constructor(
    private val apiFacade: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    val megaLocalStorageFacade: MegaLocalStorageGateway,
    @ApplicationContext val context: Context
) : AlbumsRepository {

    override fun getCameraUploadFolderId(): Long? = megaLocalStorageFacade.camSyncHandle

    override fun getMediaUploadFolderId(): Long? = megaLocalStorageFacade.megaHandleSecondaryFolder

    override fun getThumbnailFromLocal(nodeId: Long): File? {
        return getThumbnailFile(apiFacade.getMegaNodeByHandle(nodeId)).takeIf{ it.exists() }
    }

    private fun getThumbnailFile(node: MegaNode): File = File(
        File(context.cacheDir, CacheFolderManager.THUMBNAIL_FOLDER),
        "${node.base64Handle}${FileUtil.JPG_EXTENSION}"
    )

    override suspend fun getThumbnailFromServer(nodeId: Long): File =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                val node = apiFacade.getMegaNodeByHandle(nodeId)
                val thumbnail = getThumbnailFile(node)

                apiFacade.getThumbnail(node, thumbnail.absolutePath,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request, error ->
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