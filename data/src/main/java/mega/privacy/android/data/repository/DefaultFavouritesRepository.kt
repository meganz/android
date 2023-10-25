package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FavouritesRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaNode
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

/**
 * The repository implementation class regarding favourites
 * @param megaApiGateway MegaApiGateway
 * @param ioDispatcher IODispatcher
 */
internal class DefaultFavouritesRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nodeMapper: NodeMapper,
) : FavouritesRepository {

    override suspend fun addFavourites(nodeIds: List<NodeId>) {
        withContext(ioDispatcher) {
            nodeIds.forEach { nodeId ->
                val megaNode = megaApiGateway.getMegaNodeByHandle(nodeId.longValue)
                megaApiGateway.setNodeFavourite(megaNode, true)
            }
        }
    }

    override suspend fun getAllFavorites(): List<UnTypedNode> =
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
                                continuation.failWithError(error, "getAllFavorites")
                            }
                        }
                    )
                )
            }
            mapNodesToFavouriteInfo(handleList.getNodes())
        }

    override suspend fun removeFavourites(nodeIds: List<NodeId>) {
        withContext(ioDispatcher) {
            nodeIds.forEach { nodeId ->
                val megaNode = megaApiGateway.getMegaNodeByHandle(nodeId.longValue)
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
    private suspend fun mapNodesToFavouriteInfo(nodes: List<MegaNode>) =
        nodes.map { megaNode ->
            nodeMapper(
                megaNode,
            )
        }
}