package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SensitivityFilterOption
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FavouritesRepository
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * The repository implementation class regarding favourites
 * @param megaApiGateway MegaApiGateway
 * @param ioDispatcher IODispatcher
 */
internal class DefaultFavouritesRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nodeMapper: NodeMapper,
    private val megaSearchFilterMapper: MegaSearchFilterMapper,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val cancelTokenProvider: CancelTokenProvider,
) : FavouritesRepository {

    override suspend fun addFavourites(nodeIds: List<NodeId>) {
        withContext(ioDispatcher) {
            nodeIds.forEach { nodeId ->
                val megaNode = megaApiGateway.getMegaNodeByHandle(nodeId.longValue)
                megaApiGateway.setNodeFavourite(megaNode, true)
            }
        }
    }

    override suspend fun getAllFavorites(excludeSensitives: Boolean): List<UnTypedNode> =
        withContext(ioDispatcher) {
            val filter = megaSearchFilterMapper(
                parentHandle = null,
                searchCategory = SearchCategory.FAVOURITES,
                sensitivityFilter = SensitivityFilterOption.NonSensitiveOnly.takeIf { excludeSensitives },
            )
            val token = cancelTokenProvider.getOrCreateCancelToken()
            val nodes = megaApiGateway.searchWithFilter(
                filter,
                sortOrderIntMapper(SortOrder.ORDER_NONE),
                token,
            )
            mapNodesToFavouriteInfo(nodes)
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