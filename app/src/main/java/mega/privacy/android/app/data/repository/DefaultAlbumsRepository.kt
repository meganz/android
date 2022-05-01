package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.extensions.toAlbumItemInfo
import mega.privacy.android.app.data.gateway.MonitorNodeChangeFacade
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.entity.AlbumItemInfo
import mega.privacy.android.app.domain.repository.AlbumsRepository
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.MegaNodeUtil.isImage
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

/**
 * The repository implementation class regarding albums (favorite)
 * @param apiFacade MegaApiGateway
 * @param ioDispatcher IODispatcher
 * @param monitorNodeChangeFacade nodeChange
 * @param dbHandler Db
 */
class DefaultAlbumsRepository @Inject constructor(
    private val apiFacade: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorNodeChangeFacade: MonitorNodeChangeFacade,
    val dbHandler: DatabaseHandler
) : AlbumsRepository {

    override suspend fun getFavouriteAlbumItems(): List<AlbumItemInfo> = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            apiFacade.getFavourites(
                null,
                0,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK) {
                            val favourites = mutableListOf<AlbumItemInfo>()
                            val megaHandleList = request.megaHandleList
                            for (i in 0 until megaHandleList.size()) {
                                val favouriteItem = apiFacade.getMegaNodeByHandle(megaHandleList[i])
                                if (favouriteItem.isImage()
                                    || (favouriteItem.isVideo() && isInCUOrMUFolder(favouriteItem))
                                )
                                    favourites.add(
                                        favouriteItem.toAlbumItemInfo()
                                    )
                            }
                            continuation.resumeWith(Result.success(favourites))
                        } else {
                            continuation.failWithError(error)
                        }
                    }
                ))
        }
    }

    override fun monitorNodeChange(): Flow<Boolean> = monitorNodeChangeFacade.getEvents()

    /**
     * Check the file is in Camera Uploads(CU) or Media Uploads(MU) folder, if it is in, the parent handle will be camSyncHandle or secondaryMediaFolderEnabled
     *
     * @return True, the file is in CU or MU folder. False, it is not in.
     */
    private fun isInCUOrMUFolder(favouriteItem: MegaNode): Boolean {
        val pref: MegaPreferences? = dbHandler.preferences

        pref?.let { megaPreferences ->
            // get cuFolderNode if cu handle existed
            megaPreferences.camSyncHandle?.let { camSyncHandle ->
                return favouriteItem.parentHandle == camSyncHandle.toLong()
            }
            // get muFolderNode if mu handle existed
            megaPreferences.megaHandleSecondaryFolder?.let { megaHandleSecondaryFolder ->
                return favouriteItem.parentHandle == megaHandleSecondaryFolder.toLong()
            }
        }
        return false
    }
}