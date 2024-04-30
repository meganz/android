package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.data.mapper.search.SearchCategoryMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.SearchRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Search repository impl
 *
 * Implementation of [SearchRepository]
 */
internal class SearchRepositoryImpl @Inject constructor(
    private val searchCategoryMapper: SearchCategoryMapper,
    private val nodeMapper: NodeMapper,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val cancelTokenProvider: CancelTokenProvider,
    private val getLinksSortOrder: GetLinksSortOrder,
    private val megaApiGateway: MegaApiGateway,
    private val megaSearchFilterMapper: MegaSearchFilterMapper,
    private val getCloudSortOrder: GetCloudSortOrder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SearchRepository {
    override fun getSearchCategories(): List<SearchCategory> = listOf(
        MegaApiAndroid.FILE_TYPE_DEFAULT,
        MegaApiAndroid.FILE_TYPE_PHOTO,
        MegaApiAndroid.FILE_TYPE_ALL_DOCS,
        MegaApiAndroid.FILE_TYPE_AUDIO,
        MegaApiAndroid.FILE_TYPE_VIDEO,
    ).map {
        searchCategoryMapper(it)
    }

    override suspend fun search(
        nodeId: NodeId?,
        query: String,
        order: SortOrder,
        searchTarget: SearchTarget,
        searchCategory: SearchCategory,
        modificationDate: DateFilterOption?,
        creationDate: DateFilterOption?,
    ): List<UnTypedNode> = withContext(ioDispatcher) {
        val megaCancelToken = cancelTokenProvider.getOrCreateCancelToken()
        val filter = megaSearchFilterMapper(
            searchQuery = query,
            parentHandle = nodeId ?: NodeId(-1L),
            searchTarget = searchTarget,
            searchCategory = searchCategory,
            modificationDate = modificationDate,
            creationDate = creationDate
        )
        val searchList = megaApiGateway.searchWithFilter(
            filter = filter,
            order = sortOrderIntMapper(order),
            megaCancelToken = megaCancelToken,
        )
        searchList.map { item -> nodeMapper(item) }
    }

    override suspend fun getChildren(
        nodeId: NodeId?,
        query: String,
        order: SortOrder,
        searchTarget: SearchTarget,
        searchCategory: SearchCategory,
        modificationDate: DateFilterOption?,
        creationDate: DateFilterOption?,
    ): List<UnTypedNode> = withContext(ioDispatcher) {
        val megaCancelToken = cancelTokenProvider.getOrCreateCancelToken()
        val filter = megaSearchFilterMapper(
            searchQuery = query,
            parentHandle = nodeId ?: NodeId(-1),
            searchTarget = searchTarget,
            searchCategory = searchCategory,
            modificationDate = modificationDate,
            creationDate = creationDate
        )
        val searchList = megaApiGateway.getChildren(
            filter = filter,
            order = sortOrderIntMapper(order),
            megaCancelToken = megaCancelToken,
        )
        searchList.map { item -> nodeMapper(item) }
    }

    override suspend fun getInShares() = withContext(ioDispatcher) {
        megaApiGateway.getInShares(sortOrderIntMapper(getCloudSortOrder())).let { list ->
            list.map { nodeMapper(it) }
        }
    }

    override suspend fun getOutShares() = withContext(ioDispatcher) {
        val searchNodes = ArrayList<MegaNode>()
        val outShares =
            megaApiGateway.getOutgoingSharesNode(sortOrderIntMapper(getCloudSortOrder()))
        val addedHandles = mutableSetOf<Long>()
        for (outShare in outShares) {
            if (!addedHandles.contains(outShare.nodeHandle)) {
                megaApiGateway.getMegaNodeByHandle(outShare.nodeHandle)?.let {
                    addedHandles.add(it.handle)
                    searchNodes.add(it)
                }
            }
        }
        searchNodes.map { nodeMapper(it) }
    }

    override suspend fun getPublicLinks() = withContext(ioDispatcher) {
        megaApiGateway.getPublicLinks(sortOrderIntMapper(getLinksSortOrder())).let { list ->
            list.map { nodeMapper(it) }
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
}