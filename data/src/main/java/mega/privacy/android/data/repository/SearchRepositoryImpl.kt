package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.SortOrderSource
import mega.privacy.android.data.database.dao.RecentSearchDao
import mega.privacy.android.data.database.entity.RecentSearchEntity
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.data.mapper.search.MegaSearchPageMapper
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.search.SearchParameters
import mega.privacy.android.domain.extension.getNodeMappingStrategy
import mega.privacy.android.domain.extension.mapAsync
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.SearchRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrderUseCase
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSearchPage
import javax.inject.Inject

/**
 * Search repository impl
 *
 * Implementation of [SearchRepository]
 */
internal class SearchRepositoryImpl @Inject constructor(
    private val nodeMapper: NodeMapper,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val cancelTokenProvider: CancelTokenProvider,
    private val getLinksSOrtOrderUseCase: GetLinksSortOrderUseCase,
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val megaSearchFilterMapper: MegaSearchFilterMapper,
    private val megaSearchPageMapper: MegaSearchPageMapper,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    private val recentSearchDao: RecentSearchDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SearchRepository {

    override suspend fun search(
        nodeId: NodeId?,
        order: SortOrder,
        parameters: SearchParameters,
    ): List<UnTypedNode> = withContext(ioDispatcher) {
        val megaCancelToken = cancelTokenProvider.getOrCreateCancelToken()
        val (query, searchTarget, searchCategory, modificationDate, creationDate, description, tag) = parameters
        val queryFilter = megaSearchFilterMapper(
            searchQuery = query,
            parentHandle = nodeId ?: NodeId(-1L),
            searchTarget = searchTarget,
            searchCategory = searchCategory,
            modificationDate = modificationDate,
            creationDate = creationDate,
            description = description,
            tag = tag,
            useAndForTextQuery = parameters.useAndForTextQuery
                ?: (description == null && tag == null),
        )
        val offlineItems = async { getAllOfflineNodeHandle() }
        val searchList = async {
            megaApiGateway.searchWithFilter(
                filter = queryFilter,
                order = sortOrderIntMapper(order),
                megaCancelToken = megaCancelToken,
                megaSearchPage = resultsLimitPage(),
            )
        }
        mapMegaNodesToUnTypedNodes(searchList.await(), offlineItems.await())
    }

    override suspend fun getChildren(
        nodeId: NodeId?,
        order: SortOrder,
        parameters: SearchParameters,
    ): List<UnTypedNode> = withContext(ioDispatcher) {
        val megaCancelToken = cancelTokenProvider.getOrCreateCancelToken()
        val (query, searchTarget, searchCategory, modificationDate, creationDate, description, tag) = parameters
        val filter = megaSearchFilterMapper(
            searchQuery = query,
            parentHandle = nodeId ?: NodeId(-1),
            searchTarget = searchTarget,
            searchCategory = searchCategory,
            modificationDate = modificationDate,
            creationDate = creationDate,
            description = description,
            tag = tag,
        )
        val offlineItems = async { getAllOfflineNodeHandle() }
        val searchList = async {
            megaApiGateway.getChildren(
                filter = filter,
                order = sortOrderIntMapper(order),
                megaCancelToken = megaCancelToken,
                megaSearchPage = resultsLimitPage(),
            )
        }
        mapMegaNodesToUnTypedNodes(searchList.await(), offlineItems.await())
    }

    override suspend fun searchInFolderLink(
        nodeId: NodeId?,
        order: SortOrder,
        parameters: SearchParameters,
    ): List<UnTypedNode> = withContext(ioDispatcher) {
        val (query, searchTarget, searchCategory, modificationDate, creationDate, description, tag) = parameters
        val filter = megaSearchFilterMapper(
            searchQuery = query,
            parentHandle = nodeId ?: NodeId(-1L),
            searchTarget = searchTarget,
            searchCategory = searchCategory,
            modificationDate = modificationDate,
            creationDate = creationDate,
            description = description,
            tag = tag,
            useAndForTextQuery = parameters.useAndForTextQuery
                ?: (description == null && tag == null),
        )
        megaApiFolderGateway.search(
            filter = filter,
            order = sortOrderIntMapper(order),
            megaCancelToken = cancelTokenProvider.getOrCreateCancelToken(),
            megaSearchPage = resultsLimitPage(),
        ).mapNotNull { nodeMapper(megaNode = it, fromFolderLink = true) }
    }

    /**
     * Page capping how many nodes a single search materializes — mapping every node of an
     * unbounded result set to domain entities can exhaust the heap. The SDK sorts before
     * paging, so the cap keeps the top results in the requested order.
     */
    private fun resultsLimitPage(): MegaSearchPage =
        megaSearchPageMapper(offset = 0, limit = MAX_SEARCH_RESULTS.toLong())

    private suspend fun getAllOfflineNodeHandle() =
        megaLocalRoomGateway.getAllOfflineInfo().associateBy { it.handle }

    private suspend fun mapMegaNodesToUnTypedNodes(
        childList: List<MegaNode>,
        offlineItems: Map<String, Offline>?,
    ): List<UnTypedNode> = childList.mapAsync(getNodeMappingStrategy(childList.size)) {
        nodeMapper(
            megaNode = it,
            offline = offlineItems?.get(it.handle.toString())
        )
    }.filterNotNull()

    override suspend fun getInShares() = withContext(ioDispatcher) {
        megaApiGateway.getInShares(sortOrderIntMapper(getOthersSortOrder())).let { list ->
            list.mapNotNull { nodeMapper(it) }
        }
    }

    override suspend fun getOutShares() = withContext(ioDispatcher) {
        val searchNodes = ArrayList<MegaNode>()
        val outShares = megaApiGateway.getOutgoingSharesNode(
            sortOrderIntMapper(
                sortOrder = getCloudSortOrder(),
                source = SortOrderSource.OutgoingShares
            )
        )
        val addedHandles = mutableSetOf<Long>()
        for (outShare in outShares) {
            if (!addedHandles.contains(outShare.nodeHandle)) {
                megaApiGateway.getMegaNodeByHandle(outShare.nodeHandle)?.let {
                    addedHandles.add(it.handle)
                    searchNodes.add(it)
                }
            }
        }
        searchNodes.mapNotNull { nodeMapper(it) }
    }

    override suspend fun getPublicLinks() =
        withContext(ioDispatcher) {
            megaApiGateway.getPublicLinks(
                sortOrderIntMapper(
                    getLinksSOrtOrderUseCase(true)
                )
            )
                .let { list ->
                    list.mapNotNull { nodeMapper(it) }
                }
        }

    override suspend fun getBackUpNodeId(): NodeId? = withContext(ioDispatcher) {
        megaApiGateway.getBackupsNode()?.let {
            NodeId(it.handle)
        }
    }

    override suspend fun getRubbishNodeId(): NodeId? = withContext(ioDispatcher) {
        megaApiGateway.getRubbishBinNode()?.let {
            NodeId(it.handle)
        }
    }

    override suspend fun getRootNodeId(): NodeId? = withContext(ioDispatcher) {
        megaApiGateway.getRootNode()?.let {
            NodeId(it.handle)
        }
    }

    override suspend fun getInvalidHandle(): NodeId = withContext(ioDispatcher) {
        NodeId(megaApiGateway.getInvalidHandle())
    }

    override suspend fun saveRecentSearch(query: String) {
        if (query.isNotEmpty()) {
            recentSearchDao.insertRecentSearchWithPrefixCleanup(
                RecentSearchEntity(
                    searchQuery = query,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    override fun monitorRecentSearches(): Flow<List<String>> =
        recentSearchDao.monitorRecentSearches()
            .map { entities ->
                entities.map { it.searchQuery }
            }

    override suspend fun clearRecentSearches() {
        recentSearchDao.clearRecentSearches()
    }

    companion object {
        /**
         * Upper bound on nodes materialized from a single search
         */
        internal const val MAX_SEARCH_RESULTS = 10_000
    }
}