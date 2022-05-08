package mega.privacy.android.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.api.MegaDBHandlerGateway
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.repository.AlbumsRepository
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.FileUtil
import nz.mega.sdk.MegaError
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class DefaultAlbumsRepository @Inject constructor(
    private val apiFacade: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    val megaDBHandlerFacade: MegaDBHandlerGateway,
    @ApplicationContext val context: Context
) : AlbumsRepository {

    override fun getCameraUploadFolder(): String? = megaDBHandlerFacade.camSyncHandle

    override fun getMediaUploadFolder(): String? = megaDBHandlerFacade.megaHandleSecondaryFolder

    override suspend fun getThumbnail(nodeId: Long, base64Handle: String): File {
        return if (hasThumbnailFile(base64Handle)) {
            getThumbnailFileFromLocal(base64Handle)
        } else {
            getThumbnailFromServer(nodeId,base64Handle)
        }
    }

    fun getThumbnailFileFromLocal(base64Handle: String): File {
        val thumbnailFolder = File(context.cacheDir, CacheFolderManager.THUMBNAIL_FOLDER)
        return File(
            thumbnailFolder,
            base64Handle.plus(FileUtil.JPG_EXTENSION)
        )
    }

    fun hasThumbnailFile(
        base64Handle: String,
    ): Boolean {
        val thumbnail = getThumbnailFileFromLocal(base64Handle)
        return thumbnail.exists()
    }

    suspend fun getThumbnailFromServer(nodeId: Long,base64Handle:String): File =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                val node = apiFacade.getMegaNodeByHandle(nodeId)
                apiFacade.getThumbnail(node, getThumbnailFileFromLocal(base64Handle).absolutePath,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request, error ->
                            if (error.errorCode == MegaError.API_OK) {
                                continuation.resumeWith(Result.success(getThumbnailFileFromLocal(base64Handle)))
                            } else {
                                continuation.failWithError(error)
                            }
                        }
                    )
                )
            }
        }

}