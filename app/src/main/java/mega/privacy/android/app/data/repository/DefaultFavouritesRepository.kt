package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.extensions.toFavouriteInfo
import mega.privacy.android.app.data.gateway.MonitorNodeChangeFacade
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.entity.FavouriteFolderInfo
import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.domain.repository.FavouritesRepository
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import nz.mega.sdk.*
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

/**
 * The repository implementation class regarding favourites
 * @param apiFacade MegaApiGateway
 * @param ioDispatcher IODispatcher
 * @param monitorNodeChangeFacade MonitorNodeChangeFacade
 */
class DefaultFavouritesRepository @Inject constructor(
    private val apiFacade: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorNodeChangeFacade: MonitorNodeChangeFacade
) :
    FavouritesRepository {

    override suspend fun getAllFavorites(): List<FavouriteInfo> = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            apiFacade.getFavourites(
                null,
                0,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK) {
                            val favourites = mutableListOf<FavouriteInfo>()
                            val megaHandleList = request.megaHandleList
                            for (i in 0 until megaHandleList.size()) {
                                val favouriteItem = apiFacade.getMegaNodeByHandle(megaHandleList[i])
                                favourites.add(
                                    favouriteItem.toFavouriteInfo(
                                        apiFacade.hasVersion(favouriteItem),
                                        apiFacade.getNumChildFolders(favouriteItem),
                                        apiFacade.getNumChildFiles(favouriteItem)
                                    )
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

    override suspend fun getChildren(parentHandle: Long): FavouriteFolderInfo =
        withContext(ioDispatcher) {
            val parentNode = apiFacade.getMegaNodeByHandle(parentHandle)
            FavouriteFolderInfo(
                children = apiFacade.getChildrenByNode(parentNode).map { node ->
                    node.toFavouriteInfo(
                        apiFacade.hasVersion(parentNode),
                        apiFacade.getNumChildFolders(node),
                        apiFacade.getNumChildFiles(node)
                    )
                },
                name = parentNode.name,
                currentHandle = parentHandle,
                parentHandle = parentNode.parentHandle
            )
        }

    override fun monitorNodeChange(): Flow<Boolean> = monitorNodeChangeFacade.getEvents()
}