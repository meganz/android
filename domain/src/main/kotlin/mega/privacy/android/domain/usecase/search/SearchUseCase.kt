package mega.privacy.android.domain.usecase.search

import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchParameters
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.repository.FavouritesRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.SearchRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.favourites.SortFavouritesUseCase
import mega.privacy.android.domain.usecase.node.AddNodesTypeUseCase
import mega.privacy.android.domain.usecase.shares.MapNodeToShareUseCase
import javax.inject.Inject

/**
 * Search Node Use Case
 *
 * Handles every use-case related to search
 */
class SearchUseCase @Inject constructor(
    private val getCloudSortOrder: GetCloudSortOrder,
    private val searchRepository: SearchRepository,
    private val favouritesRepository: FavouritesRepository,
    private val sortFavouritesUseCase: SortFavouritesUseCase,
    private val addNodesTypeUseCase: AddNodesTypeUseCase,
    private val mapNodeToShareUseCase: MapNodeToShareUseCase,
    private val nodeRepository: NodeRepository,
) {

    /**
     * Invocation
     *
     * @param parentHandle search parent
     * @param nodeSourceType search type [NodeSourceType]
     * @param searchParameters search parameters [SearchParameters]
     *
     * @return list of search results or empty TypedNode
     */
    suspend operator fun invoke(
        parentHandle: NodeId,
        nodeSourceType: NodeSourceType,
        searchParameters: SearchParameters,
    ): List<TypedNode> {
        val (query, searchTarget, searchCategory, modificationDate, creationDate, description, tag) = searchParameters
        val invalidNodeHandle = searchRepository.getInvalidHandle()
        val searchList = when {
            // Favourites Root (No Search applied)
            query.isEmpty() && parentHandle == invalidNodeHandle && nodeSourceType == NodeSourceType.FAVOURITES ->
                sortFavouritesUseCase(favouritesRepository.getAllFavorites())

            // Incoming Shares Root (No Search applied)
            query.isEmpty() && parentHandle == invalidNodeHandle && searchTarget == SearchTarget.INCOMING_SHARE -> searchRepository.getInShares()

            // Outgoing Shares Root (No Search applied)
            query.isEmpty() && description.isNullOrEmpty() && tag.isNullOrEmpty() && parentHandle == invalidNodeHandle && searchTarget == SearchTarget.OUTGOING_SHARE -> searchRepository.getOutShares()

            // Links Shares Root (No Search applied)
            query.isEmpty() && description.isNullOrEmpty() && tag.isNullOrEmpty() && parentHandle == invalidNodeHandle && searchTarget == SearchTarget.LINKS_SHARE -> searchRepository.getPublicLinks()

            // Outgoing and Links Shares Root (Non Query Search applied)
            query.isEmpty() && (!description.isNullOrEmpty() || !tag.isNullOrEmpty())
                    && parentHandle == invalidNodeHandle &&
                    (searchTarget == SearchTarget.OUTGOING_SHARE || searchTarget == SearchTarget.LINKS_SHARE) ->
                searchRepository.search(
                    nodeId = getSearchParentNode(nodeSourceType, parentHandle, invalidNodeHandle),
                    order = getCloudSortOrder(),
                    parameters = searchParameters,
                )

            // Tag search recursively
            query.isEmpty() && tag.isNullOrEmpty().not() -> searchRepository.search(
                nodeId = getSearchParentNode(nodeSourceType, parentHandle, invalidNodeHandle),
                order = getCloudSortOrder(),
                parameters = searchParameters,
            )

            // General Children (Non Query Search applied)
            query.isEmpty() && searchCategory == SearchCategory.ALL && modificationDate == null && creationDate == null -> searchRepository.getChildren(
                nodeId = getSearchParentNode(nodeSourceType, parentHandle, invalidNodeHandle),
                order = getCloudSortOrder(),
                parameters = searchParameters,
            )

            // General Root (Query Search applied)
            else -> searchRepository.search(
                nodeId = getSearchParentNode(nodeSourceType, parentHandle, invalidNodeHandle),
                order = getCloudSortOrder(),
                parameters = searchParameters,
            )
        }

        return addNodesTypeUseCase(searchList).let { typedNodes ->
            if (searchTarget == SearchTarget.INCOMING_SHARE) {
                mapIncomingShares(typedNodes, invalidNodeHandle)
            } else {
                typedNodes
            }
        }
    }

    /**
     * Incoming-share search results come back as plain typed nodes. Re-wrap only the *root* incoming
     * shares (those with no in-account parent) as [ShareNode]s carrying their [ShareData] (sharer
     * info + access) — mirroring the browse list — so they show the sharer subtitle and honour
     * read-only shares. Inner results stay plain so they keep their folder-content subtitle.
     */
    private suspend fun mapIncomingShares(
        nodes: List<TypedNode>,
        invalidNodeHandle: NodeId,
    ): List<TypedNode> = nodes.map { node ->
        if (node.parentId != invalidNodeHandle) return@map node

        val access = nodeRepository.getNodeAccessPermission(node.id) ?: return@map node
        val shareData = ShareData(
            user = nodeRepository.getIncomingShareParentUserEmail(node.id),
            nodeHandle = node.id.longValue,
            access = access,
            timeStamp = 0L,
            isPending = false,
            isVerified = false,
            count = 0,
        )

        runCatching { mapNodeToShareUseCase(node, shareData) }.getOrDefault(node)
    }

    /**
     * This method Returns [Node] for respective selected [NodeSourceType]
     *
     * @param nodeSourceType
     * @param parentHandle
     * @return [Node]
     */
    private suspend fun getSearchParentNode(
        nodeSourceType: NodeSourceType,
        parentHandle: NodeId,
        invalidNodeHandle: NodeId,
    ): NodeId? = if (parentHandle.longValue == invalidNodeHandle.longValue) {
        when (nodeSourceType) {
            NodeSourceType.CLOUD_DRIVE -> searchRepository.getRootNodeId()
            NodeSourceType.RUBBISH_BIN -> searchRepository.getRubbishNodeId()
            NodeSourceType.BACKUPS -> searchRepository.getBackUpNodeId()
            else -> null
        }
    } else {
        parentHandle
    }
}