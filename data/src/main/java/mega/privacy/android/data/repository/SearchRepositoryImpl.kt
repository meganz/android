package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.search.SearchCategoryIntMapper
import mega.privacy.android.data.mapper.search.SearchCategoryMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.SearchRepository
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Search repository impl
 *
 * Implementation of [SearchRepository]
 */
internal class SearchRepositoryImpl @Inject constructor(
    private val searchCategoryMapper: SearchCategoryMapper,
    private val searchCategoryIntMapper: SearchCategoryIntMapper,
    private val nodeMapper: NodeMapper,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val cancelTokenProvider: CancelTokenProvider,
    private val getLinksSortOrder: GetLinksSortOrder,
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SearchRepository {
    override fun getSearchCategories(): List<SearchCategory> = listOf(
        MegaApiAndroid.FILE_TYPE_DEFAULT,
        MegaApiAndroid.FILE_TYPE_PHOTO,
        MegaApiAndroid.FILE_TYPE_DOCUMENT,
        MegaApiAndroid.FILE_TYPE_AUDIO,
        MegaApiAndroid.FILE_TYPE_VIDEO,
    ).map {
        searchCategoryMapper(it)
    }

    override suspend fun search(
        nodeId: NodeId?,
        searchCategory: SearchCategory,
        query: String,
        order: SortOrder,
    ): List<UnTypedNode> = withContext(ioDispatcher) {
        nodeId?.let {
            if ((query.isEmpty() || it.longValue != MegaApiJava.INVALID_HANDLE) &&
                searchCategory == SearchCategory.ALL
            ) {
                getNodeChildren(it, order)
            } else {
                val megaCancelToken = cancelTokenProvider.getOrCreateCancelToken()
                megaApiGateway.getMegaNodeByHandle(it.longValue)?.let { megaNode ->
                    val searchList = if (searchCategory == SearchCategory.ALL) {
                        megaApiGateway.search(
                            parent = megaNode,
                            query = query,
                            megaCancelToken = megaCancelToken,
                            order = sortOrderIntMapper(order)
                        )
                    } else {
                        megaApiGateway.searchByType(
                            parentNode = megaNode,
                            searchString = query,
                            cancelToken = megaCancelToken,
                            recursive = true,
                            order = sortOrderIntMapper(order),
                            type = searchCategoryIntMapper(searchCategory)
                        )
                    }
                    searchList.map { item -> nodeMapper(item) }
                }
            }
        }.orEmpty()
    }

    override suspend fun searchInShares(
        query: String,
        order: SortOrder,
    ): List<UnTypedNode> {
        return withContext(ioDispatcher) {
            val list = if (query.isEmpty()) {
                megaApiGateway.getInShares(sortOrderIntMapper(order))
            } else {
                megaApiGateway.searchOnInShares(
                    query,
                    cancelTokenProvider.getOrCreateCancelToken(),
                    sortOrderIntMapper(order)
                )
            }
            list.map { nodeMapper(it) }
        }
    }

    override suspend fun searchOutShares(query: String, order: SortOrder): List<UnTypedNode> =
        withContext(ioDispatcher) {
            if (query.isEmpty()) {
                val searchNodes = ArrayList<MegaNode>()
                val outShares = megaApiGateway.getOutgoingSharesNode(null)
                val addedHandles = mutableSetOf<Long>()
                for (outShare in outShares) {
                    if (!addedHandles.contains(outShare.nodeHandle)) {
                        megaApiGateway.getMegaNodeByHandle(outShare.nodeHandle)?.let {
                            addedHandles.add(it.handle)
                            searchNodes.add(it)
                        }
                    }
                }
                searchNodes
            } else {
                megaApiGateway.searchOnOutShares(
                    query = query,
                    megaCancelToken = cancelTokenProvider.getOrCreateCancelToken(),
                    order = sortOrderIntMapper(order)
                )
            }.map { nodeMapper(it) }
        }

    override suspend fun searchLinkShares(
        query: String,
        order: SortOrder,
        isFirstLevelNavigation: Boolean,
    ): List<UnTypedNode> = withContext(ioDispatcher) {
        if (query.isEmpty()) {
            megaApiGateway.getPublicLinks(
                if (isFirstLevelNavigation) sortOrderIntMapper(
                    getLinksSortOrder()
                ) else sortOrderIntMapper(order)
            )
        } else {
            megaApiGateway.searchOnLinkShares(
                query,
                cancelTokenProvider.getOrCreateCancelToken(),
                sortOrderIntMapper(order)
            )
        }.map { nodeMapper(it) }
    }

    private suspend fun getNodeChildren(
        nodeId: NodeId,
        order: SortOrder?,
    ): List<UnTypedNode> = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let { parent ->
            val childList = order?.let { sortOrder ->
                megaApiGateway.getChildrenByNode(
                    parent,
                    sortOrderIntMapper(sortOrder)
                )
            } ?: run {
                megaApiGateway.getChildrenByNode(parent)
            }
            childList.map { nodeMapper(it) }
        } ?: run {
            emptyList()
        }
    }
}