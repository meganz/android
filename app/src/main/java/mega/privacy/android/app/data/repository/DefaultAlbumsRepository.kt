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
import nz.mega.sdk.MegaError
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class DefaultAlbumsRepository @Inject constructor(
    private val apiFacade: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    val megaLocalStorageFacade: MegaLocalStorageGateway,
    @ApplicationContext val context: Context
) : AlbumsRepository {

    override fun getCameraUploadFolder(): String? = megaLocalStorageFacade.camSyncHandle

    override fun getMediaUploadFolder(): String? = megaLocalStorageFacade.megaHandleSecondaryFolder

    override fun getThumbnailFromLocal(thumbnailName: String): File? {
        return getThumbnailFile(thumbnailName).takeIf{ it.exists() }
    }

    private fun getThumbnailFile(thumbnailName: String): File {
        val thumbnailFolder = File(context.cacheDir, CacheFolderManager.THUMBNAIL_FOLDER)
        return File(
            thumbnailFolder,
            thumbnailName
        )
    }

    override suspend fun getThumbnailFromServer(nodeId: Long,thumbnailName: String): File =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                val node = apiFacade.getMegaNodeByHandle(nodeId)
                apiFacade.getThumbnail(node, getThumbnailFile(thumbnailName).absolutePath,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request, error ->
                            if (error.errorCode == MegaError.API_OK) {
                                continuation.resumeWith(Result.success(getThumbnailFile(thumbnailName)))
                            } else {
                                continuation.failWithError(error)
                            }
                        }
                    )
                )
            }
        }

}