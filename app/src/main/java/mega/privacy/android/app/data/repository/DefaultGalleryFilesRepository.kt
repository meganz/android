package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.gateway.CacheFolderGateway
import mega.privacy.android.app.data.gateway.MonitorNodeChangeFacade
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.entity.FavouriteInfo
import mega.privacy.android.domain.entity.chat.FileGalleryItem
import mega.privacy.android.domain.repository.FavouritesRepository
import mega.privacy.android.domain.repository.GalleryFilesRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaHandleList
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

/**
 * The repository implementation class regarding gallery files
 * @param ioDispatcher IODispatcher
 */
class DefaultGalleryFilesRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GalleryFilesRepository {
    override suspend fun getAllGalleryFiles(): Flow<List<FileGalleryItem>> {
        TODO("Not yet implemented")
    }

}