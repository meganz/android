package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.mapper.FavouriteInfoMapper
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.gateway.MonitorNodeChangeFacade
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.entity.FavouriteFolderInfo
import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.domain.repository.FavouritesRepository
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import nz.mega.sdk.MegaError
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

/**
 * The repository implementation class regarding favourites
 * @param megaApiGateway MegaApiGateway
 * @param ioDispatcher IODispatcher
 * @param monitorNodeChangeFacade MonitorNodeChangeFacade
 */
class DefaultFavouritesRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorNodeChangeFacade: MonitorNodeChangeFacade,
    private val favouriteInfoMapper: FavouriteInfoMapper
) :
    FavouritesRepository {

    override suspend fun getAllFavorites(): List<FavouriteInfo> = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.getFavourites(
                null,
                0,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        CoroutineScope(ioDispatcher).launch {
                            if (error.errorCode == MegaError.API_OK) {
                                val favourites = mutableListOf<FavouriteInfo>()
                                val megaHandleList = request.megaHandleList
                                for (i in 0 until megaHandleList.size()) {
                                    val favouriteItem =
                                        megaApiGateway.getMegaNodeByHandle(megaHandleList[i])
                                    favourites.add(
                                        favouriteInfoMapper(
                                            favouriteItem,
                                            megaApiGateway
                                        )
                                    )
                                }
                                continuation.resumeWith(Result.success(favourites))
                            } else {
                                continuation.failWithError(error)
                            }
                        }
                    }
                ))
        }
    }

    override suspend fun getChildren(parentHandle: Long): FavouriteFolderInfo =
        withContext(ioDispatcher) {
            val parentNode = megaApiGateway.getMegaNodeByHandle(parentHandle)
            FavouriteFolderInfo(
                children = megaApiGateway.getChildrenByNode(parentNode).map { node ->
                    favouriteInfoMapper(
                        node,
                        megaApiGateway
                    )
                },
                name = parentNode.name,
                currentHandle = parentHandle,
                parentHandle = parentNode.parentHandle
            )
        }

    override fun monitorNodeChange(): Flow<Boolean> = monitorNodeChangeFacade.getEvents()
}