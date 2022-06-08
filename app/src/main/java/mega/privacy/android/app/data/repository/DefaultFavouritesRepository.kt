package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.gateway.CacheFolderGateway
import mega.privacy.android.app.data.gateway.MonitorNodeChangeFacade
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.mapper.FavouriteInfoMapper
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.entity.FavouriteFolderInfo
import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.domain.repository.FavouritesRepository
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.MegaNodeUtil.getThumbnailFileName
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaNode
import java.io.File
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
    private val favouriteInfoMapper: FavouriteInfoMapper,
    private val cacheFolder: CacheFolderGateway
) :
    FavouritesRepository {

    override suspend fun getAllFavorites(): List<FavouriteInfo> =
        withContext(ioDispatcher) {
            val handleList = suspendCoroutine<MegaHandleList> { continuation ->
                megaApiGateway.getFavourites(
                    node = null,
                    count = 0,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request, error ->
                            if (error.errorCode == MegaError.API_OK) {
                                continuation.resumeWith(Result.success(request.megaHandleList))
                            } else {
                                continuation.failWithError(error)
                            }
                        }
                    )
                )
            }
            mapNodesToFavouriteInfo(handleList.getNodes())
        }

    override suspend fun getChildren(parentHandle: Long): FavouriteFolderInfo? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(parentHandle)?.let { parentNode ->
                FavouriteFolderInfo(
                    children = mapNodesToFavouriteInfo(megaApiGateway.getChildrenByNode(parentNode)),
                    name = parentNode.name,
                    currentHandle = parentHandle,
                    parentHandle = parentNode.parentHandle
                )
            }
        }

    override fun monitorNodeChange(): Flow<Boolean> = monitorNodeChangeFacade.getEvents()

    override suspend fun removeFavourites(handles: List<Long>) {
        withContext(ioDispatcher) {
            handles.map { handle ->
                megaApiGateway.getMegaNodeByHandle(handle)
            }.forEach { megaNode ->
                megaApiGateway.setNodeFavourite(megaNode, false)
            }
        }
    }

    /**
     * Get node from MegaHandleList
     * @return List<MegaNode>
     */
    private suspend fun MegaHandleList.getNodes() = (0..size())
        .map { this[it] }
        .mapNotNull { megaApiGateway.getMegaNodeByHandle(it) }

    /**
     * Convert the MegaNode list to FavouriteInfo list
     * @param nodes List<MegaNode>
     * @return FavouriteInfo list
     */
    private fun mapNodesToFavouriteInfo(nodes: List<MegaNode>) =
        nodes.map { megaNode ->
            favouriteInfoMapper(
                megaNode,
                getThumbnailCacheFilePath(megaNode),
                megaApiGateway.hasVersion(megaNode),
                megaApiGateway.getNumChildFolders(megaNode),
                megaApiGateway.getNumChildFiles(megaNode)
            )
        }

    /**
     * Get the thumbnail cache file path
     * @param megaNode MegaNode
     * @return thumbnail cache file path
     */
    private fun getThumbnailCacheFilePath(megaNode: MegaNode) =
        cacheFolder.getCacheFolder(CacheFolderManager.THUMBNAIL_FOLDER)?.let { thumbnail ->
            "$thumbnail${File.separator}${megaNode.getThumbnailFileName()}"
        }
            ?.takeUnless { megaNode.isFolder }
}